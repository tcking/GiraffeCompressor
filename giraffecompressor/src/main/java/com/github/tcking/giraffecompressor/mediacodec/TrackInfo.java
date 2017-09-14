package com.github.tcking.giraffecompressor.mediacodec;

import android.media.MediaExtractor;
import android.media.MediaFormat;

/**
 * Created by tc on 5/21/17.
 */

public class TrackInfo {
    private int videoIndex=-1;

    public int getVideoIndex() {
        return videoIndex;
    }

    public int getAudioIndex() {
        return audioIndex;
    }

    private int audioIndex=-1;
    private MediaFormat videoMediaFormat;
    private MediaFormat audioMediaFormat;

    public MediaFormat getVideoMediaFormat() {
        return videoMediaFormat;
    }

    public MediaFormat getAudioMediaFormat() {
        return audioMediaFormat;
    }

    public static TrackInfo from(MediaExtractor mediaExtractor) {
        TrackInfo trackInfo = new TrackInfo();
        int trackCount = mediaExtractor.getTrackCount();
        for (int i = 0; i < trackCount; i++) {
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
            String mime = trackFormat.getString(MediaFormat.KEY_MIME);
            if (trackInfo.videoMediaFormat == null && mime.startsWith("video/")) {
                trackInfo.videoMediaFormat = trackFormat;
                trackInfo.videoIndex = i;

            } else if(trackInfo.audioMediaFormat == null && mime.startsWith("audio/")){
                trackInfo.audioMediaFormat = trackFormat;
                trackInfo.audioIndex = i;
            }
            if (trackInfo.audioMediaFormat != null && trackInfo.videoMediaFormat != null) {
                break;
            }
        }
        return trackInfo;
    }
}
