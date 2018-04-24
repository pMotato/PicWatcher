# PicWatcher
### feature
这是一个高仿微信的图片查看Demo，picwatcherlib这个moudle可以直接拿来用，该库基于[P02_ImageWatcher](https://github.com/iielse/DemoProjects/tree/master/P02_ImageWatcher)的二次开发，
修复原库如下问题：
1.长图放大之后无法预览 <br />
2.缩略图到原图动画更加平滑 <br />
3.解决图片大于bitmap的显示不了的bug <br />
4.图片列表可滑动时，滑动之后在进行图片显示会错位 <br />
#### 增加新特点
1.手机回退键监听放到 picwatcherlib 中来监听 <br />
2.增加图片下载监听的功能 <br />
3.背景增加高斯模糊 <br />
4.图片查看层增加overlay <br />
5.增加大图上下滑动到边缘的回弹效果 <br />





## 废话不多说上图 <br />
![image](https://github.com/yuqiyich/PicWatcher/blob/master/art/art1.gif)

![image](https://github.com/yuqiyich/PicWatcher/blob/master/art/art2.gif)

![image](https://github.com/yuqiyich/PicWatcher/blob/master/art/art3.gif)


## Usage
  1.  git上下载这个项目，然后直接引用picwatcherlib这个moudle。
  2.
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

  调用PicWatcher.showImages(填入上面的相关参数)，即可实现gif的效果

## NextToDo
1.增加banner中滑动的图片点击显示效果 <br />
2.增加没有进场动画的展示效果，也就是不是从页面点击显示的图集的
