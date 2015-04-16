package com.android.slyce.async.future;

public interface DependentCancellable extends Cancellable {
    public DependentCancellable setParent(Cancellable parent);
}
