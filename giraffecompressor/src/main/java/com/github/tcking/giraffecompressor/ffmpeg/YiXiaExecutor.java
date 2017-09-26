package com.github.tcking.giraffecompressor.ffmpeg;

import android.content.Context;

/**
 * Created by TangChao on 2017/9/14.
 */

public class YiXiaExecutor implements FFMPEGCmdExecutor {
    @Override
    public void exec(String cmd) {
//        int i = UtilityAdapter.FFmpegRun("", "ffmpeg " + cmd);
//        if (i!=0) {
//            throw new RuntimeException("FFMPEGCmdExecutor return :" + i);
//        }
    }

    @Override
    public boolean killRunningProcesses(String tag) {
//        UtilityAdapter.FFmpegKill(tag);
        return true;
    }

    @Override
    public void init(Context context) {
//        UtilityAdapter.FFmpegInit(context, String.format("versionName=%s&versionCode=%d&sdkVersion=%s&android=%s&device=%s", "1.0", 1, "1.2.0", "com.github.tcking.giraffecompressorr", Build.MODEL==null?"":Build.MODEL));
    }
}
