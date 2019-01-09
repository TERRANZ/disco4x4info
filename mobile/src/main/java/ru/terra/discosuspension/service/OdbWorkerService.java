package ru.terra.discosuspension.service;

import android.app.IntentService;
import android.content.Intent;

public class OdbWorkerService extends IntentService {
    public OdbWorkerService() {
        super("Obd Worker Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }
}
