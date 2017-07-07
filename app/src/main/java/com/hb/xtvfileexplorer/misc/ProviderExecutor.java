package com.hb.xtvfileexplorer.misc;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class ProviderExecutor extends Thread implements Executor {

    private static final ArrayMap<String, ProviderExecutor> sExecutors = new ArrayMap<>();

    public static ProviderExecutor forAuthority(String authority) {
        synchronized (sExecutors) {
            ProviderExecutor executor = sExecutors.get(authority);
            if (executor == null) {
                executor = new ProviderExecutor();
                executor.setName("ProviderExecutor: " + authority);
                executor.start();
                sExecutors.put(authority, executor);
            }
            return executor;
        }
    }

    interface PreemptAble {
        void preempt();
    }

    private final LinkedBlockingQueue<Runnable> mQueue = new LinkedBlockingQueue<>();

    private final ArrayList<WeakReference<PreemptAble>> mPreemptAble = new ArrayList<>();

    @SuppressWarnings("unused")
	private void preempt() {
        synchronized (mPreemptAble) {
            int count = 0;
            for (WeakReference<PreemptAble> ref : mPreemptAble) {
                final PreemptAble p = ref.get();
                if (p != null) {
                    count++;
                    p.preempt();
                }
            }
            mPreemptAble.clear();
        }
    }

    /**
     * Execute the given task. If given task is not {@link PreemptAble}, it will
     * preempt all outstanding preemptable tasks.
     */
    public <P> void execute(AsyncTask<P, ?, ?> task, P... params) {
        if (task instanceof PreemptAble) {
            synchronized (mPreemptAble) {
                mPreemptAble.add(new WeakReference<>((PreemptAble) task));
            }
            task.executeOnExecutor(mNonPreemptingExecutor, params);
        } else {
            task.executeOnExecutor(this, params);
        }
    }

    private Executor mNonPreemptingExecutor = new Executor() {
        @Override
        public void execute(@NonNull Runnable command) {
            Preconditions.checkNotNull(command);
            mQueue.add(command);
        }
    };

    @Override
    public void execute(@NonNull Runnable command) {
        preempt();
        Preconditions.checkNotNull(command);
        mQueue.add(command);
    }

    @Override
    public void run() {
        try {
            final Runnable command = mQueue.take();
            command.run();
        } catch (InterruptedException e) {
            // That was weird; let's go look for more tasks.
        }
    }
}
