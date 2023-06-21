package me.superskidder.watchgpt.speaking;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.URLEncoder;

public class BaiduTranslator {
    private static final String TAG = BaiduTranslator.class.getSimpleName();

    private MediaPlayer mediaPlayer;
    private Handler handler;

    public BaiduTranslator() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());

        handler = new Handler();
    }

    public void speak(final String text, final String language) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String encodedText = URLEncoder.encode(text, "UTF-8");
                    String url = "https://fanyi.baidu.com/gettts?lan=" + language + "&text=" + encodedText + "&spd=5&source=web";

                    mediaPlayer.reset();
                    mediaPlayer.setDataSource(url);
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mediaPlayer.start();
                        }
                    });


                } catch (IOException e) {
                    Log.e(TAG, "Failed to play translation audio.", e);
                }
            }
        }).start();
    }

    public void stopSpeaking() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.release();
    }
}
