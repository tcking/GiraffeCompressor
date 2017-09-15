# GiraffeCompressor

A easy to use video compressor on android.

## home to use

``` java


//step 1: add jcenter to repositories

allprojects {
    repositories {
        ...
        jcenter()
    }
}

//step 2: add dependency

    compile 'com.github.tcking:giraffecompressor:0.1.2'

//step 3: init compressor

GiraffeCompressor.init(context);


//step 4: using compressor

GiraffeCompressor.create() //two implementations: mediacodec and ffmpeg,default is mediacodec
                  .input(inputFile) //set video to be compressed
                  .output(outputFile) //set compressed video output
                  .bitRate(bitRate)//set bitrate 码率
                  .resizeFactor(Float.parseFloat($.id(R.id.et_resize_factor).text()))//set video resize factor 分辨率缩放,默认保持原分辨率
                  .watermark("/sdcard/videoCompressor/watermarker.png")//add watermark(take a long time) 水印图片(需要长时间处理)
                  .ready()
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
                          $.id(R.id.tv_console).text("error:"+e.getMessage());

                      }

                      @Override
                      public void onNext(GiraffeCompressor.Result s) {
                          String msg = String.format("compress completed \ntake time:%s \nout put file:%s", s.getCostTime(), s.getOutput());
                          msg = msg + "\ninput file size:"+ Formatter.formatFileSize(getApplication(),inputFile.length());
                          msg = msg + "\nout file size:"+ Formatter.formatFileSize(getApplication(),new File(s.getOutput()).length());
                          System.out.println(msg);
                          $.id(R.id.tv_console).text(msg);
                      }
                  })

```

## screenshot

![](https://raw.githubusercontent.com/tcking/GiraffeCompressor/master/screenshot/device-2017-09-14-155814.png)
