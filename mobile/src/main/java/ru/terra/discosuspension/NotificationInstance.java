package ru.terra.discosuspension;

import android.app.Notification;
import android.content.Context;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

public class NotificationInstance {
    private static NotificationInstance instance = new NotificationInstance();
    private int counter;

    private NotificationInstance() {
    }

    public static NotificationInstance getInstance() {
        return instance;
    }

    public void createInfoNotification(Context context, String message, boolean finalMessage) {
        Notification notify = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("4x4 Info")
                .setContentText(message)
                .setGroup("4x4info")
//                .setGroupSummary(finalMessage)
                .build();

        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(context);
        notificationManager.notify(counter++, notify);
    }

}
