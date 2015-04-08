package com.android.slyce.async.http;

import com.android.slyce.async.AsyncSocket;
import com.android.slyce.async.DataEmitter;
import com.android.slyce.async.DataSink;
import com.android.slyce.async.callback.ConnectCallback;
import com.android.slyce.async.future.Cancellable;
import com.android.slyce.async.util.UntypedHashtable;

/**
 * AsyncHttpClientMiddleware is used by AsyncHttpClient to
 * inspect, manipulate, and handle http requests.
 */
public interface AsyncHttpClientMiddleware {
    public interface ResponseHead  {
        public AsyncSocket socket();
        public String protocol();
        public String message();
        public int code();
        public ResponseHead protocol(String protocol);
        public ResponseHead message(String message);
        public ResponseHead code(int code);
        public Headers headers();
        public ResponseHead headers(Headers headers);
        public DataSink sink();
        public ResponseHead sink(DataSink sink);
        public DataEmitter emitter();
        public ResponseHead emitter(DataEmitter emitter);
    }

    public static class OnRequestData {
        public UntypedHashtable state = new UntypedHashtable();
        public AsyncHttpRequest request;
    }

    public static class GetSocketData extends OnRequestData {
        public ConnectCallback connectCallback;
        public Cancellable socketCancellable;
        public String protocol;
    }

    public static class OnExchangeHeaderData extends GetSocketData {
        public com.android.slyce.async.AsyncSocket socket;
        public ResponseHead response;
        public com.android.slyce.async.callback.CompletedCallback sendHeadersCallback;
        public com.android.slyce.async.callback.CompletedCallback receiveHeadersCallback;
    }

    public static class OnRequestSentData extends OnExchangeHeaderData {
    }

    public static class OnHeadersReceivedDataOnRequestSentData extends OnRequestSentData {
    }

    public static class OnBodyDataOnRequestSentData extends OnHeadersReceivedDataOnRequestSentData {
        public com.android.slyce.async.DataEmitter bodyEmitter;
    }

    public static class OnResponseCompleteDataOnRequestSentData extends OnBodyDataOnRequestSentData {
        public Exception exception;
    }

    /**
     * Called immediately upon request execution
     * @param data
     */
    public void onRequest(OnRequestData data);

    /**
     * Called to retrieve the socket that will fulfill this request
     * @param data
     * @return
     */
    public com.android.slyce.async.future.Cancellable getSocket(GetSocketData data);

    /**
     * Called before when the headers are sent and received via the socket.
     * Implementers return true to denote they will manage header exchange.
     * @param data
     * @return
     */
    public boolean exchangeHeaders(OnExchangeHeaderData data);

    /**
     * Called once the headers and any optional request body has
     * been sent
     * @param data
     */
    public void onRequestSent(OnRequestSentData data);

    /**
     * Called once the headers have been received via the socket
     * @param data
     */
    public void onHeadersReceived(OnHeadersReceivedDataOnRequestSentData data);

    /**
     * Called before the body is decoded
     * @param data
     */
    public void onBodyDecoder(OnBodyDataOnRequestSentData data);

    /**
     * Called once the request is complete and response has been received,
     * or if an error occurred
     * @param data
     */
    public void onResponseComplete(OnResponseCompleteDataOnRequestSentData data);
}
