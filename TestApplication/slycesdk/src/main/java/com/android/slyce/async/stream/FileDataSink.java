package com.android.slyce.async.stream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by koush on 2/2/14.
 */
public class FileDataSink extends com.android.slyce.async.stream.OutputStreamDataSink {
    File file;
    public FileDataSink(com.android.slyce.async.AsyncServer server, File file) {
        super(server);
        this.file = file;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        OutputStream ret = super.getOutputStream();
        if (ret == null) {
            ret = new FileOutputStream(file);
            setOutputStream(ret);
        }
        return ret;
    }
}
