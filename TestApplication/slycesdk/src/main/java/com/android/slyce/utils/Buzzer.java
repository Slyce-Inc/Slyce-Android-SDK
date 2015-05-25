package com.android.slyce.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;

/**
 * Created by didiuzan on 5/21/15.
 */
public class Buzzer {

    private static Buzzer mSoundPlayer;

    private SoundPool mSoundPool;

    private Vibrator vibrator;

    // Private C'tor. Prevents instantiation from other classes
    private Buzzer() {

        mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {

            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPool.play(sampleId, 1, 1, 1, 0, 1f);
            }
        });
    }

    private static class BuzzerHolder{
        private static Buzzer INSTANCE = new Buzzer();
    }

    public static Buzzer getInstance(){
        return BuzzerHolder.INSTANCE;
    }

    public void buzz(Context context, int soundRawId, boolean playSound, boolean vibrate) {

        if (playSound){
            mSoundPool.load(context, soundRawId, 1);
        }

        if (vibrate) {

            // Set the pattern for vibration
            long pattern[] = {0, 150};

            // Start the vibration
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            // start vibration with repeated count, use -1 if you don't want to repeat the vibration
            vibrator.vibrate(pattern, -1);

        }
    }

}
