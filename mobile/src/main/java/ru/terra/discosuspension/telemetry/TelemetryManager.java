package ru.terra.discosuspension.telemetry;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.util.Date;

import ru.terra.discosuspension.activity.components.ObdState;

public class TelemetryManager {
    private final Context context;

    public TelemetryManager(Context context) {
        this.context = context;
    }

    public void appendTelemetry(final ObdState state, final Date startTime) {
        File dir = new File(Environment.getExternalStorageDirectory(), "disco4x4");
        if (!dir.exists()) {
            dir.mkdir();
        }

        try {
            File gpxfile = new File(dir, DateFormat.getDateTimeInstance().format(startTime));
            FileWriter writer = new FileWriter(gpxfile, true);
            writer.append(state.toCSV());
            writer.append("\n");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
