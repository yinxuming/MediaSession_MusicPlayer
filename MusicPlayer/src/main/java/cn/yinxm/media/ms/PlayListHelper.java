package cn.yinxm.media.ms;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *
 * @author yinxuming
 * @date 2019-07-31
 */
public class PlayListHelper {

    public static Uri rawToUri(Context context, int id) {
        String uriStr = "android.resource://" + context.getPackageName() + "/" + id;
        return Uri.parse(uriStr);
    }

    public static List<PlayBean> getPlayList() {
        List<PlayBean> list = new ArrayList<>();

        PlayBean playBean = new PlayBean();
        playBean.mediaId = R.raw.inspire;
        playBean.tilte = "Inspire.mp3";
        playBean.artist = "test";
        list.add(playBean);

        PlayBean playBean2 = new PlayBean();
        playBean2.mediaId = R.raw.gydgs;
        playBean2.tilte = "光阴的故事";
        playBean2.artist = "罗大佑";
        list.add(playBean2);

        return list;
    }

    public static ArrayList<MediaBrowserCompat.MediaItem> transformPlayList(List<PlayBean> playBeanList) {
        //我们模拟获取数据的过程，真实情况应该是异步从网络或本地读取数据
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "" + R.raw.inspire)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Inspire.mp3")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "")
                .build();
        MediaMetadataCompat metadata2 = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "" + R.raw.gydgs)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "光阴的故事")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "罗大佑")
                .build();
        ArrayList<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        mediaItems.add(createMediaItem(metadata));
        mediaItems.add(createMediaItem(metadata2));
        return mediaItems;

    }

    public static MediaMetadataCompat transformPlayBean(PlayBean bean) {
        MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "" + bean.mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, bean.tilte)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, bean.artist)
                .build();

        return metadata;
    }


    private static MediaBrowserCompat.MediaItem createMediaItem(MediaMetadataCompat metadata) {
        return new MediaBrowserCompat.MediaItem(
                metadata.getDescription(),
                MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        );
    }
}
