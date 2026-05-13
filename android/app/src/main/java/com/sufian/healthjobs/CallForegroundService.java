package com.sufian.healthjobs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class CallForegroundService extends Service {

    private static final String TAG = "CallService";
    private static final String CHANNEL_ID = "call_service_channel";
    private static final int NOTIF_ID = 888;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        String callerUid  = intent.getStringExtra("callerUid");
        String callerName = intent.getStringExtra("callerName");
        String callType   = intent.getStringExtra("callType");

        Log.d(TAG, "Service started: " + callerName);

        createChannel();

        // ✅ Foreground notification
        startForeground(NOTIF_ID, buildNotification(callerUid, callerName, callType));

        // ✅ IncomingCallActivity launch karo
        Intent callIntent = new Intent(this, IncomingCallActivity.class);
        callIntent.putExtra("callerUid",  callerUid);
        callIntent.putExtra("callerName", callerName);
        callIntent.putExtra("callType",   callType);
        callIntent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_CLEAR_TOP |
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(callIntent);

        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Call Service",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String callerUid, String callerName, String callType) {
        // ✅ Full screen intent
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("callerUid",  callerUid);
        fullScreenIntent.putExtra("callerName", callerName);
        fullScreenIntent.putExtra("callType",   callType);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPI = PendingIntent.getActivity(
            this, 10, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ✅ Accept intent
        Intent acceptIntent = new Intent(this, IncomingCallActivity.class);
        acceptIntent.putExtra("callerUid",  callerUid);
        acceptIntent.putExtra("callerName", callerName);
        acceptIntent.putExtra("callType",   callType);
        acceptIntent.putExtra("action",     "accept");
        acceptIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent acceptPI = PendingIntent.getActivity(
            this, 11, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ✅ Decline intent
        Intent declineIntent = new Intent(this, IncomingCallActivity.class);
        declineIntent.putExtra("action", "decline");
        declineIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent declinePI = PendingIntent.getActivity(
            this, 12, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName != null ? callerName : "Incoming Call")
            .setContentText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(android.R.drawable.ic_menu_call, "Accept",  acceptPI)
            .addAction(android.R.drawable.ic_delete,    "Decline", declinePI)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
