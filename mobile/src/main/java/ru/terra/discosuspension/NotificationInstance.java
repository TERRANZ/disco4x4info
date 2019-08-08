package ru.terra.discosuspension;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import java.lang.ref.WeakReference;

import static android.app.NotificationManager.IMPORTANCE_LOW;

public class NotificationInstance {
    private static NotificationInstance instance = new NotificationInstance();
    private int counter;
    private WeakReference<Context> contextWeakReference;

    private NotificationInstance() {
    }

    void initContext(final Context context) {
        contextWeakReference = new WeakReference<>(context);
    }

    public static NotificationInstance getInstance() {
        return instance;
    }

    public void createInfoNotification(final String message, final boolean finalMessage) {
        final String CHANNEL_ID = "my_channel_01";// The id of the channel.
        final NotificationManager mNotificationManager =
                (NotificationManager) contextWeakReference.get().getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "4x4Info", IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(mChannel);
        }

        final Notification notify = new NotificationCompat.Builder(contextWeakReference.get())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("4x4 Info")
                .setContentText(message)
                .setGroup("4x4info")
                .setGroupSummary(finalMessage)
                .setChannelId(CHANNEL_ID)
                .build();

        mNotificationManager.notify(counter++, notify);
    }

}
