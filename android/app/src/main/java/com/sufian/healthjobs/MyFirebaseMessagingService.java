package com.sufian.healthjobs;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

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

            // ✅ Foreground Service start karo - background mein bhi kaam karega
            Intent serviceIntent = new Intent(this, CallForegroundService.class);
            serviceIntent.setAction(CallForegroundService.ACTION_START);
            serviceIntent.putExtra("callerUid",  callerUid);
            serviceIntent.putExtra("callerName", callerName);
            serviceIntent.putExtra("callType",   callType);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }
}
