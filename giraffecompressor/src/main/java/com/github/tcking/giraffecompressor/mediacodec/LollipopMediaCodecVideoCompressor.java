package com.github.tcking.giraffecompressor.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by tc on 5/20/17.
 */

@TargetApi(21)
public class LollipopMediaCodecVideoCompressor extends BaseMediaCodecVideoCompressor {
    private static final String TAG = "LollipopMediaCodecVideoCompressor";



    private boolean videoEncoderDone = false;
    private int outputVideoTrackIndex = -1;
    private boolean videoExtractorDone;
    private boolean videoDecoderDone;

    private HandlerThread decodeHandlerThread;
    private Queue<SampleInfo> pendingVideoEncoderOutputBufferInfos = new LinkedList<>();
    private boolean muxerStarted;






    @Override
    protected void compress() throws IOException {
        try {
            //1.获取输入视频的信息
            initTrackInfo();

            muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);


            //2.初始化输出文件格式
            outputVideoMediaFormat = initOutputVideoMediaFormat(trackInfo);


            //3.创建编码器
            encoder = MediaCodec.createEncoderByType(outputVideoMediaFormat.getString(MediaFormat.KEY_MIME));
            encoder.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//                    Log.d(TAG, "encoder.onOutputBufferAvailable");
                    muxVideo(index, info);
                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    e.printStackTrace();
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    outputVideoTrackIndex = muxer.addTrack(codec.getOutputFormat());
                    muxer.start();
                    muxerStarted=true;
                    SampleInfo info;
                    while ((info = pendingVideoEncoderOutputBufferInfos.poll()) != null) {
                        muxVideo(info.index, info.bufferInfo);
                    }
                }
            });
            encoder.configure(outputVideoMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
            encoder.start();


            //4.创建解码器
            decodeHandlerThread = new HandlerThread("decoder-thread");
            decodeHandlerThread.start();
            CallbackHandler callbackHandler = new CallbackHandler(decodeHandlerThread.getLooper());
            decoder = callbackHandler.create(false, trackInfo.getVideoMediaFormat().getString(MediaFormat.KEY_MIME), new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
//                    Log.d(TAG, "decoder.onInputBufferAvailable");
                    // Extract video from file and feed to decoder.
                    ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);
                    while (!videoExtractorDone) {
                        mediaExtractor.selectTrack(trackInfo.getVideoIndex());
                        int size = mediaExtractor.readSampleData(decoderInputBuffer, 0);
                        if (size >= 0) {
                            long presentationTime = mediaExtractor.getSampleTime();
                            codec.queueInputBuffer(
                                    index,
                                    0,
                                    size,
                                    presentationTime,
                                    mediaExtractor.getSampleFlags());
                        }
                        videoExtractorDone = !mediaExtractor.advance();
//                        Log.d(TAG, "decoder.onInputBufferAvailable->videoExtractorDone:"+videoExtractorDone);
                        if (videoExtractorDone) {
                            codec.queueInputBuffer(
                                    index,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }
                        if (size >= 0)
                            break;
                    }
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
//                    Log.d(TAG, "decoder.onOutputBufferAvailable");
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        codec.releaseOutputBuffer(index, false);
//                        Log.d(TAG, "decoder.releaseOutputBuffer");
                        return;
                    }
                    boolean render = info.size != 0;
                    codec.releaseOutputBuffer(index, render);
                    if (render) {
                        inputSurface.makeCurrent();
                        outputSurface.awaitNewImage();
                        // Edit the frame and send it to the encoder.
                        outputSurface.drawImage();
                        inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
                        inputSurface.swapBuffers();
                        inputSurface.makeUnCurrent();
                    }

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        videoDecoderDone = true;
                        encoder.signalEndOfInputStream();
                    }

                }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {

                }
            });
            outputSurface = new OutputSurface();
            decoder.configure(trackInfo.getVideoMediaFormat(), outputSurface.getSurface(), null, 0);
            decoder.start();

            inputSurface.makeUnCurrent();


            synchronized (this) {
                while (!videoEncoderDone) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
        } finally {
            try {
                if (mediaExtractor != null) {
                    mediaExtractor.release();
                    mediaExtractor = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (decoder != null) {
                    decoder.stop();
                    decoder.release();
                    decoder = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (outputSurface != null) {
                    outputSurface.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (encoder != null) {
                    encoder.stop();
                    encoder.release();
                    encoder = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                    muxer = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (inputSurface != null) {
                    inputSurface.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (decodeHandlerThread != null) {
                decodeHandlerThread.quitSafely();
            }
        }
    }



    private void muxVideo(int index, MediaCodec.BufferInfo info) {
        if (!muxerStarted) {
            pendingVideoEncoderOutputBufferInfos.offer(new SampleInfo(index, info));
            return;
        }
        ByteBuffer encoderOutputBuffer = encoder.getOutputBuffer(index);
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // Simply ignore codec config buffers.
            encoder.releaseOutputBuffer(index, false);
            return;
        }
        if (info.size != 0) {
            muxer.writeSampleData(outputVideoTrackIndex, encoderOutputBuffer, info);
        }
        encoder.releaseOutputBuffer(index, false);
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            synchronized (this) {
                videoEncoderDone = true;
                notifyAll();
            }
        }
    }




    static class CallbackHandler extends Handler {
        CallbackHandler(Looper l) {
            super(l);
        }

        private MediaCodec codec;
        private boolean encoder;
        private MediaCodec.Callback callback;
        private String mime;
        private boolean isSetDone;

        @Override
        public void handleMessage(Message msg) {
            try {
                codec = encoder ? MediaCodec.createEncoderByType(mime) : MediaCodec.createDecoderByType(mime);
            } catch (IOException ioe) {
            }
            codec.setCallback(callback);
            synchronized (this) {
                isSetDone = true;
                notifyAll();
            }
        }

        public MediaCodec create(boolean encoder, String mime, MediaCodec.Callback callback) {
            this.encoder = encoder;
            this.mime = mime;
            this.callback = callback;
            isSetDone = false;
            sendEmptyMessage(0);
            synchronized (this) {
                while (!isSetDone) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                    }
                }
            }
            return codec;
        }

    }
}
