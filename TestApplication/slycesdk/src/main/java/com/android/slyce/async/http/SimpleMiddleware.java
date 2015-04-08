package com.android.slyce.async.http;

public class SimpleMiddleware implements com.android.slyce.async.http.AsyncHttpClientMiddleware {
    @Override
    public void onRequest(OnRequestData data) {
    }

    @Override
    public com.android.slyce.async.future.Cancellable getSocket(GetSocketData data) {
        return null;
    }

    @Override
    public boolean exchangeHeaders(OnExchangeHeaderData data) {
        return false;
    }

    @Override
    public void onRequestSent(OnRequestSentData data) {
    }

    @Override
    public void onHeadersReceived(OnHeadersReceivedDataOnRequestSentData data) {
    }

    @Override
    public void onBodyDecoder(OnBodyDataOnRequestSentData data) {
    }

    @Override
    public void onResponseComplete(OnResponseCompleteDataOnRequestSentData data) {
    }
}
