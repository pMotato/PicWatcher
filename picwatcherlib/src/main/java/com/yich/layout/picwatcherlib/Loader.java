package com.yich.layout.picwatcherlib;

import android.content.Context;

/**
 * 图片设置加载器（glide.picass等）
 * Created by yich on 2018/2/28.
 * 2016928168@qq.com
 */

public interface Loader {
    void load(Context context, String url, LoadCallback lc);
}