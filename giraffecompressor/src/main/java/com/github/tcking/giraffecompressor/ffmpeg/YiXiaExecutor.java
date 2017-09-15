package com.github.tcking.giraffecompressor.ffmpeg;

import android.content.Context;

/**
 * Created by TangChao on 2017/9/14.
 */

public class YiXiaExecutor implements FFMPEGCmdExecutor {
    @Override
    public boolean exec(String cmd) {
        return false;
    }

    @Override
    public boolean killRunningProcesses(String tag) {
        return false;
    }

    @Override
    public void init(Context context) {
//        UtilityAdapter.FFmpegInit(context, String.format("versionName=%s&versionCode=%d&sdkVersion=%s&android=%s&device=%s",
//                "1.0", 1, "1.2.0", "com.tcking.videocompressor", Build.MODEL==null?"":Build.MODEL));
    }
}
