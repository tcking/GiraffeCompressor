package com.github.tcking.giraffecompressor.example;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Toast;

import com.github.tcking.giraffecompressor.GiraffeCompressor;
import com.github.tcking.viewquery.ViewQuery;

import java.io.File;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import tcking.github.com.giraffeplayer2.GiraffePlayer;
import tcking.github.com.giraffeplayer2.VideoInfo;

import static com.github.tcking.giraffecompressor.GiraffeCompressor.TYPE_FFMPEG;
import static com.github.tcking.giraffecompressor.GiraffeCompressor.TYPE_MEDIACODEC;

public class MainActivity extends AppCompatActivity {
    private ViewQuery $;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        $ = new ViewQuery(this);
        $.id(R.id.et_bitrate).text("" + ($.screenHeight() * $.screenWidth()));
        $.id(R.id.et_output).text("/sdcard/test_compress_" + System.currentTimeMillis() + ".mp4");
        $.id(R.id.btn_start).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doCompress();
            }
        });
        $.id(R.id.btn_play).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GiraffePlayer.play(getApplication(), new VideoInfo(Uri.parse($.id(R.id.et_output).text())));
            }
        }).gone();

    }

    private void doCompress() {
        String inputText = $.id(R.id.et_input).text();
        final File inputFile = new File(inputText);
        if (!inputFile.exists()) {
            Toast.makeText(getApplication(), "input file not exists", Toast.LENGTH_SHORT).show();
            return;
        }

        GiraffeCompressor.create($.id(R.id.rb_m).checked() ? TYPE_MEDIACODEC : TYPE_FFMPEG)//默认采用mediacodec,可通过create(TYPE_FFMPEG)获取ffmpeg的实现
                .input(inputText) //set video to be compressed
                .output($.id(R.id.et_output).text()) //set compressed video output
                .bitRate(Integer.parseInt($.id(R.id.et_bitrate).text()))//set bitrate 码率
                .resizeFactor(Float.parseFloat($.id(R.id.et_resize_factor).text()))//set video resize factor 分辨率缩放,默认保持原分辨率
//                .watermark("/sdcard/videoCompressor/watermarker.png")//add watermark(take a long time) 水印图片(需要长时间处理)
                .ready()
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        $.id(R.id.tv_console).text("compressing:" + inputFile.getAbsolutePath());
                        $.id(R.id.btn_start).enabled(false).text("compressing...").showInputMethod(false);
                        $.id(R.id.btn_play).gone();

                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<GiraffeCompressor.Result>() {
                    @Override
                    public void onCompleted() {
                        $.id(R.id.btn_start).enabled(true).text("start compress");
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        $.id(R.id.btn_start).enabled(true).text("start compress");
                        $.id(R.id.tv_console).text("error:" + e.getMessage());
                        $.id(R.id.btn_play).gone();

                    }

                    @Override
                    public void onNext(GiraffeCompressor.Result s) {
                        String msg = String.format("compress completed \ntake time:%s ms \nout put file:%s", s.getCostTime(), s.getOutput());
                        msg = msg + "\ninput file size:" + Formatter.formatFileSize(getApplication(), inputFile.length());
                        msg = msg + "\nout file size:" + Formatter.formatFileSize(getApplication(), new File(s.getOutput()).length());
                        System.out.println(msg);
                        $.id(R.id.tv_console).text(msg);
                        $.id(R.id.btn_play).visible();
                    }
                });
    }

}
