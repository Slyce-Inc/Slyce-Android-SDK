package com.android.slyce.async.http;

import android.text.TextUtils;

import java.io.IOException;

/**
 * Created by koush on 7/24/14.
 */
public class HttpTransportMiddleware extends com.android.slyce.async.http.SimpleMiddleware {
    @Override
    public boolean exchangeHeaders(final OnExchangeHeaderData data) {
        com.android.slyce.async.http.Protocol p = com.android.slyce.async.http.Protocol.get(data.protocol);
        if (p != null && p != com.android.slyce.async.http.Protocol.HTTP_1_0 && p != com.android.slyce.async.http.Protocol.HTTP_1_1)
            return super.exchangeHeaders(data);

        AsyncHttpRequest request = data.request;
        com.android.slyce.async.http.body.AsyncHttpRequestBody requestBody = data.request.getBody();

        if (requestBody != null) {
            if (requestBody.length() >= 0) {
                request.getHeaders().set("Content-Length", String.valueOf(requestBody.length()));
                data.response.sink(data.socket);
            } else {
                request.getHeaders().set("Transfer-Encoding", "Chunked");
                data.response.sink(new com.android.slyce.async.http.filter.ChunkedOutputFilter(data.socket));
            }
        }

        String rl = request.getRequestLine().toString();
        String rs = request.getHeaders().toPrefixString(rl);
        request.logv("\n" + rs);

        com.android.slyce.async.Util.writeAll(data.socket, rs.getBytes(), data.sendHeadersCallback);

        com.android.slyce.async.LineEmitter.StringCallback headerCallback = new com.android.slyce.async.LineEmitter.StringCallback() {
            com.android.slyce.async.http.Headers mRawHeaders = new com.android.slyce.async.http.Headers();
            String statusLine;

            @Override
            public void onStringAvailable(String s) {
                try {
                    s = s.trim();
                    if (statusLine == null) {
                        statusLine = s;
                    }
                    else if (!TextUtils.isEmpty(s)) {
                        mRawHeaders.addLine(s);
                    }
                    else {
                        String[] parts = statusLine.split(" ", 3);
                        if (parts.length < 2)
                            throw new Exception(new IOException("Not HTTP"));

                        data.response.headers(mRawHeaders);
                        String protocol = parts[0];
                        data.response.protocol(protocol);
                        data.response.code(Integer.parseInt(parts[1]));
                        data.response.message(parts.length == 3 ? parts[2] : "");
                        data.receiveHeadersCallback.onCompleted(null);

                        // socket may get detached after headers (websocket)
                        com.android.slyce.async.AsyncSocket socket = data.response.socket();
                        if (socket == null)
                            return;
                        com.android.slyce.async.DataEmitter emitter;
                        // HEAD requests must not return any data. They still may
                        // return content length, etc, which will confuse the body decoder
                        if (com.android.slyce.async.http.AsyncHttpHead.METHOD.equalsIgnoreCase(data.request.getMethod())) {
                            emitter = HttpUtil.EndEmitter.create(socket.getServer(), null);
                        }
                        else {
                            emitter = HttpUtil.getBodyDecoder(socket, com.android.slyce.async.http.Protocol.get(protocol), mRawHeaders, false);
                        }
                        data.response.emitter(emitter);
                    }
                }
                catch (Exception ex) {
                    data.receiveHeadersCallback.onCompleted(ex);
                }
            }
        };

        com.android.slyce.async.LineEmitter liner = new com.android.slyce.async.LineEmitter();
        data.socket.setDataCallback(liner);
        liner.setLineCallback(headerCallback);
        return true;
    }

    @Override
    public void onRequestSent(OnRequestSentData data) {
        com.android.slyce.async.http.Protocol p = com.android.slyce.async.http.Protocol.get(data.protocol);
        if (p != null && p != com.android.slyce.async.http.Protocol.HTTP_1_0 && p != com.android.slyce.async.http.Protocol.HTTP_1_1)
            return;

        if (data.response.sink() instanceof com.android.slyce.async.http.filter.ChunkedOutputFilter)
            data.response.sink().end();
    }
}
