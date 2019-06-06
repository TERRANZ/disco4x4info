package ru.terra.discosuspension.obd.io;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;


public abstract class AbstractGatewayService extends Service {
    protected boolean isRunning = false;
    private final IBinder binder = new AbstractGatewayServiceBinder();
    protected boolean isQueueRunning = false;
    protected Long queueCounter = 0L;
    protected final BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<ObdCommandJob>();
    protected StateUpdater stateUpdater;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setStateUpdater(StateUpdater stateUpdater) {
        this.stateUpdater = stateUpdater;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public class AbstractGatewayServiceBinder extends Binder {
        public AbstractGatewayService getService() {
            return AbstractGatewayService.this;
        }
    }

    public void queueCmd(final ObdProtocolCommand cmd) {
        queueCounter++;
        final ObdCommandJob job = new ObdCommandJob(cmd);
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

    abstract protected void executeQueue();

    abstract public boolean startService();

    abstract public void stopService();

    public int getCurrentQueueSize() {
        synchronized (jobsQueue) {
            return jobsQueue.size();
        }
    }
}
