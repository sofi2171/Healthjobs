package com.sufian.healthjobs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private String pendingCallerUid  = null;
    private String pendingCallType   = null;
    private boolean pendingCall      = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleCallIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleCallIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ Agar pending call hai aur bridge ab ready hai
        if (pendingCall && pendingCallerUid != null) {
            pendingCall = false;
            loadCallUrl(pendingCallerUid, pendingCallType);
        }
    }

    private void handleCallIntent(Intent intent) {
        if (intent == null) return;

        String callerUid = intent.getStringExtra("callerUid");
        String startCall = intent.getStringExtra("startCall");
        String callType  = intent.getStringExtra("callType");

        if (callerUid != null && "true".equals(startCall)) {
            // ✅ Bridge ready check karo - delay ke saath try karo
            pendingCallerUid = callerUid;
            pendingCallType  = callType;
            pendingCall      = true;

            // 1 second wait karo Bridge ke tayyar hone ka
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (getBridge() != null && getBridge().getWebView() != null) {
                        pendingCall = false;
                        loadCallUrl(callerUid, callType);
                    }
                    // agar bridge ready nahi hai to onResume mein handle hoga
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 1000);
        }
    }

    private void loadCallUrl(String callerUid, String callType) {
        try {
            String callTypeParam = (callType != null) ? callType : "audio";
            final String url = "file:///android_asset/public/chat.html"
                + "?uid=" + callerUid
                + "&startCall=true"
                + "&incoming=true"
                + "&callType=" + callTypeParam;

            getBridge().getWebView().post(() -> {
                getBridge().getWebView().loadUrl(url);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
