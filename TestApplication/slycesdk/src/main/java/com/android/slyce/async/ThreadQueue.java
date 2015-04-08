package com.android.slyce.async;

import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;

public class ThreadQueue extends LinkedList<Runnable> {
    final private static WeakHashMap<Thread, ThreadQueue> mThreadQueues = new WeakHashMap<Thread, ThreadQueue>();

    static ThreadQueue getOrCreateThreadQueue(Thread thread) {
        ThreadQueue queue;
        synchronized (mThreadQueues) {
            queue = mThreadQueues.get(thread);
            if (queue == null) {
                queue = new ThreadQueue();
                mThreadQueues.put(thread, queue);
            }
        }

        return queue;
    }

    static void release(com.android.slyce.async.AsyncSemaphore semaphore) {
        synchronized (mThreadQueues) {
            for (ThreadQueue threadQueue: mThreadQueues.values()) {
                if (threadQueue.waiter == semaphore)
                    threadQueue.queueSemaphore.release();
            }
        }
    }

    com.android.slyce.async.AsyncSemaphore waiter;
    Semaphore queueSemaphore = new Semaphore(0);

    @Override
    public boolean add(Runnable object) {
        synchronized (this) {
            return super.add(object);
        }
    }

    @Override
    public boolean remove(Object object) {
        synchronized (this) {
            return super.remove(object);
        }
    }

    @Override
    public Runnable remove() {
        synchronized (this) {
            if (this.isEmpty())
                return null;
            return super.remove();
        }
    }
}