package ru.terra.discosuspension;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

@ReportsCrashes(formUri = "http://xn--80aafhfrpg0adapheyc1nya.xn--p1ai:18181/jbrss/errors/do.error.report/discoapp",
        httpMethod = HttpSender.Method.POST, mode = ReportingInteractionMode.SILENT, resToastText = R.string.error_crash_toast)
public class DiscoApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
