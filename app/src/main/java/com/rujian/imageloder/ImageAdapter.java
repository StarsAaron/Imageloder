package com.rujian.imageloder;

import java.io.IOException;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.rujian.imageloderlibrary.ImageLoder;

public class ImageAdapter extends BaseAdapter implements OnScrollListener {
    /**
     * 上下文对象的引用
     */
    private Context context;

    /**
     * Image Url的数组
     */
    private String[] imageThumbUrls;

    /**
     * GridView对象的应用
     */
    private GridView mGridView;

    /**
     * Image 下载器
     */
    private ImageLoder imageLoder;

    /**
     * 记录是否刚打开程序，用于解决进入程序不滚动屏幕，不会下载图片的问题。
     * 参考http://blog.csdn.net/guolin_blog/article/details/9526203#comments
     */
    private boolean isFirstEnter = true;

    /**
     * 一屏中第一个item的位置
     */
    private int mFirstVisibleItem;

    /**
     * 一屏中所有item的个数
     */
    private int mVisibleItemCount;

    private ImageView mImageView = null;


    public ImageAdapter(Context context, GridView mGridView, String[] imageThumbUrls) {
        this.context = context;
        this.mGridView = mGridView;
        this.imageThumbUrls = imageThumbUrls;
        imageLoder = ImageLoder.getInstance(context);
        mGridView.setOnScrollListener(this);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        //仅当GridView静止时才去下载图片，GridView滑动时取消所有正在下载的任务
        if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
            showImage(mFirstVisibleItem, mVisibleItemCount);
        } else {
            cancelTask();
        }

    }


    /**
     * GridView滚动的时候调用的方法，刚开始显示GridView也会调用此方法
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {
        mFirstVisibleItem = firstVisibleItem;
        mVisibleItemCount = visibleItemCount;
        // 因此在这里为首次进入程序开启下载任务。
        if (isFirstEnter && visibleItemCount > 0) {
            showImage(mFirstVisibleItem, mVisibleItemCount);
            isFirstEnter = false;
        }
    }


    @Override
    public int getCount() {
        return imageThumbUrls.length;
    }

    @Override
    public Object getItem(int position) {
        return imageThumbUrls[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final String mImageUrl = imageThumbUrls[position];
        if (convertView == null) {
            mImageView = new ImageView(context);
            mImageView.setLayoutParams(new GridView.LayoutParams(150, 150));
            mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        } else {
            mImageView = (ImageView) convertView;
        }
        // 给ImageView设置Tag,这里已经是司空见惯了
        mImageView.setTag(mImageUrl);
        mImageView.setBackground(context.getResources().getDrawable(R.drawable.ic_empty));
//		imageLoder.load(mImageUrl,mImageView);
        return mImageView;
    }

    /**
     * 显示当前屏幕的图片，先会去查找LruCache，LruCache没有就去sd卡或者手机目录查找，在没有就开启线程去下载
     *
     * @param firstVisibleItem
     * @param visibleItemCount
     */
    private void showImage(int firstVisibleItem, int visibleItemCount) {
        for (int i = firstVisibleItem; i < firstVisibleItem + visibleItemCount; i++) {
            String mImageUrl = imageThumbUrls[i];
            final ImageView mImageView = (ImageView) mGridView.findViewWithTag(mImageUrl);
            imageLoder.load(mImageUrl, mImageView);
        }
    }

    /**
     * 取消下载任务
     */
    public void cancelTask() {
        imageLoder.cancelTask();
    }

    public void flush() {
        try {
            imageLoder.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delAll() throws IOException {
        imageLoder.delAllCache();
    }
}
