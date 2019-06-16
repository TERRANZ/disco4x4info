package ru.terra.discosuspension.obd.io;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import pt.lighthouselabs.obd.commands.protocol.ObdProtocolCommand;

public abstract class AbstractGatewayService extends Service {

    protected final BlockingQueue<ObdProtocolCommand> jobsQueue = new LinkedBlockingQueue<>();
    private final IBinder binder = new AbstractGatewayServiceBinder();

    protected boolean isRunning = false;
    protected boolean isQueueRunning = false;
    protected StateUpdater stateUpdater;

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setStateUpdater(final StateUpdater stateUpdater) {
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
        try {
            jobsQueue.put(cmd);
        } catch (final InterruptedException ignored) {
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
