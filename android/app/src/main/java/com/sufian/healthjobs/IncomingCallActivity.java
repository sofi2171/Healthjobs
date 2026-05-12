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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class IncomingCallActivity extends Activity {

    private MediaPlayer ringtonePlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ Lock screen ke upar dikhao
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON  |
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON  |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // ✅ Notification action handle karo
        String action = getIntent().getStringExtra("action");

        if ("decline".equals(action)) {
            cancelNotification();
            finish();
            return;
        }

        if ("accept".equals(action)) {
            cancelNotification();
            openChat(
                getIntent().getStringExtra("callerUid"),
                getIntent().getStringExtra("callType")
            );
            return;
        }

        setContentView(R.layout.activity_incoming_call);

        String callerName = getIntent().getStringExtra("callerName");
        String callerUid  = getIntent().getStringExtra("callerUid");
        String callType   = getIntent().getStringExtra("callType");

        TextView nameView = findViewById(R.id.caller_name);
        TextView typeView = findViewById(R.id.call_type);
        Button acceptBtn  = findViewById(R.id.btn_accept);
        Button declineBtn = findViewById(R.id.btn_decline);

        nameView.setText(callerName != null ? callerName : "Unknown");
        typeView.setText("video".equals(callType) ? "Incoming Video Call" : "Incoming Audio Call");

        // ✅ Ringtone + vibration
        startRingtone();

        acceptBtn.setOnClickListener(v -> {
            stopRingtone();
            cancelNotification();
            openChat(callerUid, callType);
        });

        declineBtn.setOnClickListener(v -> {
            stopRingtone();
            cancelNotification();
            finish();
        });
    }

    private void openChat(String callerUid, String callType) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("callerUid", callerUid);
        intent.putExtra("startCall", "true");
        intent.putExtra("callType",  callType);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void startRingtone() {
        try {
            // ✅ Audio focus lo pehle
            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            audioManager.requestAudioFocus(null,
                AudioManager.STREAM_RING,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_RING),
                0
            );

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
            e.printStackTrace();
            // Fallback — Ringtone class use karo
            try {
                Uri fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                android.media.Ringtone ringtone = RingtoneManager.getRingtone(this, fallbackUri);
                if (ringtone != null) ringtone.play();
            } catch (Exception ex) { ex.printStackTrace(); }
                                                                }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
                vibrator = vm.getDefaultVibrator();
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
            e.printStackTrace();
        }
    }

    private void stopRingtone() {
        try {
            if (ringtonePlayer != null) {
                ringtonePlayer.stop();
                ringtonePlayer.release();
                ringtonePlayer = null;
            }
            if (vibrator != null) {
                vibrator.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(999);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
    }

    @Override
    public void onBackPressed() {
        stopRingtone();
        cancelNotification();
        finish();
    }
}
