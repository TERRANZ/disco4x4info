package ru.terra.discosuspension;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

public class NotificationInstance {
    private static NotificationInstance instance = new NotificationInstance();
    private NotificationManager manager;

    private NotificationInstance() {
    }

    public static NotificationInstance getInstance() {
        return instance;
    }

    public void createInfoNotification(Context context, String message) {
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setTicker(message)
                .setContentText(message)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("4x4 Info");

        Notification notification = nb.getNotification();
        manager.notify(0, notification);
    }

}
