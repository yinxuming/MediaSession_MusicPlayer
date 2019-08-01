package cn.yinxm.media.ms;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MusicService extends MediaBrowserServiceCompat {
    private AudioManager mAudioManager;
    private ComponentName mMediaButtonReceive;
    boolean isHaveAudioFocus = false;
    private MediaSessionCompat mSession;
    private MediaPlayer mMediaPlayer;
    private PlaybackStateCompat mPlaybackState;

    private static final String TAG = "MusicService";
    public static final String MEDIA_ID_ROOT = "__ROOT__";

    private int postion = -1;
    private List<PlayBean> mPlayBeanList = PlayListHelper.getPlayList();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate-----------");

        mPlaybackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                .setActions(getAvailableActions(PlaybackStateCompat.STATE_NONE))
                .build();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mMediaButtonReceive = new ComponentName(getPackageName(), MediaButtonIntentReceiver.class.getName());
        mSession = new MediaSessionCompat(this, "MusicService");
        mSession.setCallback(mSessionCallback);
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setPlaybackState(mPlaybackState);
        mSession.setActive(true);

        // 设置token后会触发MediaBrowserCompat.ConnectionCallback的回调方法
        // 表示MediaBrowser与MediaBrowserService连接成功
        setSessionToken(mSession.getSessionToken());
//        updateMediaSessionState();

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(PreparedListener);
        mMediaPlayer.setOnCompletionListener(CompletionListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            Log.d(TAG, "action=" + action);
            MediaButtonReceiver.handleIntent(mSession, intent);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mSession != null) {
            mSession.release();
            mSession = null;
        }
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        Log.e(TAG, "onGetRoot-----------");
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull final Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.e(TAG, "onLoadChildren--------");
        // 将信息从当前线程中移除，允许后续调用sendResult方法
        result.detach();

        // 我们模拟获取数据的过程，真实情况应该是异步从网络或本地读取数据
        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = PlayListHelper.transformPlayList(mPlayBeanList);

        // 向Browser发送 播放列表数据
        result.sendResult(mediaItems);
    }

    private MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        return new MediaBrowserCompat.MediaItem(
                metadata.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        );
    }

    private int requestAudioFocus() {
        int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        isHaveAudioFocus = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result;
        if (isHaveAudioFocus) {
            mAudioManager.registerMediaButtonEventReceiver(mMediaButtonReceive);
        }
        Log.d(TAG, "requestAudioFocus " + isHaveAudioFocus);
        return result;
    }

    private void abandAudioFocus() {
        int result = mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
        isHaveAudioFocus = AudioManager.AUDIOFOCUS_REQUEST_GRANTED == result;
    }


    public long getAvailableActions(@PlaybackStateCompat.State int state) {
        long actions = PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                | PlaybackStateCompat.ACTION_REWIND
                | PlaybackStateCompat.ACTION_FAST_FORWARD;
        if (state == PlaybackStateCompat.STATE_PLAYING) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        } else {
            actions |= PlaybackStateCompat.ACTION_PLAY;
        }
        return actions;
    }

    private void handlePlay() {
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PAUSED
                && requestAudioFocus() == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mMediaPlayer.start();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_PLAYING))
                    .build();
            mSession.setPlaybackState(mPlaybackState);
        }
    }

    private void handlePause(boolean isAbandFocus) {
        if (mMediaPlayer == null) {
            return;
        }
        if (mPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            mMediaPlayer.pause();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_PAUSED))
                    .build();
            mSession.setPlaybackState(mPlaybackState);
        }
        if (isAbandFocus) {

        }
    }

    private void handlePlayPosition(int pos) {
        PlayBean playBean = setPlayPosition(pos);
        if (playBean == null) {
            return;
        }
        handlePlayUri(PlayListHelper.rawToUri(this, playBean.mediaId));
    }

    private void handlePlayUri(Uri uri) {
        if (uri == null) {
            return;
        }
        if (requestAudioFocus() != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }
        mMediaPlayer.reset();
        mMediaPlayer.setLooping(true);
        try {
            mMediaPlayer.setDataSource(MusicService.this, uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
        mPlaybackState = new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_CONNECTING, 0, 1.0f)
                .setActions(getAvailableActions(PlaybackStateCompat.STATE_CONNECTING))
                .build();
        mSession.setPlaybackState(mPlaybackState);
        //我们可以保存当前播放音乐的信息，以便客户端刷新UI
        mSession.setMetadata(PlayListHelper.transformPlayBean(getPlayBean()));
    }

    private PlayBean getPlayBean() {
        if (postion >= 0 && postion < mPlayBeanList.size()) {
            return mPlayBeanList.get(postion);
        }
        return null;
    }

    private PlayBean setPlayPosition(int pos) {
        if (pos >= 0 && pos < mPlayBeanList.size()) {
            postion = pos;
            return mPlayBeanList.get(postion);
        }
        return null;
    }

    /**
     * 响应控制器指令的回调
     */
    private MediaSessionCompat.Callback mSessionCallback = new MediaSessionCompat.Callback() {
        /**
         * 响应MediaController.getTransportControls().play
         */
        @Override
        public void onPlay() {
            Log.e(TAG, "onPlay");
            handlePlay();
        }

        /**
         * 响应MediaController.getTransportControls().onPause
         */
        @Override
        public void onPause() {
            Log.e(TAG, "onPause");
            handlePause(true);
        }

        @Override
        public void onSkipToPrevious() {
            Log.e(TAG, "onSkipToPrevious");
            int pos = (postion + mPlayBeanList.size() - 1) % mPlayBeanList.size();
            handlePlayPosition(pos);
        }

        @Override
        public void onSkipToNext() {
            Log.e(TAG, "onSkipToNext");
            int pos = (postion + 1) % mPlayBeanList.size();
            handlePlayPosition(pos);
        }

        /**
         * 响应MediaController.getTransportControls().playFromUri
         *
         * @param uri
         * @param extras
         */
        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            Log.e(TAG, "onPlayFromUri");
            int position = extras.getInt("playPosition");
            setPlayPosition(position);
            handlePlayUri(uri);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.d(TAG, "MediaSessionCallback——》onMediaButtonEvent " + mediaButtonEvent);

            return super.onMediaButtonEvent(mediaButtonEvent);
        }
    };

    /**
     * 监听MediaPlayer.prepare()
     */
    private MediaPlayer.OnPreparedListener PreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            mMediaPlayer.start();
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_PLAYING))
                    .build();
            mSession.setPlaybackState(mPlaybackState);
        }
    };

    /**
     * 监听播放结束的事件
     */
    private MediaPlayer.OnCompletionListener CompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mPlaybackState = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_NONE, 0, 1.0f)
                    .setActions(getAvailableActions(PlaybackStateCompat.STATE_NONE))
                    .build();
            mSession.setPlaybackState(mPlaybackState);
            mMediaPlayer.reset();
        }
    };

    AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "onAudioFocusChange  focusChange=" + focusChange + ", before isHaveAudioFocus=" +
                    isHaveAudioFocus);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    isHaveAudioFocus = false;
                    mSessionCallback.onPause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    isHaveAudioFocus = false;
                    Log.d(TAG, " AUDIOFOCUS_LOSS_TRANSIENT  ");
                    handlePause(false);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // TODO: 2019-07-31   降低音量
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    isHaveAudioFocus = true;
                    mSessionCallback.onPlay();
                    break;
                case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                    break;
                default:
                    break;
            }
        }
    };
}
