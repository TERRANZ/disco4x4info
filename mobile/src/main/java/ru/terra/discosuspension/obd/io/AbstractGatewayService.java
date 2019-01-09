package ru.terra.discosuspension.obd.io;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import ru.terra.discosuspension.Logger;


public abstract class AbstractGatewayService extends Service {
    private static final String TAG = AbstractGatewayService.class.getName();

    protected Context ctx;
    protected boolean isRunning = false;
    private final IBinder binder = new AbstractGatewayServiceBinder();
    protected boolean isQueueRunning = false;
    protected Long queueCounter = 0L;
    protected BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<ObdCommandJob>();

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

    /**
     * This method will add a job to the queue while setting its ID to the
     * internal queue counter.
     *
     * @param job the job to queue.
     */
    public void queueJob(ObdCommandJob job) {
        queueCounter++;
        Logger.d(TAG, "Adding job[" + queueCounter + "] to queue..");

        job.setId(queueCounter);
        try {
            jobsQueue.put(job);
            Logger.d(TAG, "Job queued successfully.");
        } catch (InterruptedException e) {
            job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            Logger.e(TAG, "Failed to queue job.", e);
        }

        if (!isQueueRunning) {
            // Run the executeQueue in a different thread to lighten the UI thread
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    executeQueue();
                }
            });

            t.start();
        }
    }

    /**
     * Show a notification while this service is running.
     */


    public void setContext(Context c) {
        ctx = c;
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
