package com.yich.layout.picwatcherlib;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

/**
 * 通过图片加载的状态回到接口（你可以通过glide，picass等等加载）
 * Created by yich on 2018/2/28.
 * 2016928168@qq.com
 */

public interface LoadCallback {
    void onResourceReady(Bitmap resource);

    void onLoadStarted(Drawable placeholder);

    void onLoadFailed(Drawable errorDrawable);
    void onUpdateProgress(int progress);
}