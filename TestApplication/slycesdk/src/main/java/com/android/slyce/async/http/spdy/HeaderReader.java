package com.android.slyce.async.http.spdy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Created by koush on 7/27/14.
 */
class HeaderReader {
    Inflater inflater;
    public HeaderReader() {
        inflater = new Inflater() {
            @Override public int inflate(byte[] buffer, int offset, int count)
            throws DataFormatException {
                int result = super.inflate(buffer, offset, count);
                if (result == 0 && needsDictionary()) {
                    setDictionary(Spdy3.DICTIONARY);
                    result = super.inflate(buffer, offset, count);
                }
                return result;
            }
        };
    }

    public List<com.android.slyce.async.http.spdy.Header> readHeader(com.android.slyce.async.ByteBufferList bb, int length) throws IOException {
        byte[] bytes = new byte[length];
        bb.get(bytes);

        inflater.setInput(bytes);

        com.android.slyce.async.ByteBufferList source = new com.android.slyce.async.ByteBufferList().order(ByteOrder.BIG_ENDIAN);
        while (!inflater.needsInput()) {
            ByteBuffer b = com.android.slyce.async.ByteBufferList.obtain(8192);
            try {
                int read = inflater.inflate(b.array());
                b.limit(read);
                source.add(b);
            }
            catch (DataFormatException e) {
                throw new IOException(e);
            }
        }

        int numberOfPairs = source.getInt();
        List<com.android.slyce.async.http.spdy.Header> entries = new ArrayList<com.android.slyce.async.http.spdy.Header>(numberOfPairs);
        for (int i = 0; i < numberOfPairs; i++) {
            ByteString name = readByteString(source).toAsciiLowercase();
            ByteString values = readByteString(source);
            if (name.size() == 0) throw new IOException("name.size == 0");
            entries.add(new com.android.slyce.async.http.spdy.Header(name, values));
        }
        return entries;
    }

    private static ByteString readByteString(com.android.slyce.async.ByteBufferList source) {
        int length = source.getInt();
        return ByteString.of(source.getBytes(length));
    }
}
