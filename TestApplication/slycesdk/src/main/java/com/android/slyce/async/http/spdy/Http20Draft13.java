/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.slyce.async.http.spdy;

import com.android.slyce.async.BufferedDataSink;
import com.android.slyce.async.ByteBufferList;
import com.android.slyce.async.DataEmitter;
import com.android.slyce.async.DataEmitterReader;
import com.android.slyce.async.callback.DataCallback;
import com.android.slyce.async.http.Protocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.logging.Logger;

import static com.android.slyce.async.http.spdy.Http20Draft13.FrameLogger.formatHeader;
import static java.lang.String.format;
import static java.util.logging.Level.FINE;

/**
 * Read and write HTTP/2 v13 frames.
 * <p>http://tools.ietf.org/html/draft-ietf-httpbis-http2-13
 */

final class Http20Draft13 implements Variant {
    private static final Logger logger = Logger.getLogger(Http20Draft13.class.getName());

    @Override
    public Protocol getProtocol() {
        return Protocol.HTTP_2;
    }

    private static final ByteString CONNECTION_PREFACE
    = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");

    static final int MAX_FRAME_SIZE = 0x3fff; // 16383

    static final byte TYPE_DATA = 0x0;
    static final byte TYPE_HEADERS = 0x1;
    static final byte TYPE_PRIORITY = 0x2;
    static final byte TYPE_RST_STREAM = 0x3;
    static final byte TYPE_SETTINGS = 0x4;
    static final byte TYPE_PUSH_PROMISE = 0x5;
    static final byte TYPE_PING = 0x6;
    static final byte TYPE_GOAWAY = 0x7;
    static final byte TYPE_WINDOW_UPDATE = 0x8;
    static final byte TYPE_CONTINUATION = 0x9;

    static final byte FLAG_NONE = 0x0;
    static final byte FLAG_ACK = 0x1; // Used for settings and ping.
    static final byte FLAG_END_STREAM = 0x1; // Used for headers and data.
    static final byte FLAG_END_SEGMENT = 0x2;
    static final byte FLAG_END_HEADERS = 0x4; // Used for headers and continuation.
    static final byte FLAG_END_PUSH_PROMISE = 0x4;
    static final byte FLAG_PADDED = 0x8; // Used for headers and data.
    static final byte FLAG_PRIORITY = 0x20; // Used for headers.
    static final byte FLAG_COMPRESSED = 0x20; // Used for data.

    /**
     * Creates a frame reader with max header table size of 4096 and data frame
     * compression disabled.
     */
    @Override
    public FrameReader newReader(DataEmitter source, FrameReader.Handler handler, boolean client) {
        return new Reader(source, handler, 4096, client);
    }

    @Override
    public FrameWriter newWriter(BufferedDataSink sink, boolean client) {
        return new Writer(sink, client);
    }

    @Override
    public int maxFrameSize() {
        return MAX_FRAME_SIZE;
    }

    static final class Reader implements FrameReader {
        private final DataEmitter emitter;
        private final boolean client;
        private final Handler handler;
        private final DataEmitterReader reader;

        // Visible for testing.
        final HpackDraft08.Reader hpackReader;

        Reader(DataEmitter emitter, Handler handler, int headerTableSize, boolean client) {
            this.emitter = emitter;
            this.client = client;
            this.hpackReader = new HpackDraft08.Reader(headerTableSize);
            this.handler = handler;
            reader = new DataEmitterReader();

            parseFrameHeader();
        }

        private void parseFrameHeader() {
            emitter.setDataCallback(reader);
            reader.read(8, onFrame);
        }

        int w1;
        int w2;
        byte flags;
        byte type;
        short length;
        int streamId;
        private final DataCallback onFrame = new com.android.slyce.async.callback.DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                bb.order(ByteOrder.BIG_ENDIAN);
                w1 = bb.getInt();
                w2 = bb.getInt();

                // boolean r = (w1 & 0xc0000000) != 0; // Reserved: Ignore first 2 bits.
                length = (short) ((w1 & 0x3fff0000) >> 16); // 14-bit unsigned == MAX_FRAME_SIZE
                type = (byte) ((w1 & 0xff00) >> 8);
                flags = (byte) (w1 & 0xff);
                // boolean r = (w2 & 0x80000000) != 0; // Reserved: Ignore first bit.
                streamId = (w2 & 0x7fffffff); // 31-bit opaque identifier.
                if (logger.isLoggable(FINE))
                    logger.fine(formatHeader(true, streamId, length, type, flags));

                reader.read(length, onFullFrame);
            }
        };

        private final DataCallback onFullFrame = new com.android.slyce.async.callback.DataCallback() {
            @Override
            public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                try {
                    switch (type) {
                        case TYPE_DATA:
                            readData(bb, length, flags, streamId);
                            break;

                        case TYPE_HEADERS:
                            readHeaders(bb, length, flags, streamId);
                            break;

                        case TYPE_PRIORITY:
                            readPriority(bb, length, flags, streamId);
                            break;

                        case TYPE_RST_STREAM:
                            readRstStream(bb, length, flags, streamId);
                            break;

                        case TYPE_SETTINGS:
                            readSettings(bb, length, flags, streamId);
                            break;

                        case TYPE_PUSH_PROMISE:
                            readPushPromise(bb, length, flags, streamId);
                            break;

                        case TYPE_PING:
                            readPing(bb, length, flags, streamId);
                            break;

                        case TYPE_GOAWAY:
                            readGoAway(bb, length, flags, streamId);
                            break;

                        case TYPE_WINDOW_UPDATE:
                            readWindowUpdate(bb, length, flags, streamId);
                            break;

                        case TYPE_CONTINUATION:
                            readContinuation(bb, length, flags, streamId);
                            break;

                        default:
                            // Implementations MUST discard frames that have unknown or unsupported types.
                            bb.recycle();
                    }
                    parseFrameHeader();
                }
                catch (IOException e) {
                    handler.error(e);
                }
            }
        };

        /*
        @Override
        public void readConnectionPreface() throws IOException {
            if (client) return; // Nothing to read; servers doesn't send a connection preface!
            ByteString connectionPreface = source.readByteString(CONNECTION_PREFACE.size());
            if (logger.isLoggable(FINE))
                logger.fine(format("<< CONNECTION %s", connectionPreface.hex()));
            if (!CONNECTION_PREFACE.equals(connectionPreface)) {
                throw ioException("Expected a connection header but was %s", connectionPreface.utf8());
            }
        }
        */

        byte pendingHeaderType;
        private void readHeaders(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (streamId == 0) throw ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0");


            short padding = (flags & FLAG_PADDED) != 0 ? (short) (source.get() & 0xff) : 0;

            if ((flags & FLAG_PRIORITY) != 0) {
                readPriority(source, streamId);
                length -= 5; // account for above read.
            }

            length = lengthWithoutPadding(length, flags, padding);

            pendingHeaderType = type;
            readHeaderBlock(source, length, padding, flags, streamId);

//            handler.headers(false, endStream, streamId, -1, headerBlock, HeadersMode.HTTP_20_HEADERS);
        }

        private void readContinuation(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (streamId != continuingStreamId)
                throw new IOException("continuation stream id mismatch");
            readHeaderBlock(source, length, (short)0, flags, streamId);
        }

        int continuingStreamId;
        private void readHeaderBlock(ByteBufferList source, short length, short padding, byte flags, int streamId)
        throws IOException {
            source.skip(padding);
            hpackReader.refill(source);
            hpackReader.readHeaders();
            hpackReader.emitReferenceSet();
            // TODO: Concat multi-value headers with 0x0, except COOKIE, which uses 0x3B, 0x20.
            // http://tools.ietf.org/html/draft-ietf-httpbis-http2-09#section-8.1.3
            if ((flags & FLAG_END_HEADERS) != 0) {
                if (pendingHeaderType == TYPE_HEADERS) {
                    boolean endStream = (flags & FLAG_END_STREAM) != 0;
                    handler.headers(false, endStream, streamId, -1, hpackReader.getAndReset(), HeadersMode.HTTP_20_HEADERS);
                }
                else if (pendingHeaderType == TYPE_PUSH_PROMISE) {
                    handler.pushPromise(streamId, promisedStreamId, hpackReader.getAndReset());
                }
                else {
                    throw new AssertionError("unknown header type");
                }
            }
            else {
                continuingStreamId = streamId;
            }
        }

        private void readData(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            // TODO: checkState open or half-closed (local) or raise STREAM_CLOSED
            boolean inFinished = (flags & FLAG_END_STREAM) != 0;
            boolean gzipped = (flags & FLAG_COMPRESSED) != 0;
            if (gzipped) {
                throw ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA");
            }

            short padding = (flags & FLAG_PADDED) != 0 ? (short) (source.get() & 0xff) : 0;
            length = lengthWithoutPadding(length, flags, padding);

            handler.data(inFinished, streamId, source);
            source.skip(padding);
        }

        private void readPriority(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (length != 5) throw ioException("TYPE_PRIORITY length: %d != 5", length);
            if (streamId == 0) throw ioException("TYPE_PRIORITY streamId == 0");
            readPriority(source, streamId);
        }

        private void readPriority(ByteBufferList source, int streamId) throws IOException {
            int w1 = source.getInt();
            boolean exclusive = (w1 & 0x80000000) != 0;
            int streamDependency = (w1 & 0x7fffffff);
            int weight = (source.get() & 0xff) + 1;
            handler.priority(streamId, streamDependency, weight, exclusive);
        }

        private void readRstStream(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (length != 4) throw ioException("TYPE_RST_STREAM length: %d != 4", length);
            if (streamId == 0) throw ioException("TYPE_RST_STREAM streamId == 0");
            int errorCodeInt = source.getInt();
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw ioException("TYPE_RST_STREAM unexpected error code: %d", errorCodeInt);
            }
            handler.rstStream(streamId, errorCode);
        }

        private void readSettings(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (streamId != 0) throw ioException("TYPE_SETTINGS streamId != 0");
            if ((flags & FLAG_ACK) != 0) {
                if (length != 0) throw ioException("FRAME_SIZE_ERROR ack frame should be empty!");
                handler.ackSettings();
                return;
            }

            if (length % 6 != 0) throw ioException("TYPE_SETTINGS length %% 6 != 0: %s", length);
            Settings settings = new Settings();
            for (int i = 0; i < length; i += 6) {
                short id = source.getShort();
                int value = source.getInt();

                switch (id) {
                    case 1: // SETTINGS_HEADER_TABLE_SIZE
                        break;
                    case 2: // SETTINGS_ENABLE_PUSH
                        if (value != 0 && value != 1) {
                            throw ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1");
                        }
                        break;
                    case 3: // SETTINGS_MAX_CONCURRENT_STREAMS
                        id = 4; // Renumbered in draft 10.
                        break;
                    case 4: // SETTINGS_INITIAL_WINDOW_SIZE
                        id = 7; // Renumbered in draft 10.
                        if (value < 0) {
                            throw ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1");
                        }
                        break;
                    case 5: // SETTINGS_COMPRESS_DATA
                        break;
                    default:
                        throw ioException("PROTOCOL_ERROR invalid settings id: %s", id);
                }
                settings.set(id, 0, value);
            }
            handler.settings(false, settings);
            if (settings.getHeaderTableSize() >= 0) {
                hpackReader.maxHeaderTableByteCountSetting(settings.getHeaderTableSize());
            }
        }

        int promisedStreamId;
        private void readPushPromise(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (streamId == 0) {
                throw ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0");
            }
            short padding = (flags & FLAG_PADDED) != 0 ? (short) (source.get() & 0xff) : 0;
            promisedStreamId = source.getInt() & 0x7fffffff;
            length -= 4; // account for above read.
            length = lengthWithoutPadding(length, flags, padding);
            pendingHeaderType = TYPE_PUSH_PROMISE;
            readHeaderBlock(source, length, padding, flags, streamId);
        }

        private void readPing(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (length != 8) throw ioException("TYPE_PING length != 8: %s", length);
            if (streamId != 0) throw ioException("TYPE_PING streamId != 0");
            int payload1 = source.getInt();
            int payload2 = source.getInt();
            boolean ack = (flags & FLAG_ACK) != 0;
            handler.ping(ack, payload1, payload2);
        }

        private void readGoAway(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (length < 8) throw ioException("TYPE_GOAWAY length < 8: %s", length);
            if (streamId != 0) throw ioException("TYPE_GOAWAY streamId != 0");
            int lastStreamId = source.getInt();
            int errorCodeInt = source.getInt();
            int opaqueDataLength = length - 8;
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
                throw ioException("TYPE_GOAWAY unexpected error code: %d", errorCodeInt);
            }
            ByteString debugData = ByteString.EMPTY;
            if (opaqueDataLength > 0) { // Must read debug data in order to not corrupt the connection.
                debugData = ByteString.of(source.getBytes(opaqueDataLength));
            }
            handler.goAway(lastStreamId, errorCode, debugData);
        }

        private void readWindowUpdate(ByteBufferList source, short length, byte flags, int streamId)
        throws IOException {
            if (length != 4) throw ioException("TYPE_WINDOW_UPDATE length !=4: %s", length);
            long increment = (source.getInt() & 0x7fffffffL);
            if (increment == 0) throw ioException("windowSizeIncrement was 0", increment);
            handler.windowUpdate(streamId, increment);
        }
    }

    static final class Writer implements FrameWriter {
        private final BufferedDataSink sink;
        private final boolean client;
        private final HpackDraft08.Writer hpackWriter;
        private boolean closed;
        private final ByteBufferList frameHeader = new ByteBufferList();

        Writer(BufferedDataSink sink, boolean client) {
            this.sink = sink;
            this.client = client;
            this.hpackWriter = new HpackDraft08.Writer();
        }

        @Override
        public synchronized void ackSettings() throws IOException {
            if (closed) throw new IOException("closed");
            int length = 0;
            byte type = TYPE_SETTINGS;
            byte flags = FLAG_ACK;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
        }

        @Override
        public synchronized void connectionPreface() throws IOException {
            if (closed) throw new IOException("closed");
            if (!client) return; // Nothing to write; servers don't send connection headers!
            if (logger.isLoggable(FINE)) {
                logger.fine(format(">> CONNECTION %s", CONNECTION_PREFACE.hex()));
            }
            sink.write(new ByteBufferList(CONNECTION_PREFACE.toByteArray()));
        }

        @Override
        public synchronized void synStream(boolean outFinished, boolean inFinished,
                                           int streamId, int associatedStreamId, List<Header> headerBlock)
        throws IOException {
            if (inFinished) throw new UnsupportedOperationException();
            if (closed) throw new IOException("closed");
            headers(outFinished, streamId, headerBlock);
        }

        @Override
        public synchronized void synReply(boolean outFinished, int streamId,
                                          List<Header> headerBlock) throws IOException {
            if (closed) throw new IOException("closed");
            headers(outFinished, streamId, headerBlock);
        }

        @Override
        public synchronized void headers(int streamId, List<com.android.slyce.async.http.spdy.Header> headerBlock)
        throws IOException {
            if (closed) throw new IOException("closed");
            headers(false, streamId, headerBlock);
        }

        @Override
        public synchronized void pushPromise(int streamId, int promisedStreamId,
                                             List<com.android.slyce.async.http.spdy.Header> requestHeaders) throws IOException {
            if (closed) throw new IOException("closed");
            com.android.slyce.async.ByteBufferList hpackBuffer = hpackWriter.writeHeaders(requestHeaders);

            long byteCount = hpackBuffer.remaining();
            int length = (int) Math.min(MAX_FRAME_SIZE - 4, byteCount);
            byte type = TYPE_PUSH_PROMISE;
            byte flags = byteCount == length ? FLAG_END_HEADERS : 0;
            frameHeader(streamId, length + 4, type, flags);
            ByteBuffer sink = com.android.slyce.async.ByteBufferList.obtain(8192).order(ByteOrder.BIG_ENDIAN);
            sink.putInt(promisedStreamId & 0x7fffffff);
            sink.flip();
            frameHeader.add(sink);
            hpackBuffer.get(frameHeader, length);
            this.sink.write(frameHeader);

            if (byteCount > length) writeContinuationFrames(hpackBuffer, streamId);
        }

        void headers(boolean outFinished, int streamId, List<com.android.slyce.async.http.spdy.Header> headerBlock) throws IOException {
            if (closed) throw new IOException("closed");
            com.android.slyce.async.ByteBufferList hpackBuffer = hpackWriter.writeHeaders(headerBlock);

            long byteCount = hpackBuffer.remaining();
            int length = (int) Math.min(MAX_FRAME_SIZE, byteCount);
            byte type = TYPE_HEADERS;
            byte flags = byteCount == length ? FLAG_END_HEADERS : 0;
            if (outFinished) flags |= FLAG_END_STREAM;
            frameHeader(streamId, length, type, flags);
            hpackBuffer.get(frameHeader, length);
            this.sink.write(frameHeader);

            if (byteCount > length) writeContinuationFrames(hpackBuffer, streamId);
        }

        private void writeContinuationFrames(com.android.slyce.async.ByteBufferList hpackBuffer, int streamId) throws IOException {
            while (hpackBuffer.hasRemaining()) {
                int length = (int) Math.min(MAX_FRAME_SIZE, hpackBuffer.remaining());
                int newRemaining = hpackBuffer.remaining() - length;
                frameHeader(streamId, length, TYPE_CONTINUATION, newRemaining == 0 ? FLAG_END_HEADERS : 0);
                hpackBuffer.get(frameHeader, length);
                sink.write(frameHeader);
            }
        }

        @Override
        public synchronized void rstStream(int streamId, ErrorCode errorCode)
        throws IOException {
            if (closed) throw new IOException("closed");
            if (errorCode.spdyRstCode == -1) throw new IllegalArgumentException();

            int length = 4;
            byte type = TYPE_RST_STREAM;
            byte flags = FLAG_NONE;
            frameHeader(streamId, length, type, flags);
            ByteBuffer sink = com.android.slyce.async.ByteBufferList.obtain(8192).order(ByteOrder.BIG_ENDIAN);
            sink.putInt(errorCode.httpCode);
            sink.flip();
            this.sink.write(frameHeader.add(sink));
        }

        @Override
        public synchronized void data(boolean outFinished, int streamId, com.android.slyce.async.ByteBufferList source)
        throws IOException {
            if (closed) throw new IOException("closed");
            byte flags = FLAG_NONE;
            if (outFinished) flags |= FLAG_END_STREAM;
            dataFrame(streamId, flags, source);
        }

        void dataFrame(int streamId, byte flags, com.android.slyce.async.ByteBufferList buffer) throws IOException {
            byte type = TYPE_DATA;
            frameHeader(streamId, buffer.remaining(), type, flags);
            sink.write(buffer);
        }

        @Override
        public synchronized void settings(Settings settings) throws IOException {
            if (closed) throw new IOException("closed");
            int length = settings.size() * 6;
            byte type = TYPE_SETTINGS;
            byte flags = FLAG_NONE;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
            ByteBuffer sink = com.android.slyce.async.ByteBufferList.obtain(8192).order(ByteOrder.BIG_ENDIAN);
            for (int i = 0; i < Settings.COUNT; i++) {
                if (!settings.isSet(i)) continue;
                int id = i;
                if (id == 4) id = 3; // SETTINGS_MAX_CONCURRENT_STREAMS renumbered.
                else if (id == 7) id = 4; // SETTINGS_INITIAL_WINDOW_SIZE renumbered.
                sink.putShort((short) id);
                sink.putInt(settings.get(i));
            }
            sink.flip();
            this.sink.write(frameHeader.add(sink));
        }

        @Override
        public synchronized void ping(boolean ack, int payload1, int payload2)
        throws IOException {
            if (closed) throw new IOException("closed");
            int length = 8;
            byte type = TYPE_PING;
            byte flags = ack ? FLAG_ACK : FLAG_NONE;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
            ByteBuffer sink = com.android.slyce.async.ByteBufferList.obtain(256).order(ByteOrder.BIG_ENDIAN);
            sink.putInt(payload1);
            sink.putInt(payload2);
            sink.flip();
            this.sink.write(frameHeader.add(sink));
        }

        @Override
        public synchronized void goAway(int lastGoodStreamId, ErrorCode errorCode,
                                        byte[] debugData) throws IOException {
            if (closed) throw new IOException("closed");
            if (errorCode.httpCode == -1) throw illegalArgument("errorCode.httpCode == -1");
            int length = 8 + debugData.length;
            byte type = TYPE_GOAWAY;
            byte flags = FLAG_NONE;
            int streamId = 0;
            frameHeader(streamId, length, type, flags);
            ByteBuffer sink = com.android.slyce.async.ByteBufferList.obtain(256).order(ByteOrder.BIG_ENDIAN);
            sink.putInt(lastGoodStreamId);
            sink.putInt(errorCode.httpCode);
            sink.put(debugData);
            sink.flip();
            this.sink.write(frameHeader.add(sink));
        }

        @Override
        public synchronized void windowUpdate(int streamId, long windowSizeIncrement)
        throws IOException {
            if (closed) throw new IOException("closed");
            if (windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL) {
                throw illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s",
                windowSizeIncrement);
            }
            int length = 4;
            byte type = TYPE_WINDOW_UPDATE;
            byte flags = FLAG_NONE;
            frameHeader(streamId, length, type, flags);
            ByteBuffer sink = com.android.slyce.async.ByteBufferList.obtain(256).order(ByteOrder.BIG_ENDIAN);
            sink.putInt((int) windowSizeIncrement);
            sink.flip();
            this.sink.write(frameHeader.add(sink));
        }

        @Override
        public synchronized void close() throws IOException {
            closed = true;
        }

        void frameHeader(int streamId, int length, byte type, byte flags) throws IOException {
            if (logger.isLoggable(FINE))
                logger.fine(formatHeader(false, streamId, length, type, flags));
            if (length > MAX_FRAME_SIZE) {
                throw illegalArgument("FRAME_SIZE_ERROR length > %d: %d", MAX_FRAME_SIZE, length);
            }
            if ((streamId & 0x80000000) != 0)
                throw illegalArgument("reserved bit set: %s", streamId);
            ByteBuffer sink = com.android.slyce.async.ByteBufferList.obtain(256).order(ByteOrder.BIG_ENDIAN);
            sink.putInt((length & 0x3fff) << 16 | (type & 0xff) << 8 | (flags & 0xff));
            sink.putInt(streamId & 0x7fffffff);
            sink.flip();
            this.sink.write(frameHeader.add(sink));
        }
    }

    private static IllegalArgumentException illegalArgument(String message, Object... args) {
        throw new IllegalArgumentException(format(message, args));
    }

    private static IOException ioException(String message, Object... args) throws IOException {
        throw new IOException(format(message, args));
    }

    private static short lengthWithoutPadding(short length, byte flags, short padding)
    throws IOException {
        if ((flags & FLAG_PADDED) != 0) length--; // Account for reading the padding length.
        if (padding > length) {
            throw ioException("PROTOCOL_ERROR padding %s > remaining length %s", padding, length);
        }
        return (short) (length - padding);
    }

    /**
     * Logs a human-readable representation of HTTP/2 frame headers.
     * <p/>
     * <p>The format is:
     * <p/>
     * <pre>
     *   direction streamID length type flags
     * </pre>
     * Where direction is {@code <<} for inbound and {@code >>} for outbound.
     * <p/>
     * <p> For example, the following would indicate a HEAD request sent from
     * the client.
     * <pre>
     * {@code
     *   << 0x0000000f    12 HEADERS       END_HEADERS|END_STREAM
     * }
     * </pre>
     */
    static final class FrameLogger {

        static String formatHeader(boolean inbound, int streamId, int length, byte type, byte flags) {
            String formattedType = type < TYPES.length ? TYPES[type] : format("0x%02x", type);
            String formattedFlags = formatFlags(type, flags);
            return format("%s 0x%08x %5d %-13s %s", inbound ? "<<" : ">>", streamId, length,
            formattedType, formattedFlags);
        }

        /**
         * Looks up valid string representing flags from the table. Invalid
         * combinations are represented in binary.
         */
        // Visible for testing.
        static String formatFlags(byte type, byte flags) {
            if (flags == 0) return "";
            switch (type) { // Special case types that have 0 or 1 flag.
                case TYPE_SETTINGS:
                case TYPE_PING:
                    return flags == FLAG_ACK ? "ACK" : BINARY[flags];
                case TYPE_PRIORITY:
                case TYPE_RST_STREAM:
                case TYPE_GOAWAY:
                case TYPE_WINDOW_UPDATE:
                    return BINARY[flags];
            }
            String result = flags < FLAGS.length ? FLAGS[flags] : BINARY[flags];
            // Special case types that have overlap flag values.
            if (type == TYPE_PUSH_PROMISE && (flags & FLAG_END_PUSH_PROMISE) != 0) {
                return result.replace("HEADERS", "PUSH_PROMISE"); // TODO: Avoid allocation.
            } else if (type == TYPE_DATA && (flags & FLAG_COMPRESSED) != 0) {
                return result.replace("PRIORITY", "COMPRESSED"); // TODO: Avoid allocation.
            }
            return result;
        }

        /**
         * Lookup table for valid frame types.
         */
        private static final String[] TYPES = new String[]{
        "DATA",
        "HEADERS",
        "PRIORITY",
        "RST_STREAM",
        "SETTINGS",
        "PUSH_PROMISE",
        "PING",
        "GOAWAY",
        "WINDOW_UPDATE",
        "CONTINUATION"
        };

        /**
         * Lookup table for valid flags for DATA, HEADERS, CONTINUATION. Invalid
         * combinations are represented in binary.
         */
        private static final String[] FLAGS = new String[0x40]; // Highest bit flag is 0x20.
        private static final String[] BINARY = new String[256];

        static {
            for (int i = 0; i < BINARY.length; i++) {
                BINARY[i] = format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
            }

            FLAGS[FLAG_NONE] = "";
            FLAGS[FLAG_END_STREAM] = "END_STREAM";
            FLAGS[FLAG_END_SEGMENT] = "END_SEGMENT";
            FLAGS[FLAG_END_STREAM | FLAG_END_SEGMENT] = "END_STREAM|END_SEGMENT";
            int[] prefixFlags =
            new int[]{FLAG_END_STREAM, FLAG_END_SEGMENT, FLAG_END_SEGMENT | FLAG_END_STREAM};

            FLAGS[FLAG_PADDED] = "PADDED";
            for (int prefixFlag : prefixFlags) {
                FLAGS[prefixFlag | FLAG_PADDED] = FLAGS[prefixFlag] + "|PADDED";
            }

            FLAGS[FLAG_END_HEADERS] = "END_HEADERS"; // Same as END_PUSH_PROMISE.
            FLAGS[FLAG_PRIORITY] = "PRIORITY"; // Same as FLAG_COMPRESSED.
            FLAGS[FLAG_END_HEADERS | FLAG_PRIORITY] = "END_HEADERS|PRIORITY"; // Only valid on HEADERS.
            int[] frameFlags =
            new int[]{FLAG_END_HEADERS, FLAG_PRIORITY, FLAG_END_HEADERS | FLAG_PRIORITY};

            for (int frameFlag : frameFlags) {
                for (int prefixFlag : prefixFlags) {
                    FLAGS[prefixFlag | frameFlag] = FLAGS[prefixFlag] + '|' + FLAGS[frameFlag];
                    FLAGS[prefixFlag | frameFlag | FLAG_PADDED] =
                    FLAGS[prefixFlag] + '|' + FLAGS[frameFlag] + "|PADDED";
                }
            }

            for (int i = 0; i < FLAGS.length; i++) { // Fill in holes with binary representation.
                if (FLAGS[i] == null) FLAGS[i] = BINARY[i];
            }
        }
    }
}
