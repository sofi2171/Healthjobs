package com.sufian.healthjobs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class CallForegroundService extends Service {

    private static final String CHANNEL_ID = "call_foreground_channel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }

        String callerUid  = intent.getStringExtra("callerUid");
        String callerName = intent.getStringExtra("callerName");
        String callType   = intent.getStringExtra("callType");

        createChannel();

        // ✅ Foreground notification — service zinda rahe
        Intent callIntent = new Intent(this, IncomingCallActivity.class);
        callIntent.putExtra("callerUid",  callerUid);
        callIntent.putExtra("callerName", callerName);
        callIntent.putExtra("callType",   callType);
        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
            this, 0, callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(callerName != null ? callerName : "Incoming Call")
                .setContentText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call")
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
        } else {
            notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(callerName != null ? callerName : "Incoming Call")
                .setContentText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call")
                .setContentIntent(pi)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .build();
        }

        startForeground(998, notification);

        // ✅ IncomingCallActivity launch karo
        Intent activityIntent = new Intent(this, IncomingCallActivity.class);
        activityIntent.putExtra("callerUid",  callerUid);
        activityIntent.putExtra("callerName", callerName);
        activityIntent.putExtra("callType",   callType);
        activityIntent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_CLEAR_TOP |
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(activityIntent);

        stopSelf();
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Call Service",
                NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
