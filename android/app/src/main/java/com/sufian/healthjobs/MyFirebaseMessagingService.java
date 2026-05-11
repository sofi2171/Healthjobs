package com.sufian.healthjobs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CALL_CHANNEL_ID = "incoming_call_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().containsKey("isCall")
                && "true".equals(remoteMessage.getData().get("isCall"))) {

            String callerUid  = remoteMessage.getData().get("callerUid");
            String callerName = remoteMessage.getData().containsKey("callerName")
                    ? remoteMessage.getData().get("callerName") : "Health Jobs User";
            String callType   = remoteMessage.getData().containsKey("callType")
                    ? remoteMessage.getData().get("callType") : "audio";

            // ✅ Screen wake karo
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE,
                "healthjobs:callwake"
            );
            wl.acquire(30000);

            // ✅ IncomingCallActivity seedha launch karo
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

            // ✅ Notification bhi dikhao
            showCallNotification(callerUid, callerName, callType);
        }
    }

    private void showCallNotification(String callerUid, String callerName, String callType) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes audioAttr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

            NotificationChannel channel = new NotificationChannel(
                CALL_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setSound(ringtoneUri, audioAttr);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            nm.createNotificationChannel(channel);
        }

        // Accept PendingIntent
        Intent acceptIntent = new Intent(this, IncomingCallActivity.class);
        acceptIntent.putExtra("callerUid",  callerUid);
        acceptIntent.putExtra("callerName", callerName);
        acceptIntent.putExtra("callType",   callType);
        acceptIntent.putExtra("action",     "accept");
        acceptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent acceptPI = PendingIntent.getActivity(this, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Decline PendingIntent
        Intent declineIntent = new Intent(this, IncomingCallActivity.class);
        declineIntent.putExtra("action", "decline");
        declineIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent declinePI = PendingIntent.getActivity(this, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Full screen PendingIntent
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

        builder.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call")
            .setPriority(Notification.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(android.R.drawable.ic_menu_call, "Accept",  acceptPI)
            .addAction(android.R.drawable.ic_delete,    "Decline", declinePI)
            .setAutoCancel(true)
            .setOngoing(true);

        nm.notify(999, builder.build());
    }
              }
