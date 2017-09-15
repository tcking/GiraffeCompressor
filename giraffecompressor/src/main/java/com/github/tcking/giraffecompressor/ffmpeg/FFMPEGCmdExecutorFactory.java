package com.github.tcking.giraffecompressor.ffmpeg;

import android.content.Context;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by TangChao on 2017/9/14.
 */

public class FFMPEGCmdExecutorFactory {

    private static Set<Class<? extends FFMPEGCmdExecutor>> executorClasss = new TreeSet<>();

    public static FFMPEGCmdExecutor create(Context context){
        for (Class<? extends FFMPEGCmdExecutor> z : executorClasss) {
            try {
                return z.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new NoopExecutor();
    }


    public static void registerFFMPEGExecutor(Class<? extends FFMPEGCmdExecutor> clazz) {
        executorClasss.add(clazz);
    }
}
