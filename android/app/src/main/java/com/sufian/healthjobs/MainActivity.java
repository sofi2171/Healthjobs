package com.sufian.healthjobs;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

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

    private void handleCallIntent(Intent intent) {
        if (intent == null) return;

        String callerUid = intent.getStringExtra("callerUid");
        String startCall = intent.getStringExtra("startCall");

        if (callerUid != null && "true".equals(startCall)) {
            final String url = "file:///android_asset/public/chat.html?uid="
                + callerUid
                + "&startCall=true&incoming=true";

            // ✅ WebView ready hone ke baad URL load karo
            getBridge().getWebView().post(() ->
                getBridge().getWebView().loadUrl(url)
            );
        }
    }
}
