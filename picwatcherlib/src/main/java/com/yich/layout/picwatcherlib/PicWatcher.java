package com.yich.layout.picwatcherlib;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.yich.layout.picwatcherlib.progress.ProgressModelLoader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.yich.layout.picwatcherlib.ImageWatcher.Helper.VIEW_IMAGE_WATCHER_ID;


/**
 * Created by yich on 2018/2/1.
 * 2016928168@qq.com
 */

public class PicWatcher {
    public final  static  int MSG_UPDATE_PROGRESS=1;

    /**
     *
     * @param activity 当前的activity
     * @param p 当前点击的imageView
     * @param position 图片显示在图片集合中的位置
     * @param thumUrlsImageView  可见的imageview的集合
     * @param bigUrlLists 所有图片的下载地址集合
     * @param overlay  覆盖在图片上的overlay
     * @param canCache 是否缓存图片
     */
    private static  void showImages(final Activity activity, ImageView p, List<ImageView> thumUrlsImageView,int position, List<String> bigUrlLists,View overlay,boolean canCache){
        if (activity!=null){
            ImageWatcher watcher=getWatchLayout(activity,canCache);
            watcher.show(p,thumUrlsImageView, position,bigUrlLists,overlay);
        }
    }

    /**
     *
     * @param activity 当前的activity
     * @param p 当前点击的imageView
     * @param position 图片显示在图片集合中的位置
     * @param thumUrlsImageView  可见的imageview的集合
     * @param bigUrlLists 所有图片的下载地址集合
     */
    public static  void showImages(final Activity activity, ImageView p,int position, List<ImageView> thumUrlsImageView, List<String> bigUrlLists){
        showImages(activity,p,thumUrlsImageView,position,bigUrlLists,null,true);
    }

    /**
     *
     * @param activity 当前的activity
     * @param p 当前点击的imageView
     * @param position 图片显示在图片集合中的位置
     * @param thumUrlsImageView  可见 的imageview的集合
     * @param bigUrlLists 所有图片的下载地址
     * @param canCache 是否需要缓存图片，默认缓存
     */
    public static  void showImages(final Activity activity, ImageView p,int position, List<ImageView> thumUrlsImageView, List<String> bigUrlLists,boolean  canCache){
        showImages(activity,p,thumUrlsImageView,position,bigUrlLists,null,canCache);
    }
    /**
     *
     * @param activity 当前的activity
     * @param p 当前点击的imageView
     * @param bigUrlLists 所有图片的下载地址
     * @param canCache 是否需要缓存图片，默认缓存
     */
    public static  void showSingleImage(final Activity activity, ImageView p,  String bigUrlLists , View overLay,boolean canCache){
        ArrayList<ImageView> thumImageViews=new ArrayList<ImageView>();
        thumImageViews.add(p);
        ArrayList<String>  bigurls=new ArrayList<>();
        bigurls.add(bigUrlLists);
        showImages(activity,p,thumImageViews,0,bigurls,overLay,canCache);
    }
    public static  void showSingleImage(final Activity activity, ImageView p, String bigUrlLists , View overLay){
        ArrayList<ImageView> thumImageViews=new ArrayList<ImageView>();
        thumImageViews.add(p);
        ArrayList<String>  bigurls=new ArrayList<>();
        bigurls.add(bigUrlLists);
        showImages(activity,p,thumImageViews,0,bigurls,overLay,true);
    }
    public static  void showSingleImage(final Activity activity, ImageView p, String bigUrlLists){
              showSingleImage(activity,p,bigUrlLists,null,true);
    }
    private static ImageWatcher getWatchLayout(Activity activity,boolean canCache) {
        if ( (ViewGroup) activity.getWindow().getDecorView().findViewById(VIEW_IMAGE_WATCHER_ID)!=null){
             return (ImageWatcher)((ViewGroup) activity.getWindow().getDecorView().findViewById(VIEW_IMAGE_WATCHER_ID));
        }else{
            return createViewPicLayout(activity, canCache);
        }
    }



    /**
     *  创建图片查看的layout层
     * @param activity
     */
    private static ImageWatcher createViewPicLayout(final Activity activity,final  boolean canCache) {
      return   ImageWatcher.Helper.with(activity) // 一般来讲， ImageWatcher 需要占据全屏的位置
                .setTranslucentStatus(calcStatusBarHeight(activity)) // 如果是透明状态栏，你需要给ImageWatcher标记 一个偏移值，以修正点击ImageView查看的启动动画的Y轴起点的不正确
                .setErrorImageRes(R.drawable.image_view_error) // 配置error图标 如果不介意使用lib自带的图标，并不一定要调用这个API
//                .setOnPictureLongPressListener(this) // 长按图片的回调，你可以显示一个框继续提供一些复制，发送等功能
                .setLoader(new Loader() {

                    @Override
                    public void load(Context context, String url,final  LoadCallback lc) {
                       SimpleTarget<Bitmap> target= new SimpleTarget<Bitmap>()
                        {
                            @Override
                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                lc.onResourceReady(resource);
                            }

                            @Override
                            public void onLoadFailed(Exception e, Drawable errorDrawable) {
                                super.onLoadFailed(e, errorDrawable);
                                lc.onLoadFailed(errorDrawable);
                            }

                            @Override
                            public void onStart() {
                                super.onStart();
                                lc.onLoadStarted(null);
                            }
                        };
                        if (url!=null&&url.contains("http")){
                            Glide.with(context).using(new ProgressModelLoader((new ProgressHandler(activity, lc))))
                                    .load(url).asBitmap().diskCacheStrategy(canCache? DiskCacheStrategy.ALL:DiskCacheStrategy.NONE)
                                    .into(target
                                    );
                        }else{
                            Glide.with(context)
                                    .load(url).asBitmap().diskCacheStrategy(canCache?DiskCacheStrategy.ALL:DiskCacheStrategy.NONE)
                                    .into(target
                                    );
                        }

                    }
                })
                .create();
        //        Utils.fitsSystemWindows(isTranslucentStatus, findViewById(R.id.v_fit));
    }

    private static class ProgressHandler extends Handler {

        private final WeakReference<Activity> mActivity;
        private final LoadCallback lc;

        public ProgressHandler(Activity activity, LoadCallback progressImageView) {
            super(Looper.getMainLooper());
            mActivity = new WeakReference<>(activity);
            lc = progressImageView;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final Activity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MSG_UPDATE_PROGRESS:
                        int percent = msg.arg1*100/msg.arg2;
                        lc.onUpdateProgress(percent);
                        break;
                    default:
                        break;
                }
            }
        }
    }


/**
 * 获取状态栏的高度
 */
    public static int calcStatusBarHeight(Context context) {
        int statusHeight =0;
        try {
            Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height").get(object).toString());
            statusHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusHeight;
    }



}
