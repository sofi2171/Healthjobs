package com.sufian.healthjobs;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class IncomingCallActivity extends Activity {

    private static final String TAG = "IncomingCall";
    private MediaPlayer ringtonePlayer;
    private Vibrator vibrator;

    // ✅ Static flag — call active hai ya nahi
    public static boolean isCallActive = false;
    public static String activeCallerUid = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Lock screen ke upar dikhao
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED   |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON     |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON     |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD   |
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        String action    = getIntent().getStringExtra("action");
        String callerUid = getIntent().getStringExtra("callerUid");
        String callType  = getIntent().getStringExtra("callType");
        String callerName = getIntent().getStringExtra("callerName");

        // ✅ Agar call already cancel ho gayi to band karo
        if (!isCallActive && "cancel".equals(action)) {
            cancelNotification();
            finish();
            return;
        }

        // ✅ Decline action
        if ("decline".equals(action)) {
            isCallActive = false;
            activeCallerUid = null;
            cancelNotification();
            finish();
            return;
        }

        // ✅ Accept action
        if ("accept".equals(action)) {
            isCallActive = false;
            activeCallerUid = null;
            cancelNotification();
            openChat(callerUid, callType);
            return;
        }

        // ✅ Agar same caller ki call already handle ho gayi to ignore karo
        if (!isCallActive && activeCallerUid != null && activeCallerUid.equals(callerUid)) {
            finish();
            return;
        }

        // ✅ Call active mark karo
        isCallActive = true;
        activeCallerUid = callerUid;

        setContentView(R.layout.activity_incoming_call);

        TextView nameView = findViewById(R.id.caller_name);
        TextView typeView = findViewById(R.id.call_type);
        Button acceptBtn  = findViewById(R.id.btn_accept);
        Button declineBtn = findViewById(R.id.btn_decline);

        if (nameView != null) nameView.setText(callerName != null ? callerName : "Unknown");
        if (typeView != null) typeView.setText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call");

        startRingtone();

        if (acceptBtn != null) {
            acceptBtn.setOnClickListener(v -> {
                isCallActive = false;
                activeCallerUid = null;
                stopRingtone();
                cancelNotification();
                openChat(callerUid, callType);
            });
        }

        if (declineBtn != null) {
            declineBtn.setOnClickListener(v -> {
                isCallActive = false;
                activeCallerUid = null;
                stopRingtone();
                cancelNotification();
                // ✅ Back stack clear karo
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    // ✅ Cancel signal receive hone par bhi band ho
    public static void cancelIncomingCall() {
        isCallActive = false;
        activeCallerUid = null;
    }

    private void openChat(String callerUid, String callType) {
        if (callerUid == null || callerUid.isEmpty()) {
            finish();
            return;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("callerUid", callerUid);
        intent.putExtra("startCall", "true");
        intent.putExtra("callType",  callType != null ? callType : "audio");
        // ✅ Back stack poora clear karo
        intent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK |
            Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }

    private void startRingtone() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_RING,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                audioManager.setStreamVolume(
                    AudioManager.STREAM_RING,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                    0);
            }
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtonePlayer = new MediaPlayer();
            ringtonePlayer.setDataSource(this, ringtoneUri);
            ringtonePlayer.setAudioStreamType(AudioManager.STREAM_RING);
            ringtonePlayer.setLooping(true);
            ringtonePlayer.prepare();
            ringtonePlayer.start();
        } catch (Exception e) {
            Log.e(TAG, "Ringtone error: " + e.getMessage());
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                if (vm != null) vibrator = vm.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }
            if (vibrator != null) {
                long[] pattern = {0, 1000, 500, 1000, 500};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Vibration error: " + e.getMessage());
        }
    }

    private void stopRingtone() {
        try {
            if (ringtonePlayer != null) {
                if (ringtonePlayer.isPlaying()) ringtonePlayer.stop();
                ringtonePlayer.release();
                ringtonePlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop ringtone: " + e.getMessage());
        }
        try {
            if (vibrator != null) {
                vibrator.cancel();
                vibrator = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Stop vibration: " + e.getMessage());
        }
    }

    private void cancelNotification() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(999);
        } catch (Exception e) {
            Log.e(TAG, "Cancel notif: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
    }

    @Override
    public void onBackPressed() {
        // ✅ Back press = decline
        isCallActive = false;
        activeCallerUid = null;
        stopRingtone();
        cancelNotification();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
