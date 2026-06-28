package com.sufian.healthjobs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginHandle;
import ee.forgr.capacitor.social.login.GoogleProvider;
import ee.forgr.capacitor.social.login.ModifiedMainActivityForSocialLoginPlugin;
import ee.forgr.capacitor.social.login.SocialLoginPlugin;

// ⚠️ "implements ModifiedMainActivityForSocialLoginPlugin" Google Login ke liye zaroori hai
public class MainActivity extends BridgeActivity implements ModifiedMainActivityForSocialLoginPlugin {

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
    public void onResume() {
        super.onResume();
        // ✅ Agar pending call hai aur bridge ab ready hai
        if (pendingCall && pendingCallerUid != null) {
            pendingCall = false;
            loadCallUrl(pendingCallerUid, pendingCallType);
        }
    }

    // ✅ Google Sign-In (@capgo/capacitor-social-login) ka result yahan se native side handle hota hai
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode >= GoogleProvider.REQUEST_AUTHORIZE_GOOGLE_MIN
                && requestCode < GoogleProvider.REQUEST_AUTHORIZE_GOOGLE_MAX) {
            PluginHandle pluginHandle = getBridge().getPlugin("SocialLogin");
            if (pluginHandle == null) {
                Log.i("Google Activity Result", "SocialLogin login handle is null");
                return;
            }
            Plugin plugin = pluginHandle.getInstance();
            if (plugin instanceof SocialLoginPlugin) {
                ((SocialLoginPlugin) plugin).handleGoogleLoginIntent(requestCode, data);
            }
        }
    }

    // ✅ Yeh interface ka required acknowledgment method hai — sirf yeh confirm karta hai
    // ke onActivityResult() upar manually override kiya gaya hai. Body khaali hi rehta hai.
    @Override
    public void IHaveModifiedTheMainActivityForTheUseWithSocialLoginPlugin() {
        // Intentionally empty — required by ModifiedMainActivityForSocialLoginPlugin interface
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
            final String url = "https://localhost/chat.html"
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
