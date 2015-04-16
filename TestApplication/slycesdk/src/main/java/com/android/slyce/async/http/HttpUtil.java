package com.android.slyce.async.http;

public class HttpUtil {
    public static com.android.slyce.async.http.body.AsyncHttpRequestBody getBody(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.callback.CompletedCallback reporter, com.android.slyce.async.http.Headers headers) {
        String contentType = headers.get("Content-Type");
        if (contentType != null) {
            String[] values = contentType.split(";");
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i].trim();
            }
            for (String ct: values) {
                if (com.android.slyce.async.http.body.UrlEncodedFormBody.CONTENT_TYPE.equals(ct)) {
                    return new com.android.slyce.async.http.body.UrlEncodedFormBody();
                }
                if (com.android.slyce.async.http.body.JSONObjectBody.CONTENT_TYPE.equals(ct)) {
                    return new com.android.slyce.async.http.body.JSONObjectBody();
                }
                if (com.android.slyce.async.http.body.StringBody.CONTENT_TYPE.equals(ct)) {
                    return new com.android.slyce.async.http.body.StringBody();
                }
                if (com.android.slyce.async.http.body.MultipartFormDataBody.CONTENT_TYPE.equals(ct)) {
                    return new com.android.slyce.async.http.body.MultipartFormDataBody(values);
                }
            }
        }

        return null;
    }

    static class EndEmitter extends com.android.slyce.async.FilteredDataEmitter {
        private EndEmitter() {
        }

        public static EndEmitter create(com.android.slyce.async.AsyncServer server, final Exception e) {
            final EndEmitter ret = new EndEmitter();
            // don't need to worry about any race conditions with post and this return value
            // since we are in the server thread.
            server.post(new Runnable() {
                @Override
                public void run() {
                    ret.report(e);
                }
            });
            return ret;
        }
    }

    public static com.android.slyce.async.DataEmitter getBodyDecoder(com.android.slyce.async.DataEmitter emitter, com.android.slyce.async.http.Protocol protocol, com.android.slyce.async.http.Headers headers, boolean server) {
        long _contentLength;
        try {
            _contentLength = Long.parseLong(headers.get("Content-Length"));
        }
        catch (Exception ex) {
            _contentLength = -1;
        }
        final long contentLength = _contentLength;
        if (-1 != contentLength) {
            if (contentLength < 0) {
                EndEmitter ender = EndEmitter.create(emitter.getServer(), new BodyDecoderException("not using chunked encoding, and no content-length found."));
                ender.setDataEmitter(emitter);
                emitter = ender;
                return emitter;
            }
            if (contentLength == 0) {
                EndEmitter ender = EndEmitter.create(emitter.getServer(), null);
                ender.setDataEmitter(emitter);
                emitter = ender;
                return emitter;
            }
            com.android.slyce.async.http.filter.ContentLengthFilter contentLengthWatcher = new com.android.slyce.async.http.filter.ContentLengthFilter(contentLength);
            contentLengthWatcher.setDataEmitter(emitter);
            emitter = contentLengthWatcher;
        }
        else if ("chunked".equalsIgnoreCase(headers.get("Transfer-Encoding"))) {
            com.android.slyce.async.http.filter.ChunkedInputFilter chunker = new com.android.slyce.async.http.filter.ChunkedInputFilter();
            chunker.setDataEmitter(emitter);
            emitter = chunker;
        }
        else {
            if ((server || protocol == com.android.slyce.async.http.Protocol.HTTP_1_1) && !"close".equalsIgnoreCase(headers.get("Connection"))) {
                // if this is the server, and the client has not indicated a request body, the client is done
                EndEmitter ender = EndEmitter.create(emitter.getServer(), null);
                ender.setDataEmitter(emitter);
                emitter = ender;
                return emitter;
            }
        }

        if ("gzip".equals(headers.get("Content-Encoding"))) {
            com.android.slyce.async.http.filter.GZIPInputFilter gunzipper = new com.android.slyce.async.http.filter.GZIPInputFilter();
            gunzipper.setDataEmitter(emitter);
            emitter = gunzipper;
        }
        else if ("deflate".equals(headers.get("Content-Encoding"))) {
            com.android.slyce.async.http.filter.InflaterInputFilter inflater = new com.android.slyce.async.http.filter.InflaterInputFilter();
            inflater.setDataEmitter(emitter);
            emitter = inflater;
        }

        // conversely, if this is the client (http 1.0), and the server has not indicated a request body, we do not report
        // the close/end event until the server actually closes the connection.
        return emitter;
    }

    public static boolean isKeepAlive(com.android.slyce.async.http.Protocol protocol, com.android.slyce.async.http.Headers headers) {
        // connection is always keep alive as this is an http/1.1 client
        String connection = headers.get("Connection");
        if (connection == null)
            return protocol == com.android.slyce.async.http.Protocol.HTTP_1_1;
        return "keep-alive".equalsIgnoreCase(connection);
    }

    public static boolean isKeepAlive(String protocol, com.android.slyce.async.http.Headers headers) {
        // connection is always keep alive as this is an http/1.1 client
        String connection = headers.get("Connection");
        if (connection == null)
            return com.android.slyce.async.http.Protocol.get(protocol) == com.android.slyce.async.http.Protocol.HTTP_1_1;
        return "keep-alive".equalsIgnoreCase(connection);
    }

    public static int contentLength(com.android.slyce.async.http.Headers headers) {
        String cl = headers.get("Content-Length");
        if (cl == null)
            return -1;
        try {
            return Integer.parseInt(cl);
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }
}
