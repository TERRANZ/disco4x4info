package ru.terra.discosuspension.telemetry;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

public class TelemetryService extends IntentService {
    public TelemetryService() {
        super("TelemetryService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
