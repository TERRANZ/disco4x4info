package ru.terra.discosuspension.obd;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import java.util.Timer;
import java.util.TimerTask;

public abstract class AsyncTaskEx<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
    private TimerTask tt;
    protected Context context;
    protected Exception exception;
    protected ProgressDialog dlg;

    public AsyncTaskEx(Long delay, Context a) {
        this.context = a;
        tt = new TimerTask() {
            @Override
            public void run() {
                AsyncTaskEx.this.cancel(true);
            }
        };
        Timer t = new Timer();
        t.schedule(tt, delay);
    }

    @Override
    protected abstract void onCancelled();

    protected void showDialog(CharSequence title, CharSequence message) {
        dlg = ProgressDialog.show(context, title, message);
    }

    protected void dismissDialog() {
        try {
            if (dlg != null && dlg.isShowing())
                dlg.dismiss();
        } catch (Exception e) {
        }
    }

}
