package cn.yinxm.media.ms;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;


/**
 * api 21 一下使用此接收器，高版本系统使用MediaSession
 */
public class MediaButtonIntentReceiver extends BroadcastReceiver {
    private static final String TAG = "MediaButtonIntentReceiver";

    @SuppressLint("LongLogTag")
    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Log.d(TAG, "MediaButton onReceive action:" + action);

        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {


            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            int keyCode = keyEvent.getKeyCode();
            int keyAction = keyEvent.getAction();
            long eventTime = keyEvent.getEventTime();

            Log.i(TAG, "keyCode:" + keyCode + ",keyAction:" + keyAction + ",eventTime:" + eventTime);

            ComponentName componentName = new ComponentName(context, MusicService.class);
            intent.setComponent(componentName);
            context.startService(intent);
//
//            if (KeyEvent.ACTION_DOWN == keyAction) {
//                switch (keyCode) {
//                    case KeyEvent.KEYCODE_MEDIA_PLAY:
//                        PlayControlManager.getInstance().play();
//                        break;
//                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
//                        PlayControlManager.getInstance().pause();
//                        break;
//                    case KeyEvent.KEYCODE_HEADSETHOOK:
//                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
//                        if (PlayControlManager.getInstance().isPlaying()) {
//                            PlayControlManager.getInstance().pause();
//                        } else {
//                            PlayControlManager.getInstance().play();
//                        }
//                        break;
//                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
//                        PlayControlManager.getInstance().playPrevious();
//                        break;
//                    case KeyEvent.KEYCODE_MEDIA_NEXT:
//                        PlayControlManager.getInstance().playNext();
//                        break;
//                    case KeyEvent.KEYCODE_MEDIA_STOP:
//                        PlayControlManager.getInstance().stop();
//                        break;
//                    default:
//                        break;
//                }
//            }
        }
    }
}
