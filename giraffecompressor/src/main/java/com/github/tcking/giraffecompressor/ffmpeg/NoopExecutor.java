package com.github.tcking.giraffecompressor.ffmpeg;

import android.content.Context;

/**
 * Created by TangChao on 2017/9/15.
 */

public class NoopExecutor implements FFMPEGCmdExecutor {
    @Override
    public void exec(String cmd) {
        throw new RuntimeException("call FFMPEGCmdExecutorFactory.registerFFMPEGExecutor() before init");
    }

    @Override
    public boolean killRunningProcesses(String tag) {
        return false;
    }

    @Override
    public void init(Context context) {

    }
}
