package com.github.tcking.giraffecompressor;

import android.content.Context;
import android.os.Build;

import com.github.tcking.giraffecompressor.ffmpeg.FFMPEGCmdExecutorFactory;
import com.github.tcking.giraffecompressor.ffmpeg.FFMPEGVideoCompressor;
import com.github.tcking.giraffecompressor.mediacodec.JellyMediaCodecVideoCompressor;
import com.github.tcking.giraffecompressor.mediacodec.LollipopMediaCodecVideoCompressor;

import java.io.File;
import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * Created by TangChao on 2017/5/22.
 */

public abstract class GiraffeCompressor {
    public static boolean DEBUG = false;
    protected static Context context;
    public static final String TAG = "GiraffeCompressor";
    public static final String TYPE_MEDIACODEC="media_codec";
    public static final String TYPE_FFMPEG="ffmpeg";
    protected static boolean FFmpegNotSupported = false;

    protected File inputFile;
    protected File outputFile;
    protected int bitRate;
    protected float resizeFactor = 1.0f;
    private File watermarkFile;

    public static GiraffeCompressor create(String type) {
        if (TYPE_FFMPEG.equals(type)) {
            return new FFMPEGVideoCompressor();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                return new LollipopMediaCodecVideoCompressor();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                return new JellyMediaCodecVideoCompressor();
            } else {
                return new FFMPEGVideoCompressor();
            }
        }
    }

    public static Context getContext() {
        return context;
    }

    private static void initFFMPEG(Context context) {
        FFMPEGCmdExecutorFactory.create().init(context);
    }

    public static GiraffeCompressor create() {
        return create(TYPE_MEDIACODEC);

    }

    public final GiraffeCompressor input(String input) {
        return input(new File(input));
    }

    public final GiraffeCompressor input(File input) {
        inputFile = input;
        return this;
    }

    public GiraffeCompressor output(String output) {
        return output(new File(output));
    }

    public GiraffeCompressor output(File output) {
        outputFile = output;
        return this;
    }



    public GiraffeCompressor bitRate(int bitRate) {
        this.bitRate = bitRate;
        return this;
    }

    public Observable<Result> ready() {
        Observable<Result> resultObservable = Observable.create(new Observable.OnSubscribe<Result>() {
            @Override
            public void call(Subscriber<? super Result> subscriber) {
                try {
                    verifyParameters();
                    Result result = new Result();
                    compress();
                    String outputFilePath = outputFile.getAbsolutePath();
                    if (watermarkFile != null) {
                        File tmp = new File(outputFilePath + ".tmp");
                        outputFile.renameTo(tmp);
                        String cmd = "-i " + tmp.getAbsolutePath() + " -i " + watermarkFile.getAbsolutePath() + " -filter_complex \"overlay=x=0:y=0\" -f mp4 " + outputFilePath;
                        FFMPEGCmdExecutorFactory.create().exec(cmd);
                        tmp.delete();
                    }
                    result.endTime = System.currentTimeMillis();

                    result.output = outputFilePath;

                    subscriber.onNext(result);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        });

        return resultObservable.subscribeOn(Schedulers.io()).doOnUnsubscribe(new Action0() {
            @Override
            public void call() {
                doOnUnsubscribe();
            }
        });
    }

    protected void doOnUnsubscribe(){

    };

    /**
     * 检查参数
     */
    private void verifyParameters() {
        if (resizeFactor <= 0 || resizeFactor > 1) {
            throw new IllegalArgumentException("resizeFactor must in (0,1) now:"+resizeFactor);
        }
        if (watermarkFile != null && !watermarkFile.exists()) {
            throw new IllegalArgumentException("watermark file not exists:"+watermarkFile.getAbsolutePath());
        }
        if (inputFile == null) {
            throw new NullPointerException("inputFile can't be null");
        }
        if (!inputFile.exists()) {
            throw new IllegalArgumentException("inputFile not exists:"+inputFile.getAbsolutePath());
        }
        if (!inputFile.isFile()) {
            throw new IllegalArgumentException("inputFile is not a file:"+inputFile.getAbsolutePath());
        }
        if (outputFile == null) {
            throw new NullPointerException("outputFile can't be null");
        }
        try {
            if (outputFile.getParentFile() != null) {
                outputFile.getParentFile().mkdirs();
            }
            outputFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException("create output file error:" + outputFile.getAbsolutePath(), e);
        }
    };

    public static void init(Context ctx) {
        context = ctx;
        initFFMPEG(context);
    }

    /**
     * 分辨率缩放因子，默认为1，保持原大小，设值区间为(0,1)
     * @param resizeFactor
     * @return
     */
    public GiraffeCompressor resizeFactor(float resizeFactor) {
        this.resizeFactor = resizeFactor;
        return this;
    }

    public GiraffeCompressor watermark(String path) {
        watermarkFile = new File(path);
        return this;
    }

    public static class Result {
        private long startTime = System.currentTimeMillis();
        private long endTime;
        private String output;

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public String getOutput() {
            return output;
        }

        public long getCostTime(){
            return endTime - startTime;
        }
    }

    protected abstract void compress() throws IOException;
}
