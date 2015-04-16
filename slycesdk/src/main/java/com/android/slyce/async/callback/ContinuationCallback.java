package com.android.slyce.async.callback;

import com.android.slyce.async.future.Continuation;

public interface ContinuationCallback {
    public void onContinue(Continuation continuation, CompletedCallback next) throws Exception;
}
