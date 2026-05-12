
package com.sufian.healthjobs;

import android.app.KeyguardManager;
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
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CALL_CHANNEL_ID = "incoming_call_channel";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "FCM received: " + remoteMessage.getData().toString());

        Map<String, String> data = remoteMessage.getData();

        // ✅ Cancel call handle karo
        if ("cancel_call".equals(data.get("action"))) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(999);
            return;
        }

        // ✅ Incoming call handle karo
        String isCall = data.get("isCall");
        if ("true".equals(isCall)) {
            String callerUid  = data.containsKey("callerUid")  ? data.get("callerUid")  : "";
            String callerName = data.containsKey("callerName") ? data.get("callerName") : "Health Jobs User";
            String callType   = data.containsKey("callType")   ? data.get("callType")   : "audio";

            Log.d(TAG, "Incoming call from: " + callerName + " uid: " + callerUid);

            // ✅ WakeLock — screen jagao
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE,
                    "healthjobs:callwake"
                );
                wl.acquire(30000);
            }

            // ✅ Notification channel banao
            createCallChannel();

            // ✅ Full screen notification dikhao
            showFullScreenNotification(callerUid, callerName, callType);

            // ✅ IncomingCallActivity directly launch karo
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
        }
    }

    private void createCallChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

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
            channel.setBypassDnd(true);
            nm.createNotificationChannel(channel);
        }
    }

    private void showFullScreenNotification(String callerUid, String callerName, String callType) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Full screen intent
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("callerUid",  callerUid);
        fullScreenIntent.putExtra("callerName", callerName);
        fullScreenIntent.putExtra("callType",   callType);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreenPI = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Accept intent
        Intent acceptIntent = new Intent(this, IncomingCallActivity.class);
        acceptIntent.putExtra("callerUid",  callerUid);
        acceptIntent.putExtra("callerName", callerName);
        acceptIntent.putExtra("callType",   callType);
        acceptIntent.putExtra("action",     "accept");
        acceptIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent acceptPI = PendingIntent.getActivity(
            this, 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Decline intent
        Intent declineIntent = new Intent(this, IncomingCallActivity.class);
        declineIntent.putExtra("action", "decline");
        declineIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent declinePI = PendingIntent.getActivity(
            this, 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CALL_CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_MAX);
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(callerName)
            .setContentText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call")
            .setCategory(Notification.CATEGORY_CALL)
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(android.R.drawable.ic_menu_call, "Accept",  acceptPI)
            .addAction(android.R.drawable.ic_delete,    "Decline", declinePI)
            .setOngoing(true)
            .setAutoCancel(false);

        nm.notify(999, builder.build());
    }
}
