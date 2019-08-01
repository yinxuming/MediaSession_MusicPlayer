package cn.yinxm.media.ms.client;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Client_MediaBrowser";

    Context mContext;
    PackageManager mPackageManager;
    TextView mTvInfo;
    MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat mController;
    PlayInfo mPlayInfo = new PlayInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        mPackageManager = getPackageManager();

        mTvInfo = findViewById(R.id.tv_info);
    }


    @Override
    protected void onResume() {
        super.onResume();
        connectRemoteService();
    }

    private void connectRemoteService() {
        // TODO: 2019-07-31 车载蓝牙音乐 实际上是通过获取所有的MediaBrowserService结合当前的MediaSession以及音频焦点状态，
        //  来知道该显示哪个多媒体app的播放数据
//        Intent intent = new Intent(MediaBrowserService.SERVICE_INTERFACE);
//        List<ResolveInfo> resInfos = mPackageManager.queryIntentServices(intent, PackageManager.MATCH_ALL);
//        Log.d(TAG, "resInfos=" + resInfos);
//        for (ResolveInfo resolveInfo : resInfos) {
//            Log.d(TAG, "pkg=" + resolveInfo.serviceInfo.packageName + ", service=" + resolveInfo.serviceInfo.name + ", " + resolveInfo.loadLabel(mPackageManager).toString());
//        }
//        if (resInfos.isEmpty()) {
//            return;
//        }

        // 1.待连接的服务
        ComponentName componentName = new ComponentName("cn.yinxm.media.ms", "cn.yinxm.media.ms.MusicService");
//        ComponentName componentName = new ComponentName(resInfos.get(0).serviceInfo.packageName,
//                resInfos.get(0).serviceInfo.name);
        // 2.创建MediaBrowser
        mMediaBrowser = new MediaBrowserCompat(mContext, componentName, mConnectionCallbacks, null);
        // 3.建立连接
        mMediaBrowser.connect();
    }

    private void refreshPlayInfo() {
        mTvInfo.setText(mPlayInfo.debugInfo());
    }

    private void updatePlayState(PlaybackStateCompat state) {
        if (state == null) {
            return;
        }
        mPlayInfo.setState(state);
        refreshPlayInfo();
    }

    private void updatePlayMetadata(MediaMetadataCompat metadata) {
        if (metadata == null) {
            return;
        }
        mPlayInfo.setMetadata(metadata);
        refreshPlayInfo();
    }


    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {

                @Override
                public void onConnected() {
                    Log.d(TAG, "MediaBrowser.onConnected");
                    if (mMediaBrowser.isConnected()) {
                        String mediaId = mMediaBrowser.getRoot();
                        mMediaBrowser.unsubscribe(mediaId);
                        //之前说到订阅的方法还需要一个参数，即设置订阅回调SubscriptionCallback
                        //当Service获取数据后会将数据发送回来，此时会触发SubscriptionCallback.onChildrenLoaded回调
                        mMediaBrowser.subscribe(mediaId, BrowserSubscriptionCallback);
                        try {
                            mController = new MediaControllerCompat(MainActivity.this, mMediaBrowser.getSessionToken());
                            mController.registerCallback(mMediaControllerCallback);
                            if (mController.getMetadata() != null) {
                                updatePlayMetadata(mController.getMetadata());
                                updatePlayState(mController.getPlaybackState());
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onConnectionSuspended() {
                    // 连接中断回调
                    Log.d(TAG, "onConnectionSuspended");
                }

                @Override
                public void onConnectionFailed() {
                    Log.d(TAG, "onConnectionFailed");
                }
            };

    /**
     * 向媒体浏览器服务(MediaBrowserService)发起数据订阅请求的回调接口
     */
    private final MediaBrowserCompat.SubscriptionCallback BrowserSubscriptionCallback =
            new MediaBrowserCompat.SubscriptionCallback() {
                @Override
                public void onChildrenLoaded(@NonNull String parentId,
                                             @NonNull List<MediaBrowserCompat.MediaItem> children) {
                    Log.e(TAG, "onChildrenLoaded------" + children);
                    mPlayInfo.setChildren(children);
                    refreshPlayInfo();
                }
            };


    /**
     * 被动接收蓝牙播放信息、状态改变
     */
    MediaControllerCompat.Callback mMediaControllerCallback =

            new MediaControllerCompat.Callback() {
                @Override
                public void onSessionDestroyed() {
                    // Session销毁
                    Log.d(TAG, "onSessionDestroyed");

                }

                @Override
                public void onRepeatModeChanged(int repeatMode) {
                    // 循环模式发生变化
                    Log.d(TAG, "onRepeatModeChanged");

                }

                @Override
                public void onShuffleModeChanged(int shuffleMode) {
                    // 随机模式发生变化
                    Log.d(TAG, "onShuffleModeChanged");

                }

                @Override
                public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
                    // 当前蓝牙播放列表更新回调
                }

                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    // 数据变化
                    Log.e(TAG, "onMetadataChanged ");
                    updatePlayMetadata(metadata);
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    // 播放状态变化
                    Log.d(TAG, "onPlaybackStateChanged   PlaybackState:" + state.getState());
                    updatePlayState(state);

                }
            };

    static class PlayInfo {
        private MediaMetadataCompat metadata;
        private PlaybackStateCompat state;
        private List<MediaBrowserCompat.MediaItem> children;


        public void setMetadata(MediaMetadataCompat metadata) {
            this.metadata = metadata;
        }

        public void setState(PlaybackStateCompat state) {
            this.state = state;
        }

        public void setChildren(List<MediaBrowserCompat.MediaItem> children) {
            this.children = children;
        }

        public String debugInfo() {
            StringBuilder builder = new StringBuilder();
            if (state != null) {
                builder.append("当前播放状态：\t" + (state.getState() == PlaybackStateCompat.STATE_PLAYING ? "播放中" : "未播放"));
                builder.append("\n\n");
            }
            if (metadata != null) {
                builder.append("当前播放信息：\t" + transform(metadata));
                builder.append("\n\n");
            }
            if (children != null && !children.isEmpty()) {
                builder.append("当前播放列表：\n");
                for (int i = 0; i < children.size(); i++) {
                    MediaBrowserCompat.MediaItem mediaItem = children.get(i);
                    builder.append((i + 1) + " " + mediaItem.getDescription().getTitle() + " - " + mediaItem.getDescription().getSubtitle()).append("\n");
                }

            }
            return builder.toString();
        }

        public static String transform(MediaMetadataCompat data) {
            if (data == null) {
                return null;
            }
            String title = data.getString(MediaMetadata.METADATA_KEY_TITLE);
            String artist = data.getString(MediaMetadata.METADATA_KEY_ARTIST);
            String albumName = data.getString(MediaMetadata.METADATA_KEY_ALBUM);
            long mediaNumber = data.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER);
            long mediaTotalNumber = data.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS);

            return title + " - " + artist;
        }
    }


}
