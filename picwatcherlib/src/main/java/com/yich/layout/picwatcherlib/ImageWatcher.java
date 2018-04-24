package com.yich.layout.picwatcherlib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.opengl.GLES10;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.commit451.nativestackblur.NativeStackBlur;
import com.yich.layout.picwatcherlib.utils.ScreenUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;




/**
 *
 * <p>
 * 图片查看器，为各位追求用户体验的daLao提供更优质的服务<br/>
 * <b>它能够</b><br/>
 * 1、点击图片时以一种无缝顺畅的动画切换到图片查看的界面，同样以一种无缝顺畅的动画退出图片查看界面
 * 2、支持多图查看，快速翻页，双击放大，单击退出，双手缩放旋转图片
 * 3、下拽退出查看图片的操作，以及效果是本View的最大卖点(仿微信)
 */
public class ImageWatcher extends FrameLayout implements GestureDetector.OnGestureListener, ViewPager.OnPageChangeListener{
    private static final int SINGLE_TAP_UP_CONFIRMED = 1;
    private static final int MSG_IS_REASURCE_READY = 2;
    private static final int MSG_IS_THUM_AMIN_END = 3;
    private static final String TAG ="yich" ;
    private final Handler mHandler;
    private static final String  ID_LOADING_VIEW = "loadview"; //
    private static final String  ID_ERROR_VIEW = "errorview"; //
    private static final String  ID_OVER_LAY_VIEW = "overlay"; //
    static final float MIN_SCALE = 0.5f;
    static final float MAX_SCALE = 3.8f;
    private  int[] maxBitmapSize=new int[1];//bitmap的极限值
    private int maxTranslateX;
    private int maxTranslateY;
    private boolean isBigExit=false;
    //手指的不同状态
    private static final int TOUCH_MODE_NONE = 0; // 无状态
    private static final int TOUCH_MODE_DOWN = 1; // 按下
    private static final int TOUCH_MODE_DRAG = 2; // 单点拖拽
    private static final int TOUCH_MODE_EXIT = 3; // 退出动作
    private static final int TOUCH_MODE_SLIDE = 4; // 页面滑动
    private static final int TOUCH_MODE_SCALE_ROTATE = 5; // 缩放旋转
    private static final int TOUCH_MODE_LOCK = 6; // 缩放旋转锁定
    private static final int TOUCH_MODE_AUTO_FLING = 7; // 动画中

    private static final int BIG_PIC_LOAD_DEAFUALT = 0; //还未加载，或者加载中
    private static final int BIG_PIC_LOAD_SUCC = 1; // 加载成功
    private static final int BIG_PIC_LOAD_FAILED = 2; // 加载失败

    private final float tCurrentIdxTransY;
    private final TextView tCurrentIdx;
    private ImageView iSource;//自己生成的imageView
    private ImageView iOrigin;//在底层的view也就是被点击的view


    private int mErrorImageRes ;
    private int mStatusBarHeight;
    private int mWidth, mHeight;
    private int mBackgroundColor = 0x00000000;
    private int mTouchMode = TOUCH_MODE_NONE;
    private final float mTouchSlop;

    private float mFingersDistance;
    private double mFingersAngle; // 相对于[东] point0作为起点;point1作为终点
    private float mFingersCenterX;
    private float mFingersCenterY;
    private float mExitScalingRef; // 触摸退出进度

    private ValueAnimator animBackground;
    private ValueAnimator animImageTransform;
    private boolean isInTransformAnimation;
    private final GestureDetector mGestureDetector;

    private OnPictureLongPressListener mPictureLongPressListener;
    private ImagePagerAdapter adapter;
    private final ViewPager vPager;
    private List<ImageView> mImageGroupList;
    private List<String> mUrlList;
    private HashMap<Integer,Integer> bigPicLoadResult;//大图加载结果
    private int initPosition;
    private int mPagerPositionOffsetPixels;
    View grayView;
    private Loader loader;//图片加载器
    private Drawable rootViewDrawable;

    private float  backGroudAlpha;//false 是不可见 ;true是可见
    private int orginImageViewPosition=-1;//原始图片的在传进来的imageview中的位置

    public void setLoader(Loader l) {
        loader = l;
    }

    public void setAnimParam(ViewGroup animParam) {
        mWidth = animParam.getMeasuredWidth();
        mHeight = animParam.getMeasuredHeight();
        maxTranslateX = mWidth / 2;
        maxTranslateY = mHeight / 2;
    }



    private static class AminHandler extends Handler {
       private boolean isResourceReady=false;
        private boolean isThumAminEnd=false;
        private Bitmap bitmap;
        private ImageView imageView;
        private final WeakReference<ImageWatcher> mWathcer;


        public AminHandler(ImageWatcher activity) {
            super(Looper.getMainLooper());
            mWathcer = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final ImageWatcher activity = mWathcer.get();
            if (activity != null) {
                switch (msg.what) {
                    case MSG_IS_REASURCE_READY:
                        bitmap=(Bitmap) msg.obj;
                        isResourceReady=true;
                        break;
                    case MSG_IS_THUM_AMIN_END:
                        imageView=(ImageView) msg.obj;
                        isThumAminEnd=true;
                        break;
                    default:
                        break;
                }
                if (isResourceReady&&isThumAminEnd){
                    ViewState vsDefault = ViewState.read(imageView, ViewState.STATE_DEFAULT);
                    //.translationX(sourceDefaultTranslateX).translationY(sourceDefaultTranslateY);
                    Log.d(TAG,"imageView.getTranslationX():"+imageView.getTranslationX());
                    Log.d(TAG,"imageView.getTranslationY():"+imageView.getTranslationY());
                    Log.d(TAG,"imageView.getMeasuredHeight():"+imageView.getMeasuredHeight());
                    Log.d(TAG,"imageView.getMeasuredWidth():"+imageView.getMeasuredWidth());
                    Log.d(TAG,"imageView.getMeasuredHeight():"+imageView.getMeasuredHeight());
                    Log.d(TAG,"bitmap.getWidth:"+bitmap.getWidth());
                    Log.d(TAG,"bitmap.getHeight:"+bitmap.getHeight());
                    imageView.setImageBitmap(bitmap);
                    if (imageView.getMeasuredHeight()==vsDefault.height&&imageView.getMeasuredWidth()==vsDefault.width){

                    }else{
                        activity.animSourceViewStateTransform(imageView, vsDefault);
                    }
                }
            }
        }

        public void clean(){
            isResourceReady=false;
            isThumAminEnd=false;
             bitmap=null;
        }
    }
    private AminHandler mAminhander;
    public ImageWatcher(Context context) {
        this(context, null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getGLESTextureLimitEqualAboveLollipop();
        } else {
            getGLESTextureLimitBelowLollipop();
        }
    }

    public ImageWatcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        mErrorImageRes = R.drawable.image_view_error;
        mHandler = new GestureHandler(this);
        mGestureDetector = new GestureDetector(context, this);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
         grayView=new View(getContext());
        grayView.setBackgroundColor(Color.parseColor("#b0000000"));
        LayoutParams grayviewLayoutP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        grayView.setLayoutParams(grayviewLayoutP);
        addView(grayView);
        addView(vPager = new ViewPager(getContext()));
        vPager.addOnPageChangeListener(this);

        setVisibility(View.INVISIBLE);
        addView(tCurrentIdx = new TextView(context));
        LayoutParams lpCurrentIdx = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lpCurrentIdx.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        tCurrentIdx.setLayoutParams(lpCurrentIdx);
        tCurrentIdx.setTextColor(0xFFFFFFFF);

        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        tCurrentIdxTransY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, displayMetrics) + 0.5f;
        tCurrentIdx.setTranslationY(-1*tCurrentIdxTransY);
        mAminhander=new AminHandler(this);
    }



    /**
     * 调用show方法前，请先调用setLoader 给ImageWatcher提供加载图片的实现
     *
     * @param i              被点击的ImageView
     * @param positon        被点击的imageView在总的显示的图片中的位置
     * @param imageGroupList 被点击的ImageView的所在列表，加载图片时会提前展示列表中已经下载完成的thumb图片
     * @param urlList        被加载的图片url列表，数量必须大于等于 imageGroupList.size。 且顺序应当和imageGroupList保持一致
     */
    public void show(ImageView i, List<ImageView> imageGroupList,int positon, final List<String> urlList,View overLay) {
        if (loader == null) {
            throw new NullPointerException("please invoke `setLoader` first [loader == null]");
        }
        if (mAminhander!=null){
            mAminhander.clean();
        }
        if (i == null || imageGroupList == null || urlList == null || imageGroupList.size() < 1 ||
                urlList.size() < imageGroupList.size()) {
            String info = "i[" + i + "]";
            info += "#imageGroupList " + (imageGroupList == null ? "null" : "size : " + imageGroupList.size());
            info += "#urlList " + (urlList == null ? "null" : "size :" + urlList.size());
            Log.e(TAG,"error params \n" + info);
            return ;

        }
        initPosition = positon;
        orginImageViewPosition=imageGroupList.indexOf(i);
        if (orginImageViewPosition < 0) {
            throw new IllegalArgumentException("param ImageView i must be a member of the List <ImageView> imageGroupList!");
        }

        if (i.getDrawable() == null) return;
        rootViewDrawable =new BitmapDrawable(getContext().getResources(), NativeStackBlur.process(loadBitmapFromViewBySystem((View)getParent()), 20)) ;
        setFrameBackground(rootViewDrawable);
        if (animImageTransform != null) animImageTransform.cancel();
        animImageTransform = null;

        mImageGroupList = imageGroupList;
        mUrlList = urlList;

        bigPicLoadResult=new HashMap<Integer,Integer>(mUrlList.size());
         for (int k=0; k<mUrlList.size(); k++){
             bigPicLoadResult.put(k,BIG_PIC_LOAD_DEAFUALT);
         }
        iOrigin = i;
        iSource = null;

        ImageWatcher.this.setVisibility(View.VISIBLE);
        ImageWatcher.this.removeView(ImageWatcher.this.findViewWithTag(ID_OVER_LAY_VIEW));
        if (overLay!=null){
            if (overLay.getLayoutParams()!=null){
                ImageWatcher.this.addView(overLay);
            }else{
                LayoutParams lp=new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.gravity=Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                overLay.setLayoutParams(lp);
                ImageWatcher.this.addView(overLay);
            }
        }
        vPager.setFocusable(true);
        vPager.setFocusableInTouchMode(true);
        vPager.requestFocus();
        vPager.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG,"imageWatcher===onKeyDown");
                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            return handleBackPressed();
                        }
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
        vPager.setAdapter(adapter = new ImagePagerAdapter());
        vPager.setCurrentItem(initPosition);
        refreshCurrentIdx(initPosition);
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (iSource == null) return true;
        if (isInTransformAnimation) return true;

        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);

        final int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_UP:
                onUp(event);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (vsDefault != null && (mTouchMode != TOUCH_MODE_SLIDE) || mPagerPositionOffsetPixels == 0) {
                    if (mTouchMode != TOUCH_MODE_SCALE_ROTATE) {
                        mFingersDistance = 0;
                        mFingersAngle = 0;
                        mFingersCenterX = 0;
                        mFingersCenterY = 0;
                        ViewState.write(iSource, ViewState.STATE_TOUCH_SCALE_ROTATE);
                    }
                    mTouchMode = TOUCH_MODE_SCALE_ROTATE;
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (vsDefault != null && mTouchMode != TOUCH_MODE_SLIDE) {
                    if (event.getPointerCount() - 1 < 1 + 1) {
                        mTouchMode = TOUCH_MODE_LOCK;
                    }
                }
                break;
        }
        return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mTouchMode = TOUCH_MODE_DOWN;
        ViewState.write(iSource, ViewState.STATE_TOUCH_DOWN);
        vPager.onTouchEvent(e);
        return true;
    }

    public void onUp(MotionEvent e) {
        if (mTouchMode == TOUCH_MODE_EXIT) {
            handleExitTouchResult();
        } else if (mTouchMode == TOUCH_MODE_SCALE_ROTATE
                || mTouchMode == TOUCH_MODE_LOCK) {
            handleScaleRotateTouchResult();
        } else if (mTouchMode == TOUCH_MODE_DRAG) {
            handleDragTouchResult();
        }
        try {
            vPager.onTouchEvent(e);
        } catch (Exception err) {
        }
    }

    /**
     * 处理滑动过程
     * @param e1
     * @param e2
     * @param distanceX
     * @param distanceY
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        final float moveX = (e1 != null) ? e2.getX() - e1.getX() : 0;
        final float moveY = (e1 != null) ? e2.getY() - e1.getY() : 0;
        Log.d(TAG,"111Math.abs(moveX) :"+Math.abs(moveX));
        Log.d(TAG,"11moveY:"+moveY);
//        Log.d(TAG,"11mTouchSlop:"+mTouchSlop);
        if (mTouchMode == TOUCH_MODE_DOWN) {
            Log.d(TAG,"TOUCH_MODE_DOWN&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&"+mTouchMode);
            isBigExit=false;
            if (Math.abs(moveX) > mTouchSlop || Math.abs(moveY) > mTouchSlop) {
                ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);
                ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
                if (vsDefault == null) {
                    // 没有vsDefault标志的View说明图标正在下载中。转化为Slide手势，可以进行viewpager的翻页滑动
                    mTouchMode = TOUCH_MODE_SLIDE;
                }  else if (Math.abs(moveX) < mTouchSlop && moveY > mTouchSlop * 3&&!(vsCurrent.scaleY > vsDefault.scaleY || vsCurrent.scaleX > vsDefault.scaleX)) {
                    if (vsCurrent.scaleY > vsDefault.scaleY || vsCurrent.scaleX > vsDefault.scaleX){
                        isBigExit=true;
                    }
                    // 单手垂直下拉。转化为Exit手势，可以在下拉过程中看到原始界面;
                    mTouchMode = TOUCH_MODE_EXIT;
                } else if (vsCurrent.scaleY > vsDefault.scaleY || vsCurrent.scaleX > vsDefault.scaleX) {
                        // 图片当前为放大状态。宽或高超出了屏幕尺寸
                        if (mTouchMode != TOUCH_MODE_DRAG) {
                            ViewState.write(iSource, ViewState.STATE_DRAG);//按下记录下位置
                        }
                        // 转化为Drag手势，可以对图片进行拖拽操作
                        mTouchMode = TOUCH_MODE_DRAG;
                        String imageOrientation = (String) iSource.getTag(R.id.image_orientation);

                        if ("horizontal".equals(imageOrientation)) {//初始是宽贴着屏幕
                            float translateXEdge = vsDefault.width * (vsCurrent.scaleX - 1) / 2;
                            if (vsCurrent.translationX >= translateXEdge && moveX > 0) {

                                Log.d(TAG,"图片左边已到边界vsCurrent.translationX ："+vsCurrent.translationX +"translateXEdge:"+translateXEdge+"imagview translationX::"+iSource.getTranslationX());
                                // 图片位于边界，且仍然尝试向边界外拽动。。转化为Slide手势，可以进行viewpager的翻页滑动
                                mTouchMode = TOUCH_MODE_SLIDE;
                            } else if (vsCurrent.translationX <= -translateXEdge && moveX < 0) {
                                // 同上
                                Log.d(TAG,"图片右边已到边界");
                                mTouchMode = TOUCH_MODE_SLIDE;
                            }
                        } else if ("vertical".equals(imageOrientation)) {//初始是高贴着屏幕
                            if (vsDefault.width * vsCurrent.scaleX <= mWidth)//放大之后宽还是小于屏幕
                            {
                                if (Math.abs(moveY) < mTouchSlop &&  Math.abs(moveX) > mTouchSlop * 3){//小于屏幕是还是让左右滑动
                                    mTouchMode = TOUCH_MODE_SLIDE;
                                }
                                // 同上
//                                mTouchMode = TOUCH_MODE_SLIDE;
                            } else {
                                    float translateXRightEdge = vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;
                                    float translateXLeftEdge = mWidth - vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;
                                    if (vsCurrent.translationX >= translateXRightEdge && moveX > 0) {
                                        // 同上
                                        mTouchMode = TOUCH_MODE_SLIDE;
                                    } else if (vsCurrent.translationX <= translateXLeftEdge && moveX < 0) {
                                        // 同上
                                        mTouchMode = TOUCH_MODE_SLIDE;
                                    }
                            }

                        }

                }else if (Math.abs(moveX) > mTouchSlop) {
                    // 左右滑动。转化为Slide手势，可以进行viewpager的翻页滑动
                    mTouchMode = TOUCH_MODE_SLIDE;
                }
            }
        }
        Log.d(TAG,"mTouchMode:"+mTouchMode);
        if (mTouchMode == TOUCH_MODE_SLIDE) {
            vPager.onTouchEvent(e2);
        } else if (mTouchMode == TOUCH_MODE_SCALE_ROTATE) {
            handleScaleRotateGesture(e2);
        } else if (mTouchMode == TOUCH_MODE_EXIT) {
            handleExitGesture(e2, e1);
        } else if (mTouchMode == TOUCH_MODE_DRAG) {
        /*    ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
            ViewState vsCurrent = ViewState.read(iSource, ViewState.STATE_CURRENT);
            float pretransy=(vsCurrent.translationY+moveY-vsDefault.translationY);
            float delt=((vsCurrent.scaleY*vsDefault.height)-mHeight)/2.0f;
            Log.d(TAG,"pretransy:"+pretransy);
            Log.d(TAG,"delt:"+delt);
            if ((Math.abs(moveX) < mTouchSlop && moveY > mTouchSlop * 3)
                    &&((vsCurrent.scaleY*vsDefault.height)<=mHeight||(vsCurrent.scaleY*vsDefault.height>mHeight&&pretransy>delt))
                    )//大图上边界出来情况下的下拉
            {
                mTouchMode = TOUCH_MODE_EXIT;
                isBigExit=true;
                handleExitGesture(e2, e1);
            }else{*/
                handleDragGesture(e2, e1);
//            }

        }

        return false;
    }

    /**
     * 处理单击的手指事件
     */
    public boolean onSingleTapConfirmed() {
        if (iSource == null) return false;
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null || (vsCurrent.scaleY <= vsDefault.scaleY && vsCurrent.scaleX <= vsDefault.scaleX)) {
            mExitScalingRef = 0;
        } else {
            mExitScalingRef = 1;
        }
        mExitScalingRef=0;
        handleExitTouchResult();
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        boolean hadTapMessage = mHandler.hasMessages(SINGLE_TAP_UP_CONFIRMED);
        if (hadTapMessage) {
            mHandler.removeMessages(SINGLE_TAP_UP_CONFIRMED);
            handleDoubleTapTouchResult();
            return true;
        } else {
            mHandler.sendEmptyMessageDelayed(SINGLE_TAP_UP_CONFIRMED, 350);
        }
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mPictureLongPressListener != null) {
            mPictureLongPressListener.onPictureLongPress(iSource, mUrlList.get(vPager.getCurrentItem()), vPager.getCurrentItem());
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    /**
     * 处理响应退出图片查看
     */
    private void handleExitGesture(MotionEvent e2, MotionEvent e1) {
        Log.d(TAG,"handleExitGesture");
        if (iSource == null) return;
        ViewState vsTouchDown = ViewState.read(iSource, ViewState.STATE_TOUCH_DOWN);
        if (vsTouchDown == null) return;

        mExitScalingRef = 1;
        final float moveY = e2.getY() - e1.getY();
        final float moveX = e2.getX() - e1.getX();
        if (moveY > 0) {
            mExitScalingRef -= moveY / (getHeight()*iSource.getScaleY());
        }
        if (mExitScalingRef < MIN_SCALE) mExitScalingRef = MIN_SCALE;

        iSource.setTranslationX(vsTouchDown.translationX + moveX);
        iSource.setTranslationY(vsTouchDown.translationY + moveY);
        iSource.setScaleX(vsTouchDown.scaleX * mExitScalingRef);
        iSource.setScaleY(vsTouchDown.scaleY * mExitScalingRef);
         getBackground().setAlpha((int)(mExitScalingRef*255));
         grayView.setAlpha(mExitScalingRef);
//        setBackgroundColor(mColorEvaluator.evaluate(mExitScalingRef, 0x00000000, 0xFF000000));
    }

    /**
     * 处理响应双手拖拽缩放旋转
     */
    private void handleScaleRotateGesture(MotionEvent e2) {
        if (iSource == null) return;
        final ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        final ViewState vsTouchScaleRotate = ViewState.read(iSource, ViewState.STATE_TOUCH_SCALE_ROTATE);
        if (vsTouchScaleRotate == null) return;

        if (e2.getPointerCount() < 2) return;
        final float deltaX = e2.getX(1) - e2.getX(0);
        final float deltaY = e2.getY(1) - e2.getY(0);
       /* double angle = Math.toDegrees(Math.atan(deltaX / deltaY));
        if (deltaY < 0) angle = angle + 180;
        if (mFingersAngle == 0) mFingersAngle = angle;

        float changedAngle = (float) (mFingersAngle - angle);
        float changedAngleValue = (vsTouchScaleRotate.rotation + changedAngle) % 360;
        if (changedAngleValue > 180) {
            changedAngleValue = changedAngleValue - 360;
        } else if (changedAngleValue < -180) {
            changedAngleValue = changedAngleValue + 360;
        }
        iSource.setRotation(changedAngleValue);*/

        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (mFingersDistance == 0) mFingersDistance = distance;

        float changedScale = (mFingersDistance - distance) / (mWidth * 0.8f);
        float scaleResultX = vsTouchScaleRotate.scaleX - changedScale;
        if (scaleResultX < MIN_SCALE) scaleResultX = MIN_SCALE;
        else if (scaleResultX > MAX_SCALE) scaleResultX = MAX_SCALE;
        iSource.setScaleX(scaleResultX);
        float scaleResultY = vsTouchScaleRotate.scaleY - changedScale;
        if (scaleResultY < MIN_SCALE) scaleResultY = MIN_SCALE;
        else if (scaleResultY > MAX_SCALE) scaleResultY = MAX_SCALE;
        iSource.setScaleY(scaleResultY);

        float centerX = (e2.getX(1) + e2.getX(0)) / 2;
        float centerY = (e2.getY(1) + e2.getY(0)) / 2;
        if (mFingersCenterX == 0 && mFingersCenterY == 0) {
            mFingersCenterX = centerX;
            mFingersCenterY = centerY;
        }
        float changedCenterX = mFingersCenterX - centerX;
        float changedCenterXValue = vsTouchScaleRotate.translationX - changedCenterX;
        if (changedCenterXValue > maxTranslateX) changedCenterXValue = maxTranslateX;
        else if (changedCenterXValue < -maxTranslateX) changedCenterXValue = -maxTranslateX;
        iSource.setTranslationX(changedCenterXValue);

        float changedCenterY = mFingersCenterY - centerY;
        float changedCenterYValue = vsTouchScaleRotate.translationY - changedCenterY;
        if (changedCenterYValue > maxTranslateY) changedCenterYValue = maxTranslateY;
        else if (changedCenterYValue < -maxTranslateY) changedCenterYValue = -maxTranslateY;
        iSource.setTranslationY(changedCenterYValue);
    }

    /**
     * 处理响应单手拖拽平移
     */
    private void handleDragGesture(MotionEvent e2, MotionEvent e1) {
        if (iSource == null) return;
        final float moveY = e2.getY() - e1.getY();
        final float moveX = e2.getX() - e1.getX();

        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
//        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_ORIGIN);
        if (vsDefault == null) return;
        ViewState vsTouchDrag = ViewState.read(iSource, ViewState.STATE_DRAG); //读取按下时drag前的状态也就是down的状态
        if (vsTouchDrag == null) return;
        boolean isXCanMove=true;
        boolean isYCanMove=true;
        float translateXValue = vsTouchDrag.translationX + moveX * 1.6f;
        float translateYValue = vsTouchDrag.translationY + moveY* 1.6f;
        String imageOrientation = (String) iSource.getTag(R.id.image_orientation);


        if ("horizontal".equals(imageOrientation)) {
            float translateXEdge = vsDefault.width * (vsTouchDrag.scaleX - 1) / 2;
            if (translateXValue > translateXEdge) {
                translateXValue = translateXEdge + (translateXValue - translateXEdge) * 0.12f;
            } else if (translateXValue < -translateXEdge) {
                translateXValue = -translateXEdge + (translateXValue - (-translateXEdge)) * 0.12f;
            }
            if (vsDefault.height * vsTouchDrag.scaleY <= mHeight){//放大之后高还是小于屏幕
                isYCanMove=false;
              /*  if(Math.abs(moveX) < mTouchSlop && moveY > mTouchSlop * 3){//如果判断是下滑就下滑处理
                    isBigExit=true;
                    mTouchMode=TOUCH_MODE_EXIT;
                    return ;
                }*/
            }else{
                float translateYEdgeLen = (vsDefault.height *vsTouchDrag.scaleY-mHeight) / 2;
                /*if (Math.abs(translateYValue -vsDefault.translationY) > translateYEdgeLen+ ScreenUtils.dpToPx(getContext(),30))
                {
                    if(Math.abs(moveX) < mTouchSlop && moveY > mTouchSlop * 3){//如果判断是下滑就下滑处理
                        isBigExit=true;
                        mTouchMode=TOUCH_MODE_EXIT;
                        return ;
                    }
                }*/
                if (translateYValue -vsDefault.translationY > translateYEdgeLen) {
                    translateYValue = vsDefault.translationY +translateYEdgeLen+ (translateYValue - vsDefault.translationY) * 0.12f;
                } else if (translateYValue -vsDefault.translationY <- translateYEdgeLen) {
                    translateYValue = vsDefault.translationY -translateYEdgeLen+ (translateYValue -vsDefault.translationY) * 0.12f;
                }

            }

        } else if ("vertical".equals(imageOrientation)) {
            float translateYEdgeLen = (vsDefault.height *vsTouchDrag.scaleY - mHeight) / 2;
            Log.d(TAG, "translateYEdgeLen:"+translateYEdgeLen);
            if (translateYValue -vsDefault.translationY > translateYEdgeLen) {
                translateYValue = vsDefault.translationY +translateYEdgeLen+ (translateYValue - vsDefault.translationY) * 0.12f;
            } else if (translateYValue -vsDefault.translationY <- translateYEdgeLen) {
                translateYValue = vsDefault.translationY -translateYEdgeLen+ (translateYValue -vsDefault.translationY) * 0.12f;
            }
            if (vsDefault.width * vsTouchDrag.scaleX <= mWidth) {
                isXCanMove = false;
            }
            if (Math.abs(translateYValue - vsDefault.translationY) > translateYEdgeLen + ScreenUtils.dpToPx(getContext(), 1)) {
//                if (Math.abs(moveX) < mTouchSlop && moveY > mTouchSlop * 3) {//如果判断是下滑就下滑处理
//                    isBigExit=true;
//                    mTouchMode = TOUCH_MODE_EXIT;
//                    return;
//                }
                if (Math.abs(moveY) < mTouchSlop &&  Math.abs(moveX) > mTouchSlop * 3){//小于屏幕是还是让左右滑动
                    mTouchMode = TOUCH_MODE_SLIDE;
                    return ;
                }
                if (vsDefault.width * vsTouchDrag.scaleX <= mWidth) {
                    isXCanMove = false;
                } else {
                    float translateXRightEdge = vsDefault.width * vsTouchDrag.scaleX / 2 - vsDefault.width / 2;
                    float translateXLeftEdge = mWidth - vsDefault.width * vsTouchDrag.scaleX / 2 - vsDefault.width / 2;

                    if (translateXValue > translateXRightEdge) {
                        translateXValue = translateXRightEdge + (translateXValue - translateXRightEdge) * 0.12f;
                    } else if (translateXValue < translateXLeftEdge) {
                        translateXValue = translateXLeftEdge + (translateXValue - translateXLeftEdge) * 0.12f;
                    }
                }
            }
        }
            Log.d(TAG, "handleDragGesture start");
            if (isXCanMove) {
                iSource.setTranslationX(translateXValue);
            }
            if (isYCanMove) {
                iSource.setTranslationY(translateYValue);
            }


    }



    /**
     * 处理结束下拉退出的手指事件，进行退出图片查看或者恢复到初始状态的收尾动画<br>
     * 还需要还原背景色
     */
    private void handleExitTouchResult() {
        if (iSource == null) return;

        if (mExitScalingRef > 0.9f) {
            ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
            ViewState vsTemp = ViewState.read(iSource, ViewState.STATE_TEMP);
            if (vsDefault == null) return;
            Log.d(TAG,"isBigExit start"+isBigExit);
            if (isBigExit&&vsTemp!=null){
                animSourceViewStateTransform(iSource, vsTemp);
            }else{
                animSourceViewStateTransform(iSource, vsDefault);
            }

            animBackgroundTransform(true);
        } else {
            ViewState vsOrigin = ViewState.read(iSource, ViewState.STATE_ORIGIN);
            if (vsOrigin == null) return;
            if (vsOrigin.alpha == 0)
                vsOrigin.translationX(iSource.getTranslationX()).translationY(iSource.getTranslationY());

            animSourceViewStateTransform(iSource, vsOrigin);
            animBackgroundTransform(false);

            ((FrameLayout) iSource.getParent()).getChildAt(2).animate().alpha(0).start();
        }
    }

    /**
     * 处理结束双击的手指事件，进行图片放大到指定大小或者恢复到初始大小的收尾动画
     */
    private void handleDoubleTapTouchResult() {
        if (iSource == null) return;
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);

        if (vsCurrent.scaleY <= vsDefault.scaleY && vsCurrent.scaleX <= vsDefault.scaleX) {
            final float expectedScale = (MAX_SCALE - vsDefault.scaleX) * 0.4f + vsDefault.scaleX;
            animSourceViewStateTransform(iSource,
                    ViewState.write(iSource, ViewState.STATE_TEMP).scaleX(expectedScale).scaleY(expectedScale));
        } else {
            animSourceViewStateTransform(iSource, vsDefault);
        }
    }

    /**
     * 处理结束缩放旋转模式的手指事件，进行恢复到零旋转角度和大小收缩到正常范围以内的收尾动画<br>
     * 如果是从{@link ImageWatcher#TOUCH_MODE_EXIT}半路转化过来的事件 还需要还原背景色
     */
    private void handleScaleRotateTouchResult() {
        if (iSource == null) return;
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);

        final float endScaleX, endScaleY;
        Log.e("TTT", "AAA  vsCurrent.scaleX :" + vsCurrent.scaleX + "###  vsDefault.scaleX:" + vsDefault.scaleX);
        endScaleX = vsCurrent.scaleX < vsDefault.scaleX ? vsDefault.scaleX : vsCurrent.scaleX;
        endScaleY = vsCurrent.scaleY < vsDefault.scaleY ? vsDefault.scaleY : vsCurrent.scaleY;

        ViewState vsTemp = ViewState.copy(vsDefault, ViewState.STATE_TEMP).scaleX(endScaleX).scaleY(endScaleY);
        iSource.setTag(ViewState.STATE_TEMP, vsTemp);
        animSourceViewStateTransform(iSource, vsTemp);
        animBackgroundTransform(true);
    }

    /**
     * 处理结束拖拽模式的手指事件，进行超过边界则恢复到边界的收尾动画
     */
    private void handleDragTouchResult() {
        if (iSource == null) return;
        ViewState vsDefault = ViewState.read(iSource, ViewState.STATE_DEFAULT);
        if (vsDefault == null) return;
        ViewState vsCurrent = ViewState.write(iSource, ViewState.STATE_CURRENT);
        Boolean isDownTo=false;
        final float endTranslateX, endTranslateY;
        String imageOrientation = (String) iSource.getTag(R.id.image_orientation);
        if ("horizontal".equals(imageOrientation)) {
            float translateXEdge = vsDefault.width * (vsCurrent.scaleX - 1) / 2;
            if (vsCurrent.translationX > translateXEdge) endTranslateX = translateXEdge;
            else if (vsCurrent.translationX < -translateXEdge)
                endTranslateX = -translateXEdge;
            else endTranslateX = vsCurrent.translationX;

            if (vsDefault.height * vsCurrent.scaleY <= mHeight) {
                endTranslateY = vsDefault.translationY;
                Log.d(TAG,"y方向不移动");
            } else {
                float translateYBottomEdge = vsDefault.height * vsCurrent.scaleY / 2 - vsDefault.height / 2;
                float translateYTopEdge = mHeight - vsDefault.height * vsCurrent.scaleY / 2 - vsDefault.height / 2;
                Log.d(TAG,"translateYBottomEdge :"+translateYBottomEdge);
                Log.d(TAG,"translateYTopEdge :"+translateYTopEdge);
                if (vsCurrent.translationY > translateYBottomEdge)
                    endTranslateY = translateYBottomEdge;
                else if (vsCurrent.translationY < translateYTopEdge)
                    endTranslateY = translateYTopEdge;
                else endTranslateY = vsCurrent.translationY;
            }
            Log.d(TAG,"vsDefault.translationY :"+vsDefault.translationY);
            Log.d(TAG,"vsCurrent.translationY :"+vsCurrent.translationY);
            Log.d(TAG,"endTranslateY :"+endTranslateY);
        } else if ("vertical".equals(imageOrientation)) {
            float translateYEdge = vsDefault.height * (vsCurrent.scaleY - 1) / 2;
            if (vsCurrent.translationY > translateYEdge) endTranslateY = translateYEdge;
            else if (vsCurrent.translationY < -translateYEdge)
                endTranslateY = -translateYEdge;
            else endTranslateY = vsCurrent.translationY;

            if (vsDefault.width * vsCurrent.scaleX <= mWidth) {
                endTranslateX = vsDefault.translationX;
                Log.d(TAG,"x方向不移动");
            } else {
                float translateXRightEdge = vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;
                float translateXLeftEdge = mWidth - vsDefault.width * vsCurrent.scaleX / 2 - vsDefault.width / 2;
                Log.d(TAG,"translateXRightEdge :"+translateXRightEdge);
                Log.d(TAG,"translateXLeftEdge :"+translateXLeftEdge);
                if (vsCurrent.translationX > translateXRightEdge)
                    endTranslateX = translateXRightEdge;
                else if (vsCurrent.translationX < translateXLeftEdge)
                    endTranslateX = translateXLeftEdge;
                else endTranslateX = vsCurrent.translationX;
            }
            Log.d(TAG," vsDefault.translationX :"+ vsDefault.translationX);
            Log.d(TAG,"vsCurrent.translationX :"+vsCurrent.translationX);
            Log.d(TAG,"endTranslateX :"+endTranslateX);
        } else {
            return;
        }
        if (vsCurrent.translationX == endTranslateX && vsCurrent.translationY == endTranslateY) {
            return;// 如果没有变化跳过动画实行时间的触摸锁定
        }
        Log.d(TAG,"handleDragTouchResult start");
//        if (isDownTo&&(vsCurrent.translationY-vsDefault.height * vsCurrent.scaleY / 2 +vsDefault.height / 2>ScreenUtils.dpToPx(getContext(),60))){
//            animSourceViewStateTransform(iSource,
//                    ViewState.read(iSource,ViewState.STATE_ORIGIN));
//            animBackgroundTransform(false);
//        }else{
            animSourceViewStateTransform(iSource,
                    ViewState.write(iSource, ViewState.STATE_TEMP).translationX(endTranslateX).translationY(endTranslateY));
//        }

    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mPagerPositionOffsetPixels = positionOffsetPixels;
    }

    /**
     * 每当ViewPager滑动到新的一页后，此方法会被触发<br/>
     * 此刻必不可少的需要同步更新顶部索引，还原前一项后一项的状态等
     */
    @Override
    public void onPageSelected(int position) {
        iSource = adapter.mImageSparseArray.get(position);
        if (iOrigin != null) {
            iOrigin.setVisibility(View.VISIBLE);
        }
        int pos=orginImageViewPosition-initPosition+position;
        if (pos<mImageGroupList.size()&&pos>=0){
            iOrigin = mImageGroupList.get(pos);
            if (iOrigin.getDrawable() != null) iOrigin.setVisibility(View.INVISIBLE);
        }
        refreshCurrentIdx(position);

        ImageView mLast = adapter.mImageSparseArray.get(position - 1);
        if (ViewState.read(mLast, ViewState.STATE_DEFAULT) != null) {
            ViewState.restoreByAnim(mLast, ViewState.STATE_DEFAULT).create().start();
        }
        ImageView mNext = adapter.mImageSparseArray.get(position + 1);
        if (ViewState.read(mNext, ViewState.STATE_DEFAULT) != null) {
            ViewState.restoreByAnim(mNext, ViewState.STATE_DEFAULT).create().start();
        }
    }


    @Override
    public void onPageScrollStateChanged(int state) {
    }

    class ImagePagerAdapter extends PagerAdapter {
        private final LayoutParams lpCenter = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        private final SparseArray<ImageView> mImageSparseArray = new SparseArray<>();
        private boolean hasPlayBeginAnimation;

        @Override
        public int getCount() {
            return mUrlList != null ? mUrlList.size() : 0;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
            mImageSparseArray.remove(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            FrameLayout itemView = new FrameLayout(container.getContext());
            container.addView(itemView);
            ImageView imageView = new ImageView(container.getContext());
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            itemView.addView(imageView);
            mImageSparseArray.put(position, imageView);

            LottieAnimationView loadView = new LottieAnimationView(container.getContext());
            loadView.setAnimation("image_loading_anim.json");
            loadView.setProgress(0.0f);
            loadView.setRepeatCount(0);
            LayoutParams   loadlpCenter=    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            loadlpCenter.gravity = Gravity.CENTER;
            loadView.setLayoutParams(loadlpCenter);
            loadView.setTag(ID_LOADING_VIEW);
            itemView.addView(loadView);
            loadView.setVisibility(View.GONE);

            TextView errorView = new TextView(container.getContext());
            LayoutParams   errorlpCenter=    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            errorlpCenter.gravity = Gravity.CENTER;
            errorView.setLayoutParams(errorlpCenter);
            errorView.setText("图片加载失败");
            errorView.setGravity(Gravity.CENTER);
            errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP,12);
            errorView.setTextColor(Color.parseColor("#FFFFFF"));
            errorView.setTag(ID_ERROR_VIEW);
            Drawable img = getResources().getDrawable(mErrorImageRes);
    // 调用setCompoundDrawables时，必须调用Drawable.setBounds()方法,否则图片不显示
            img.setBounds(0, 0, img.getMinimumWidth(), img.getMinimumHeight());
            errorView.setCompoundDrawablePadding((int) ScreenUtils.dpToPx(getContext(),23.0f));
            errorView.setCompoundDrawables(null, img, null, null); //设置左图标
            itemView.addView(errorView);
            errorView.setVisibility(View.GONE);

            if (setDefaultDisplayConfigs(imageView, position, hasPlayBeginAnimation)) {
                hasPlayBeginAnimation = true;
            }
            return itemView;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        /**
         * 更新ViewPager中每项的当前状态，比如是否加载，比如是否加载失败
         *
         * @param position 当前项的位置
         * @param loading  是否显示加载中
         * @param error    是否显示加载失败
         */
        void notifyItemChangedState(int position, boolean loading, boolean error) {
            ImageView imageView = mImageSparseArray.get(position);
            if (imageView != null) {
                FrameLayout itemView = (FrameLayout) imageView.getParent();
                LottieAnimationView loadView = (LottieAnimationView) itemView.findViewWithTag(ID_LOADING_VIEW);
                float progress=loadView.getProgress();
                if (loading&&!isFloatEaqul(progress,1.0f)) {
                    loadView.setVisibility(View.VISIBLE);
                } else {
                    loadView.setVisibility(View.GONE);
                }

                TextView errorView = (TextView) itemView.findViewWithTag(ID_ERROR_VIEW);
                errorView.setAlpha(1f);
                if (error){
                    loadView.setVisibility(View.GONE);
                    bigPicLoadResult.put(position,BIG_PIC_LOAD_FAILED);
                    errorView.setVisibility(error ? View.VISIBLE : View.GONE);
                }

            }
        }

        private void notifyItemProgress(int progress,int position) {
            ImageView imageView = mImageSparseArray.get(position);
            if (imageView != null) {
                FrameLayout itemView = (FrameLayout) imageView.getParent();
                LottieAnimationView loadView = (LottieAnimationView) itemView.findViewWithTag(ID_LOADING_VIEW);
                loadView.setVisibility(View.VISIBLE);
                loadView.setProgress(progress/100.0f);
            }


        }

        private boolean setDefaultDisplayConfigs(final ImageView imageView, final int pos1, boolean hasPlayBeginAnimation) {
            boolean isFindEnterImagePicture = false;
            final int pos=orginImageViewPosition-initPosition+pos1;//获取imageGroup的位置
             if (pos>=mImageGroupList.size()||pos<0){
                 ImageView originRef = mImageGroupList.get(mImageGroupList.size()-1);
                 int[] location = new int[2];
                 originRef.getLocationOnScreen(location);
                 imageView.setTranslationX(location[0]);
                 int locationYOfFullScreen = location[1];
                 locationYOfFullScreen -= mStatusBarHeight;
                 imageView.setTranslationY(locationYOfFullScreen);
                 imageView.getLayoutParams().width = originRef.getWidth();
                 imageView.getLayoutParams().height = originRef.getHeight();
                 ViewState.write(imageView, ViewState.STATE_ORIGIN).width(originRef.getWidth()).height(originRef.getHeight());
             }
            if (pos < mImageGroupList.size()&&pos>=0) {
                ImageView originRef = mImageGroupList.get(pos);
                if (pos1 == initPosition && !hasPlayBeginAnimation) {
                    isFindEnterImagePicture = true;
                    iSource = imageView;
                    iOrigin = originRef;
                }

                int[] location = new int[2];
                originRef.getLocationOnScreen(location);
                imageView.setTranslationX(location[0]);
                int locationYOfFullScreen = location[1];
                locationYOfFullScreen -= mStatusBarHeight;
                imageView.setTranslationY(locationYOfFullScreen);
                imageView.getLayoutParams().width = originRef.getWidth();
                imageView.getLayoutParams().height = originRef.getHeight();

                ViewState.write(imageView, ViewState.STATE_ORIGIN).width(originRef.getWidth()).height(originRef.getHeight());

                Drawable bmpMirror = originRef.getDrawable();

                if (bmpMirror != null) {
                    int bmpMirrorWidth = bmpMirror.getBounds().width();
                    int bmpMirrorHeight = bmpMirror.getBounds().height();
                    float scale=mWidth/(1.0f*bmpMirrorWidth)>mHeight/(1.0f*bmpMirrorHeight)?mHeight/(1.0f*bmpMirrorHeight):mWidth/(1.0f*bmpMirrorWidth);
                    ViewState vsThumb = ViewState.write(imageView, ViewState.STATE_THUMB).width((int)(bmpMirrorWidth*scale)).height((int)(bmpMirrorHeight*scale))
                            .translationX((mWidth - (int)(bmpMirrorWidth*scale)) / 2).translationY((mHeight - (int)(bmpMirrorHeight*scale)) / 2).imgPos(pos1);
                    imageView.setImageDrawable(bmpMirror);

                    if (isFindEnterImagePicture) {
                        Log.d(TAG,pos1+"==thum orgin-mWidth:"+bmpMirrorWidth+";orgin-mHeight:"+bmpMirrorHeight);
                        Log.d(TAG,pos1+"==thum with:"+(int)(bmpMirrorWidth*scale)+";height:"+(int)(bmpMirrorHeight*scale));
                        animSourceViewStateTransform(imageView, vsThumb);
                    } else {
                        ViewState.restore(imageView, vsThumb.mTag);
                    }
                }
            }
            Log.d(TAG,"==***animImageTransform next code");
            final boolean isPlayEnterAnimation = isFindEnterImagePicture;
            // loadHighDefinitionPicture
            ViewState.clear(imageView, ViewState.STATE_DEFAULT);

            loader.load(imageView.getContext(), mUrlList.get(pos1), new LoadCallback() {
                @Override
                public void onResourceReady(Bitmap resource) {
                    resource=    dealResSize(resource);
                    final int sourceDefaultWidth, sourceDefaultHeight, sourceDefaultTranslateX, sourceDefaultTranslateY;
                    int resourceImageWidth = resource.getWidth();
                    int resourceImageHeight = resource.getHeight();
                    if (resourceImageWidth * 1f / mWidth > resourceImageHeight* 1f / mHeight) {
                        Log.d(TAG,"imageView source is horizontal");
                        sourceDefaultWidth = mWidth;
                        sourceDefaultHeight = (int) (sourceDefaultWidth * 1f / resourceImageWidth * resourceImageHeight);
                        sourceDefaultTranslateX = 0;
//                        sourceDefaultTranslateY=0;
                        sourceDefaultTranslateY = (mHeight - sourceDefaultHeight) / 2;
//                        imageView.setTranslationY((mWidth-imageView.getDrawable().getBounds().height())/2);
                        imageView.setTag(R.id.image_orientation, "horizontal");
                    } else {
                        sourceDefaultHeight = mHeight;
                        sourceDefaultWidth = (int) (sourceDefaultHeight * 1f / resourceImageHeight * resourceImageWidth);
                        sourceDefaultTranslateY = 0;
//                        sourceDefaultTranslateX=0;
                        sourceDefaultTranslateX = (mWidth - sourceDefaultWidth) / 2;
                        imageView.setTag(R.id.image_orientation, "vertical");
                    }
                    bigPicLoadResult.put(pos1,BIG_PIC_LOAD_SUCC);
                    notifyItemChangedState(pos1, false, false);
                    Log.d(TAG,pos1+"==watcher-onResourceReady");
                    ViewState vsDefault = ViewState.write(imageView, ViewState.STATE_DEFAULT).width(sourceDefaultWidth).height(sourceDefaultHeight)
                            .translationX(sourceDefaultTranslateX).translationY(sourceDefaultTranslateY);
                    if (isPlayEnterAnimation) {
                        Message msg=Message.obtain();
                        msg.what=MSG_IS_REASURCE_READY;
                        msg.obj=resource;
                        mAminhander.sendMessage(msg);
//                        animSourceViewStateTransform(imageView, vsDefault);
                    } else {
                        imageView.setImageDrawable( new BitmapDrawable(getResources(), resource));
                        ViewState.restore(imageView, vsDefault.mTag);
                        imageView.setAlpha(0f);
                        imageView.animate().alpha(1).start();
                    }
                }

                @Override
                public void onLoadStarted(Drawable placeholder) {
                    if (bigPicLoadResult.get(pos1)!=BIG_PIC_LOAD_SUCC){
                        notifyItemChangedState(pos1, true, false);
                    }

                    Log.d(TAG,"==watcher-onLoadStarted");
                }

                @Override
                public void onLoadFailed(Drawable errorDrawable) {
                    notifyItemChangedState(pos1, false, true);
                    imageView.setAlpha(0f);
                    Log.d(TAG,"==watcher-onLoadFailed");
                }

                @Override
                public void onUpdateProgress(int progress) {
//                    Log.d(TAG,"==watcher-progress"+progress);
                    notifyItemProgress(progress,pos);
                }
            });

            if (isPlayEnterAnimation) {
                iOrigin.setVisibility(View.INVISIBLE);
                animBackgroundTransform(true);
            }
            return isPlayEnterAnimation;
        }

    }

    private Bitmap dealResSize(Bitmap resource) {
        int resourceImageWidth = resource.getWidth();
        int resourceImageHeight = resource.getHeight();

      if (maxBitmapSize[0]<=0){
          return resource;
      }
        if (resourceImageWidth<=maxBitmapSize[0]&&resourceImageHeight<=maxBitmapSize[0]){
                 return resource;
        }else{
            double scaleW=resourceImageWidth*1.0/maxBitmapSize[0];
            double scaleH=resourceImageHeight*1.0/maxBitmapSize[0];
            double scale=scaleW>scaleH?scaleW:scaleH;
            Matrix matrix = new Matrix();
            matrix.postScale((float)(1/scale), (float)(1/scale));
          // 得到新的图片
            resource = Bitmap.createBitmap(resource, 0, 0, resourceImageWidth, resourceImageHeight, matrix, true);
            return resource;
        }
    }

    private void getGLESTextureLimitBelowLollipop( ) {
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxBitmapSize, 0);

    }

    private void getGLESTextureLimitEqualAboveLollipop() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] vers = new int[2];
        egl.eglInitialize(dpy, vers);
        int[] configAttr = {
                EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
                EGL10.EGL_LEVEL, 0,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(dpy, configAttr, configs, 1, numConfig);
        if (numConfig[0] == 0) {// TROUBLE! No config found.
        }
        EGLConfig config = configs[0];
        int[] surfAttr = {
                EGL10.EGL_WIDTH, 64,
                EGL10.EGL_HEIGHT, 64,
                EGL10.EGL_NONE
        };
        EGLSurface surf = egl.eglCreatePbufferSurface(dpy, config, surfAttr);
        final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;  // missing in EGL10
        int[] ctxAttrib = {
                EGL_CONTEXT_CLIENT_VERSION, 1,
                EGL10.EGL_NONE
        };
        EGLContext ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, ctxAttrib);
        egl.eglMakeCurrent(dpy, surf, surf, ctx);

        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxBitmapSize, 0);
        egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(dpy, surf);
        egl.eglDestroyContext(dpy, ctx);
        egl.eglTerminate(dpy);


    }

    private static class GestureHandler extends Handler {
        WeakReference<ImageWatcher> mRef;

        GestureHandler(ImageWatcher ref) {
            mRef = new WeakReference<>(ref);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mRef.get() != null) {
                ImageWatcher holder = mRef.get();
                switch (msg.what) {
                    case SINGLE_TAP_UP_CONFIRMED:
                        holder.onSingleTapConfirmed();
                        break;
                    default:
                        throw new RuntimeException("Unknown message " + msg); //never
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mPagerPositionOffsetPixels == 0;
    }

    /**
     * 动画执行时加入这个监听器后会自动记录标记 {@link ImageWatcher#isInTransformAnimation} 的状态<br/>
     * isInTransformAnimation值为true的时候可以达到在动画执行时屏蔽触摸操作的目的
     */
    final AnimatorListenerAdapter mAnimTransitionStateListener = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationCancel(Animator animation) {
            isInTransformAnimation = false;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            isInTransformAnimation = true;
            mTouchMode = TOUCH_MODE_AUTO_FLING;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            isInTransformAnimation = false;
        }
    };

    final TypeEvaluator<Integer> mColorEvaluator = new TypeEvaluator<Integer>() {
        @Override
        public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
            int startColor = startValue;
            int endColor = endValue;

            int alpha = (int) (Color.alpha(startColor) + fraction * (Color.alpha(endColor) - Color.alpha(startColor)));
            int red = (int) (Color.red(startColor) + fraction * (Color.red(endColor) - Color.red(startColor)));
            int green = (int) (Color.green(startColor) + fraction * (Color.green(endColor) - Color.green(startColor)));
            int blue = (int) (Color.blue(startColor) + fraction * (Color.blue(endColor) - Color.blue(startColor)));
            return Color.argb(alpha, red, green, blue);
        }
    };

    public void setTranslucentStatus(int statusBarHeight) {
        mStatusBarHeight = statusBarHeight;
        tCurrentIdx.setTranslationY(tCurrentIdxTransY - statusBarHeight);
    }

    public void setErrorImageRes(int resErrorImage) {
        mErrorImageRes = resErrorImage;
    }

    private void refreshCurrentIdx(int position) {
        if (mUrlList.size() > 1) {
            tCurrentIdx.setVisibility(View.VISIBLE);
            tCurrentIdx.setText((position + 1) + " / " + mUrlList.size());
        } else {
            tCurrentIdx.setVisibility(View.GONE);
        }
    }

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        super.setBackgroundColor(color);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        maxTranslateX = mWidth / 2;
        maxTranslateY = mHeight / 2;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animImageTransform != null) animImageTransform.cancel();
        animImageTransform = null;
        if (animBackground != null) animBackground.cancel();
        animBackground = null;
    }

    /**
     * 当界面处于图片查看状态需要在Activity中的{@link Activity#onBackPressed()}
     * 将事件传递给ImageWatcher优先处理<br/>
     * 1、当处于收尾动画执行状态时，消费返回键事件<br/>
     * 2、当图片处于放大状态时，执行图片缩放到原始大小的动画，消费返回键事件<br/>
     * 3、当图片处于原始状态时，退出图片查看，消费返回键事件<br/>
     * 4、其他情况，ImageWatcher并没有展示图片
     */
    public boolean handleBackPressed() {
        return isInTransformAnimation || (iSource != null && getVisibility() == View.VISIBLE && onSingleTapConfirmed());
    }


    /**
     * 将指定的ImageView形态(尺寸大小，缩放，旋转，平移，透明度)逐步转化到期望值
     */
    private void animSourceViewStateTransform(final ImageView view, final ViewState vsResult) {
        if (view == null) return;
        if (animImageTransform != null) animImageTransform.cancel();

        animImageTransform = ViewState.restoreByAnim( view, vsResult.mTag).addListener(mAnimTransitionStateListener).create();

        if (animImageTransform != null) {
            if (vsResult.mTag == ViewState.STATE_ORIGIN) {
                animImageTransform.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // 如果是退出查看操作，动画执行完后，原始被点击的ImageView恢复可见
                        if (iOrigin != null){
                            iOrigin.setAlpha(1.0f);
                            iOrigin.setVisibility(View.VISIBLE);
                        }
                        setVisibility(View.GONE);
                        mAminhander.clean();
                    }
                });
            }
            if (vsResult.mTag == ViewState.STATE_THUMB) {
                animImageTransform.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        Log.d(TAG,"STATE_THUMB==***animImageTransform end");
                        // 加载失败
                        if (bigPicLoadResult.get(vsResult.getImgPos())==BIG_PIC_LOAD_FAILED){
                            Log.d(TAG," ViewState.STATE_THUMB end");
                            if (view != null) view.setAlpha(0f);
                        }
                        Message msg=Message.obtain();
                        msg.what=MSG_IS_THUM_AMIN_END;
                        msg.obj=view;
                        mAminhander.handleMessage(msg);
                    }
                });
            }
            animImageTransform.start();
            Log.d(TAG,vsResult.mTag+"==animImageTransform start");
        }
    }

    /**
     * 执行ImageWatcher自身的背景色渐变至期望值[colorResult]的动画
     * @param  isVisviable 是否是背景显现的动画 如果不是则是消失的动画
     *                     (Math.abs(backGroudAlpha-1)>0.5) 不透明
     */
    private void animBackgroundTransform(final boolean isVisviable) {
       if ((Math.abs(backGroudAlpha-1)<0.5)^isVisviable){
       }else{
           return ;
       }

        if (animBackground != null) animBackground.cancel();
//        final int mCurrentBackgroundColor = mBackgroundColor;
//        Blurry.with(getContext()).radius(25).sampling(2).onto((ViewGroup) getParent());
//        setBackgroundResource(R.drawable.image_view_error);
        animBackground = ValueAnimator.ofFloat(0, 1).setDuration(300);

        animBackground.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float p = (float) animation.getAnimatedValue();
                if (!isVisviable){
                    p=1-p;
                }
                backGroudAlpha=p;
                setAlpha(p);
                grayView.setAlpha(p);
//                setBackgroundColor(mColorEvaluator.evaluate(p, mCurrentBackgroundColor, colorResult));
            }
        });
        animBackground.start();
    }

    /**
     * 当前展示图片长按的回调
     */
    public interface OnPictureLongPressListener {
        /**
         * @param v   当前被按的ImageView
         * @param url 当前ImageView加载展示的图片url地址
         * @param pos 当前ImageView在展示组中的位置
         */
        void onPictureLongPress(ImageView v, String url, int pos);
    }

    public void setOnPictureLongPressListener(OnPictureLongPressListener listener) {
        mPictureLongPressListener = listener;
    }

    public static class Helper {
        public static final int VIEW_IMAGE_WATCHER_ID = R.id.view_image_watcher;
        private final ViewGroup activityDecorView;
        private final ImageWatcher mImageWatcher;

        private Helper(Activity activity) {
            mImageWatcher = new ImageWatcher(activity);
            mImageWatcher.setId(VIEW_IMAGE_WATCHER_ID);
            activityDecorView = (ViewGroup) activity.getWindow().getDecorView();
        }

        public static Helper with(Activity activity) {
            return new Helper(activity);
        }

        public Helper setLoader(Loader l) {
            mImageWatcher.setLoader(l);
            return this;
        }

        public Helper setTranslucentStatus(int statusBarHeight) {
            mImageWatcher.mStatusBarHeight = statusBarHeight;
            return this;
        }

        public Helper setErrorImageRes(int resErrorImage) {
            mImageWatcher.mErrorImageRes = resErrorImage;
            return this;
        }

        public Helper setOnPictureLongPressListener(OnPictureLongPressListener listener) {
            mImageWatcher.setOnPictureLongPressListener(listener);
            return this;
        }

        public ImageWatcher create() {
            removeExistingOverlayInView(activityDecorView);
            mImageWatcher.setAnimParam(activityDecorView);
            activityDecorView.addView(mImageWatcher);
            return mImageWatcher;
        }

        void removeExistingOverlayInView(ViewGroup parent) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child.getId() == VIEW_IMAGE_WATCHER_ID) {
                    parent.removeView(child);
                }
                if (child instanceof ViewGroup) {
                    removeExistingOverlayInView((ViewGroup) child);
                }
            }
        }
    }

  private boolean  isFloatEaqul(float a,float b){
        return Math.abs(a-b)<0.00000001;
  }

    public static Bitmap loadBitmapFromViewBySystem(View v) {
        if (v == null) {
            return null;
        }

        v.setDrawingCacheEnabled(true);
        v.buildDrawingCache();
        Bitmap bitmap = v.getDrawingCache();
        Bitmap  tBitmap = bitmap.createBitmap(bitmap);
        v.setDrawingCacheEnabled(false);
        return tBitmap;
    }

    public  void setFrameBackground( Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }


}
