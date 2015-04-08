package com.android.slyce.async.future;

public class SimpleCancellable implements com.android.slyce.async.future.DependentCancellable {
    boolean complete;
    @Override
    public boolean isDone() {
        return complete;
    }

    protected void cancelCleanup() {
    }

    protected void cleanup() {
    }

    protected void completeCleanup() {
    }

    public boolean setComplete() {
        synchronized (this) {
            if (cancelled)
                return false;
            if (complete) {
                // don't allow a Cancellable to complete twice...
                assert false;
                return true;
            }
            complete = true;
            parent = null;
        }
        completeCleanup();
        cleanup();
        return true;
    }

    @Override
    public boolean cancel() {
        com.android.slyce.async.future.Cancellable parent;
        synchronized (this) {
            if (complete)
                return false;
            if (cancelled)
                return true;
            cancelled = true;
            parent = this.parent;
            // null out the parent to allow garbage collection
            this.parent = null;
        }
        if (parent != null)
            parent.cancel();
        cancelCleanup();
        cleanup();
        return true;
    }
    boolean cancelled;

    private com.android.slyce.async.future.Cancellable parent;
    @Override
    public SimpleCancellable setParent(com.android.slyce.async.future.Cancellable parent) {
        synchronized (this) {
            if (!isDone())
                this.parent = parent;
        }
        return this;
    }

    @Override
    public boolean isCancelled() {
        synchronized (this) {
            return cancelled || (parent != null && parent.isCancelled());
        }
    }

    public static final com.android.slyce.async.future.Cancellable COMPLETED = new SimpleCancellable() {
        {
            setComplete();
        }
    };

    public com.android.slyce.async.future.Cancellable reset() {
        cancel();
        complete = false;
        cancelled = false;
        return this;
    }
}
