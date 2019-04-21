package ru.terra.discosuspension.activity.components;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public abstract class AsyncTaskEx<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    private TimerTask timerTask;
    protected WeakReference<Context> context;
    protected Exception exception;
    ProgressDialog progressDialog;

    AsyncTaskEx(Long delay, Context context) {
        this.context = new WeakReference<>(context);
        timerTask = new TimerTask() {
            @Override
            public void run() {
                AsyncTaskEx.this.cancel(true);
            }
        };
        Timer t = new Timer();
        t.schedule(timerTask, delay);
    }

    @Override
    protected abstract void onCancelled();

    void showDialog(final CharSequence title, final CharSequence message) {
        progressDialog = ProgressDialog.show(context.get(), title, message);
    }

    void dismissDialog() {
        try {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
        } catch (Exception e) {
        }
    }

}