package com.sufian.healthjobs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class CallForegroundService extends Service {

    private static final String CALL_CHANNEL_ID = "incoming_call_channel";
    public static final String ACTION_START = "ACTION_START_CALL";
    public static final String ACTION_STOP  = "ACTION_STOP_CALL";

    private PowerManager.WakeLock wakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopForeground(true);
            stopSelf();
            releaseWakeLock();
            return START_NOT_STICKY;
        }

        if (ACTION_START.equals(action)) {
            String callerUid  = intent.getStringExtra("callerUid");
            String callerName = intent.getStringExtra("callerName");
            String callType   = intent.getStringExtra("callType");

            // ✅ WakeLock - screen on karo
            acquireWakeLock();

            // ✅ Foreground notification ke saath start karo
            startForeground(998, buildNotification(callerUid, callerName, callType));

            // ✅ Ab IncomingCallActivity launch karo
            Intent callIntent = new Intent(this, IncomingCallActivity.class);
            callIntent.putExtra("callerUid",  callerUid);
            callIntent.putExtra("callerName", callerName);
            callIntent.putExtra("callType",   callType);
            callIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            );
            startActivity(callIntent);
        }

        return START_NOT_STICKY;
    }

    private Notification buildNotification(String callerUid, String callerName, String callType) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes audioAttr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            NotificationChannel channel = new NotificationChannel(
                CALL_CHANNEL_ID, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH);
            channel.setSound(ringtoneUri, audioAttr);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            nm.createNotificationChannel(channel);
        }

        // Accept intent
        Intent acceptIntent = new Intent(this, IncomingCallActivity.class);
        acceptIntent.putExtra("callerUid",  callerUid);
        acceptIntent.putExtra("callerName", callerName);
        acceptIntent.putExtra("callType",   callType);
        acceptIntent.putExtra("action",     "accept");
        acceptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent acceptPI = PendingIntent.getActivity(this, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Decline intent
        Intent declineIntent = new Intent(this, IncomingCallActivity.class);
        declineIntent.putExtra("action", "decline");
        declineIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent declinePI = PendingIntent.getActivity(this, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Full screen intent
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("callerUid",  callerUid);
        fullScreenIntent.putExtra("callerName", callerName);
        fullScreenIntent.putExtra("callType",   callType);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreenPI = PendingIntent.getActivity(this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CALL_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName != null ? callerName : "Incoming Call")
            .setContentText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call")
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(android.R.drawable.ic_menu_call, "Accept",  acceptPI)
            .addAction(android.R.drawable.ic_delete,    "Decline", declinePI)
            .setAutoCancel(false)
            .setOngoing(true)
            .build();
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK |
            PowerManager.ACQUIRE_CAUSES_WAKEUP |
            PowerManager.ON_AFTER_RELEASE,
            "healthjobs:callwake"
        );
        wakeLock.acquire(60000); // 60 seconds
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }
}
