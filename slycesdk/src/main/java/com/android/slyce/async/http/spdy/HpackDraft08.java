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

import com.android.slyce.async.ByteBufferList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read and write HPACK v08.
 * <p/>
 * http://tools.ietf.org/html/draft-ietf-httpbis-header-compression-08
 * <p/>
 * This implementation uses an array for the header table with a bitset for
 * references.  Dynamic entries are added to the array, starting in the last
 * position moving forward.  When the array fills, it is doubled.
 */
final class HpackDraft08 {
    private static final int PREFIX_4_BITS = 0x0f;
    private static final int PREFIX_6_BITS = 0x3f;
    private static final int PREFIX_7_BITS = 0x7f;

    private static final com.android.slyce.async.http.spdy.Header[] STATIC_HEADER_TABLE = new com.android.slyce.async.http.spdy.Header[]{
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_AUTHORITY, ""),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_METHOD, "GET"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_METHOD, "POST"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_PATH, "/"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_PATH, "/index.html"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_SCHEME, "http"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.TARGET_SCHEME, "https"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS, "200"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS, "204"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS, "206"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS, "304"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS, "400"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS, "404"),
    new com.android.slyce.async.http.spdy.Header(com.android.slyce.async.http.spdy.Header.RESPONSE_STATUS, "500"),
    new com.android.slyce.async.http.spdy.Header("accept-charset", ""),
    new com.android.slyce.async.http.spdy.Header("accept-encoding", "gzip, deflate"),
    new com.android.slyce.async.http.spdy.Header("accept-language", ""),
    new com.android.slyce.async.http.spdy.Header("accept-ranges", ""),
    new com.android.slyce.async.http.spdy.Header("accept", ""),
    new com.android.slyce.async.http.spdy.Header("access-control-allow-origin", ""),
    new com.android.slyce.async.http.spdy.Header("age", ""),
    new com.android.slyce.async.http.spdy.Header("allow", ""),
    new com.android.slyce.async.http.spdy.Header("authorization", ""),
    new com.android.slyce.async.http.spdy.Header("cache-control", ""),
    new com.android.slyce.async.http.spdy.Header("content-disposition", ""),
    new com.android.slyce.async.http.spdy.Header("content-encoding", ""),
    new com.android.slyce.async.http.spdy.Header("content-language", ""),
    new com.android.slyce.async.http.spdy.Header("content-length", ""),
    new Header("content-location", ""),
    new Header("content-range", ""),
    new Header("content-type", ""),
    new Header("cookie", ""),
    new Header("date", ""),
    new Header("etag", ""),
    new Header("expect", ""),
    new Header("expires", ""),
    new Header("from", ""),
    new Header("host", ""),
    new Header("if-match", ""),
    new Header("if-modified-since", ""),
    new Header("if-none-match", ""),
    new Header("if-range", ""),
    new Header("if-unmodified-since", ""),
    new Header("last-modified", ""),
    new Header("link", ""),
    new Header("location", ""),
    new Header("max-forwards", ""),
    new Header("proxy-authenticate", ""),
    new Header("proxy-authorization", ""),
    new Header("range", ""),
    new Header("referer", ""),
    new Header("refresh", ""),
    new Header("retry-after", ""),
    new Header("server", ""),
    new Header("set-cookie", ""),
    new Header("strict-transport-security", ""),
    new Header("transfer-encoding", ""),
    new Header("user-agent", ""),
    new Header("vary", ""),
    new Header("via", ""),
    new Header("www-authenticate", "")
    };

    private HpackDraft08() {
    }

    // http://tools.ietf.org/html/draft-ietf-httpbis-header-compression-08#section-3.2
    static final class Reader {

        private final List<Header> emittedHeaders = new ArrayList<Header>();
        private final ByteBufferList source = new ByteBufferList();

        private int maxHeaderTableByteCountSetting;
        private int maxHeaderTableByteCount;
        // Visible for testing.
        Header[] headerTable = new Header[8];
        // Array is populated back to front, so new entries always have lowest index.
        int nextHeaderIndex = headerTable.length - 1;
        int headerCount = 0;

        /**
         * Set bit positions indicate {@code headerTable[pos]} should be emitted.
         */
        // Using a BitArray as it has left-shift operator.
        BitArray referencedHeaders = new com.android.slyce.async.http.spdy.BitArray.FixedCapacity();

        /**
         * Set bit positions indicate {@code headerTable[pos]} was already emitted.
         */
        BitArray emittedReferencedHeaders = new com.android.slyce.async.http.spdy.BitArray.FixedCapacity();
        int headerTableByteCount = 0;

        Reader(int maxHeaderTableByteCountSetting) {
            this.maxHeaderTableByteCountSetting = maxHeaderTableByteCountSetting;
            this.maxHeaderTableByteCount = maxHeaderTableByteCountSetting;
        }

        public void refill(ByteBufferList bb) {
            bb.get(source);
        }

        int maxHeaderTableByteCount() {
            return maxHeaderTableByteCount;
        }

        /**
         * Called by the reader when the peer sent a new header table size setting.
         * While this establishes the maximum header table size, the
         * {@link #maxHeaderTableByteCount} set during processing may limit the
         * table size to a smaller amount.
         * <p> Evicts entries or clears the table as needed.
         */
        void maxHeaderTableByteCountSetting(int newMaxHeaderTableByteCountSetting) {
            this.maxHeaderTableByteCountSetting = newMaxHeaderTableByteCountSetting;
            this.maxHeaderTableByteCount = maxHeaderTableByteCountSetting;
            adjustHeaderTableByteCount();
        }

        private void adjustHeaderTableByteCount() {
            if (maxHeaderTableByteCount < headerTableByteCount) {
                if (maxHeaderTableByteCount == 0) {
                    clearHeaderTable();
                } else {
                    evictToRecoverBytes(headerTableByteCount - maxHeaderTableByteCount);
                }
            }
        }

        private void clearHeaderTable() {
            clearReferenceSet();
            Arrays.fill(headerTable, null);
            nextHeaderIndex = headerTable.length - 1;
            headerCount = 0;
            headerTableByteCount = 0;
        }

        /**
         * Returns the count of entries evicted.
         */
        private int evictToRecoverBytes(int bytesToRecover) {
            int entriesToEvict = 0;
            if (bytesToRecover > 0) {
                // determine how many headers need to be evicted.
                for (int j = headerTable.length - 1; j >= nextHeaderIndex && bytesToRecover > 0; j--) {
                    bytesToRecover -= headerTable[j].hpackSize;
                    headerTableByteCount -= headerTable[j].hpackSize;
                    headerCount--;
                    entriesToEvict++;
                }
                referencedHeaders.shiftLeft(entriesToEvict);
                emittedReferencedHeaders.shiftLeft(entriesToEvict);
                System.arraycopy(headerTable, nextHeaderIndex + 1, headerTable,
                nextHeaderIndex + 1 + entriesToEvict, headerCount);
                nextHeaderIndex += entriesToEvict;
            }
            return entriesToEvict;
        }

        /**
         * Read {@code byteCount} bytes of headers from the source stream into the
         * set of emitted headers. This implementation does not propagate the never
         * indexed flag of a header.
         */
        void readHeaders() throws IOException {
            while (source.hasRemaining()) {
                int b = source.get() & 0xff;
                if (b == 0x80) { // 10000000
                    throw new IOException("index == 0");
                } else if ((b & 0x80) == 0x80) { // 1NNNNNNN
                    int index = readInt(b, PREFIX_7_BITS);
                    readIndexedHeader(index - 1);
                } else if (b == 0x40) { // 01000000
                    readLiteralHeaderWithIncrementalIndexingNewName();
                } else if ((b & 0x40) == 0x40) {  // 01NNNNNN
                    int index = readInt(b, PREFIX_6_BITS);
                    readLiteralHeaderWithIncrementalIndexingIndexedName(index - 1);
                } else if ((b & 0x20) == 0x20) {  // 001NNNNN
                    if ((b & 0x10) == 0x10) { // 0011NNNN
                        if ((b & 0x0f) != 0)
                            throw new IOException("Invalid header table state change " + b);
                        clearReferenceSet(); // 00110000
                    } else { // 0010NNNN
                        maxHeaderTableByteCount = readInt(b, PREFIX_4_BITS);
                        if (maxHeaderTableByteCount < 0
                        || maxHeaderTableByteCount > maxHeaderTableByteCountSetting) {
                            throw new IOException("Invalid header table byte count " + maxHeaderTableByteCount);
                        }
                        adjustHeaderTableByteCount();
                    }
                } else if (b == 0x10 || b == 0) { // 000?0000 - Ignore never indexed bit.
                    readLiteralHeaderWithoutIndexingNewName();
                } else { // 000?NNNN - Ignore never indexed bit.
                    int index = readInt(b, PREFIX_4_BITS);
                    readLiteralHeaderWithoutIndexingIndexedName(index - 1);
                }
            }
        }

        private void clearReferenceSet() {
            referencedHeaders.clear();
            emittedReferencedHeaders.clear();
        }

        void emitReferenceSet() {
            for (int i = headerTable.length - 1; i != nextHeaderIndex; --i) {
                if (referencedHeaders.get(i) && !emittedReferencedHeaders.get(i)) {
                    emittedHeaders.add(headerTable[i]);
                }
            }
        }

        /**
         * Returns all headers emitted since they were last cleared, then clears the
         * emitted headers.
         */
        List<Header> getAndReset() {
            List<Header> result = new ArrayList<Header>(emittedHeaders);
            emittedHeaders.clear();
            emittedReferencedHeaders.clear();
            return result;
        }

        private void readIndexedHeader(int index) throws IOException {
            if (isStaticHeader(index)) {
                index -= headerCount;
                if (index > STATIC_HEADER_TABLE.length - 1) {
                    throw new IOException("Header index too large " + (index + 1));
                }
                Header staticEntry = STATIC_HEADER_TABLE[index];
                if (maxHeaderTableByteCount == 0) {
                    emittedHeaders.add(staticEntry);
                } else {
                    insertIntoHeaderTable(-1, staticEntry);
                }
            } else {
                int headerTableIndex = headerTableIndex(index);
                if (!referencedHeaders.get(headerTableIndex)) { // When re-referencing, emit immediately.
                    emittedHeaders.add(headerTable[headerTableIndex]);
                    emittedReferencedHeaders.set(headerTableIndex);
                }
                referencedHeaders.toggle(headerTableIndex);
            }
        }

        // referencedHeaders is relative to nextHeaderIndex + 1.
        private int headerTableIndex(int index) {
            return nextHeaderIndex + 1 + index;
        }

        private void readLiteralHeaderWithoutIndexingIndexedName(int index) throws IOException {
            ByteString name = getName(index);
            ByteString value = readByteString();
            emittedHeaders.add(new Header(name, value));
        }

        private void readLiteralHeaderWithoutIndexingNewName() throws IOException {
            ByteString name = checkLowercase(readByteString());
            ByteString value = readByteString();
            emittedHeaders.add(new Header(name, value));
        }

        private void readLiteralHeaderWithIncrementalIndexingIndexedName(int nameIndex)
        throws IOException {
            ByteString name = getName(nameIndex);
            ByteString value = readByteString();
            insertIntoHeaderTable(-1, new Header(name, value));
        }

        private void readLiteralHeaderWithIncrementalIndexingNewName() throws IOException {
            ByteString name = checkLowercase(readByteString());
            ByteString value = readByteString();
            insertIntoHeaderTable(-1, new Header(name, value));
        }

        private ByteString getName(int index) {
            if (isStaticHeader(index)) {
                return STATIC_HEADER_TABLE[index - headerCount].name;
            } else {
                return headerTable[headerTableIndex(index)].name;
            }
        }

        private boolean isStaticHeader(int index) {
            return index >= headerCount;
        }

        /**
         * index == -1 when new.
         */
        private void insertIntoHeaderTable(int index, Header entry) {
            int delta = entry.hpackSize;
            if (index != -1) { // Index -1 == new header.
                delta -= headerTable[headerTableIndex(index)].hpackSize;
            }

            // if the new or replacement header is too big, drop all entries.
            if (delta > maxHeaderTableByteCount) {
                clearHeaderTable();
                // emit the large header to the callback.
                emittedHeaders.add(entry);
                return;
            }

            // Evict headers to the required length.
            int bytesToRecover = (headerTableByteCount + delta) - maxHeaderTableByteCount;
            int entriesEvicted = evictToRecoverBytes(bytesToRecover);

            if (index == -1) { // Adding a value to the header table.
                if (headerCount + 1 > headerTable.length) { // Need to grow the header table.
                    Header[] doubled = new Header[headerTable.length * 2];
                    System.arraycopy(headerTable, 0, doubled, headerTable.length, headerTable.length);
                    if (doubled.length == 64) {
                        referencedHeaders = ((com.android.slyce.async.http.spdy.BitArray.FixedCapacity) referencedHeaders).toVariableCapacity();
                        emittedReferencedHeaders =
                        ((com.android.slyce.async.http.spdy.BitArray.FixedCapacity) emittedReferencedHeaders).toVariableCapacity();
                    }
                    referencedHeaders.shiftLeft(headerTable.length);
                    emittedReferencedHeaders.shiftLeft(headerTable.length);
                    nextHeaderIndex = headerTable.length - 1;
                    headerTable = doubled;
                }
                index = nextHeaderIndex--;
                referencedHeaders.set(index);
                headerTable[index] = entry;
                headerCount++;
            } else { // Replace value at same position.
                index += headerTableIndex(index) + entriesEvicted;
                referencedHeaders.set(index);
                headerTable[index] = entry;
            }
            headerTableByteCount += delta;
        }

        private int readByte() throws IOException {
            return source.get() & 0xff;
        }

        int readInt(int firstByte, int prefixMask) throws IOException {
            int prefix = firstByte & prefixMask;
            if (prefix < prefixMask) {
                return prefix; // This was a single byte value.
            }

            // This is a multibyte value. Read 7 bits at a time.
            int result = prefixMask;
            int shift = 0;
            while (true) {
                int b = readByte();
                if ((b & 0x80) != 0) { // Equivalent to (b >= 128) since b is in [0..255].
                    result += (b & 0x7f) << shift;
                    shift += 7;
                } else {
                    result += b << shift; // Last byte.
                    break;
                }
            }
            return result;
        }

        /**
         * Reads a potentially Huffman encoded byte string.
         */
        ByteString readByteString() throws IOException {
            int firstByte = readByte();
            boolean huffmanDecode = (firstByte & 0x80) == 0x80; // 1NNNNNNN
            int length = readInt(firstByte, PREFIX_7_BITS);

            if (huffmanDecode) {
                return ByteString.of(Huffman.get().decode(source.getBytes(length)));
            } else {
                return ByteString.of(source.getBytes(length));
            }
        }
    }

    private static final Map<ByteString, Integer> NAME_TO_FIRST_INDEX = nameToFirstIndex();

    private static Map<ByteString, Integer> nameToFirstIndex() {
        Map<ByteString, Integer> result = new LinkedHashMap<ByteString, Integer>(STATIC_HEADER_TABLE.length);
        for (int i = 0; i < STATIC_HEADER_TABLE.length; i++) {
            if (!result.containsKey(STATIC_HEADER_TABLE[i].name)) {
                result.put(STATIC_HEADER_TABLE[i].name, i);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    static final class Writer {
        Writer() {
        }

        /**
         * This does not use "never indexed" semantics for sensitive headers.
         */
        // https://tools.ietf.org/html/draft-ietf-httpbis-header-compression-08#section-4.3.3
        ByteBufferList writeHeaders(List<Header> headerBlock) throws IOException {
            ByteBufferList out = new ByteBufferList();
            // TODO: implement index tracking
            ByteBuffer current = ByteBufferList.obtain(8192);
            for (int i = 0, size = headerBlock.size(); i < size; i++) {
                if (current.remaining() < current.capacity() / 2) {
                    current.flip();
                    out.add(current);
                    current = ByteBufferList.obtain(current.capacity() * 2);
                }
                ByteString name = headerBlock.get(i).name.toAsciiLowercase();
                Integer staticIndex = NAME_TO_FIRST_INDEX.get(name);
                if (staticIndex != null) {
                    // Literal Header Field without Indexing - Indexed Name.
                    writeInt(current, staticIndex + 1, PREFIX_4_BITS, 0);
                    writeByteString(current, headerBlock.get(i).value);
                } else {
                    current.put((byte) 0x00); // Literal Header without Indexing - New Name.
                    writeByteString(current, name);
                    writeByteString(current, headerBlock.get(i).value);
                }
            }

            out.add(current);
            return out;
        }

        // http://tools.ietf.org/html/draft-ietf-httpbis-header-compression-08#section-4.1.1
        void writeInt(ByteBuffer out, int value, int prefixMask, int bits) throws IOException {
            // Write the raw value for a single byte value.
            if (value < prefixMask) {
                out.put((byte) (bits | value));
                return;
            }

            // Write the mask to start a multibyte value.
            out.put((byte)(bits | prefixMask));
            value -= prefixMask;

            // Write 7 bits at a time 'til we're done.
            while (value >= 0x80) {
                int b = value & 0x7f;
                out.put((byte) (b | 0x80));
                value >>>= 7;
            }
            out.put((byte) value);
        }

        void writeByteString(ByteBuffer out, ByteString data) throws IOException {
            writeInt(out, data.size(), PREFIX_7_BITS, 0);
            out.put(data.toByteArray());
        }
    }

    /**
     * An HTTP/2 response cannot contain uppercase header characters and must
     * be treated as malformed.
     */
    private static ByteString checkLowercase(ByteString name) throws IOException {
        for (int i = 0, length = name.size(); i < length; i++) {
            byte c = name.getByte(i);
            if (c >= 'A' && c <= 'Z') {
                throw new IOException("PROTOCOL_ERROR response malformed: mixed case name: " + name.utf8());
            }
        }
        return name;
    }
}
