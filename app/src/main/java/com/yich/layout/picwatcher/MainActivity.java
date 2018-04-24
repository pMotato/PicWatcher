package com.yich.layout.picwatcher;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.yich.layout.picwatcherlib.PicWatcher;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
   private Button clearBtn;
    private ImageView imageView;
    private RecyclerView mRecycleView;
    private RecyclerView.Adapter mAdapter;
    private static ArrayList<String> thumbnailImageList;
    private static ArrayList<String> sourceImageList;
static
    {
        thumbnailImageList = new ArrayList<>();
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486263782969.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1485055822651.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486194909983.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486194996586.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486195059137.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486173497249.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486173526402.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486173639603.png@233w_160h_20q");
        thumbnailImageList.add("http://static.fdc.com.cn/avatar/sns/1486172566083.png@233w_160h_20q");

        sourceImageList = new ArrayList<>();
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486263782969.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1485055822651.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486194909983.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486194996586.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486195059137.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486173497249.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486173526402.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486173639603.png");
        sourceImageList.add("http://static.fdc.com.cn/avatar/sns/1486172566083.png");
         }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        initListener();
        initView();
    }

    private void initListener() {
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        Glide.get(MainActivity.this).clearDiskCache();
                        Glide.get(MainActivity.this).clearMemory();
                    }
                }.start();

            }
        });
    }

    private void initView() {
        mAdapter=new ImageAdapter(thumbnailImageList,getBaseContext());
        mRecycleView.setLayoutManager( new GridLayoutManager(getBaseContext(), 4));
        mRecycleView.setAdapter(mAdapter);
        Glide.with(MainActivity.this).load(thumbnailImageList.get(2)).into(new SimpleTarget<GlideDrawable>() {
            @Override
            public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                imageView.setImageDrawable(resource);
            }
        });
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //展示单张张图片
               PicWatcher.showSingleImage(MainActivity.this,imageView,"");
            }
        });
    }

    private void findView() {
        clearBtn=(Button) findViewById(R.id.button);
        mRecycleView=(RecyclerView) findViewById(R.id.recyclerView);
        imageView=(ImageView) findViewById(R.id.imageView2);
    }

    class  ImageAdapter extends  RecyclerView.Adapter<RecyclerView.ViewHolder>{
    private  List mDatas;
    private Context con;
       public  ImageAdapter(List data,Context con){
           mDatas=data;
           this.con=con;
       }
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            MyViewHolder holder=new MyViewHolder(LayoutInflater.from(getBaseContext()).inflate(R.layout.list_item,parent,false));
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
          final   MyViewHolder myHolder=(MyViewHolder)holder;
            Glide.with(MainActivity.this).load(mDatas.get(position)).into(new SimpleTarget<GlideDrawable>() {
                @Override
                public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                    myHolder.picIv.setImageDrawable(resource);
                }
            });
            myHolder.picIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //展示多张图片
                    PicWatcher.showImages(MainActivity.this,(ImageView)v,position,findRecelyVisiableImageviews(mRecycleView),sourceImageList);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }
    }

public  List<ImageView>  findRecelyVisiableImageviews(RecyclerView list){
    ArrayList<ImageView> imageViews=new ArrayList<>();
    GridLayoutManager layoutManager=(GridLayoutManager)(list.getLayoutManager());
    int fisrtPos=layoutManager.findFirstVisibleItemPosition();
    int lastPos=layoutManager.findLastVisibleItemPosition();
    for (int i=fisrtPos;i<lastPos+1;i++){
        imageViews.add((ImageView) layoutManager.findViewByPosition(i).findViewById(R.id.imageView))  ;
    }
    return imageViews;
}

    class  MyViewHolder extends RecyclerView.ViewHolder{
        public ImageView picIv;
        public MyViewHolder(View itemView) {
            super(itemView);
            picIv=(ImageView) itemView.findViewById(R.id.imageView);
        }
    }


}
