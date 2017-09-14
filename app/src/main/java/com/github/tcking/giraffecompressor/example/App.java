package com.github.tcking.giraffecompressor.example;

import android.app.Application;

import com.github.tcking.giraffecompressor.GiraffeCompressor;

/**
 * Created by TangChao on 2017/5/22.
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        GiraffeCompressor.init(this);
    }
}
