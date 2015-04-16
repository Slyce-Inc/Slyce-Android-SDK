package com.android.slyce.async;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDataSink extends com.android.slyce.async.FilteredDataSink {
    public ZipDataSink(com.android.slyce.async.DataSink sink) {
        super(sink);
    }

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    ZipOutputStream zop = new ZipOutputStream(bout);

    public void putNextEntry(ZipEntry ze) throws IOException {
        zop.putNextEntry(ze);
    }

    public void closeEntry() throws IOException {
        zop.closeEntry();
    }

    protected void report(Exception e) {
        com.android.slyce.async.callback.CompletedCallback closed = getClosedCallback();
        if (closed != null)
            closed.onCompleted(e);
    }

    @Override
    public void end() {
        try {
            zop.close();
        }
        catch (IOException e) {
            report(e);
            return;
        }
        setMaxBuffer(Integer.MAX_VALUE);
        write(new com.android.slyce.async.ByteBufferList());
        super.end();
    }

    @Override
    public com.android.slyce.async.ByteBufferList filter(com.android.slyce.async.ByteBufferList bb) {
        try {
            if (bb != null) {
                while (bb.size() > 0) {
                    ByteBuffer b = bb.remove();
                    com.android.slyce.async.ByteBufferList.writeOutputStream(zop, b);
                    com.android.slyce.async.ByteBufferList.reclaim(b);
                }
            }
            com.android.slyce.async.ByteBufferList ret = new com.android.slyce.async.ByteBufferList(bout.toByteArray());
            bout.reset();
            return ret;
        }
        catch (IOException e) {
            report(e);
            return null;
        }
        finally {
            if (bb != null)
                bb.recycle();
        }
    }
}
