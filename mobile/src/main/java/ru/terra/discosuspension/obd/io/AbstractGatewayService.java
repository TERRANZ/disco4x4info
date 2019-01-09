package ru.terra.discosuspension.obd.io;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.lang.ref.WeakReference;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public abstract class AbstractGatewayService extends Service {
    private static final String TAG = AbstractGatewayService.class.getName();

    protected WeakReference<Context> ctx;
    protected boolean isRunning = false;
    private final IBinder binder = new AbstractGatewayServiceBinder();
    protected boolean isQueueRunning = false;
    protected Long queueCounter = 0L;
    protected final BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<ObdCommandJob>();

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public boolean isRunning() {
        return isRunning;
    }


    public class AbstractGatewayServiceBinder extends Binder {
        public AbstractGatewayService getService() {
            return AbstractGatewayService.this;
        }
    }

    public void queueJob(ObdCommandJob job) {
        queueCounter++;
        job.setId(queueCounter);
        try {
            jobsQueue.put(job);
        } catch (InterruptedException e) {
            job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
        }

        if (!isQueueRunning) {
            new Thread(this::executeQueue).start();
        }
    }

    public void setContext(Context c) {
        ctx = new WeakReference<>(c);
    }

    abstract protected void executeQueue();

    abstract public boolean startService();

    abstract public void stopService();

    public int getCurrentQueueSize() {
        synchronized (jobsQueue) {
            return jobsQueue.size();
        }
    }
}
