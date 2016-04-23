package com.rujian.imageloderlibrary;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.graphics.Bitmap.CompressFormat;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.widget.Toast;

/**
 * 图片加载类
 * 使用 DiskLruCache 和 LruCache 缓存图片数据
 *
 * @author zhrjian
 */
public class ImageLoder {
    private Context context = null;

    /**
     * SAVE_TO_MEMORY 保存图片在内存 SAVE_TO_FILE 保存图片在手机内存文件中 SAVE_TO_BOTH
     * 保存图片在手机内存文件和缓存中
     */
    public enum SaveType {
        SAVE_TO_MEMORY, SAVE_TO_FILE, SAVE_TO_BOTH
    }

    // 缓存Image的类，当存储Image的大小大于LruCache设定的值，系统自动释放内存
    private LruCache<String, Bitmap> mMemoryCache = null;
    //
    private DiskLruCache diskLruCache = null;
    private Config config = null;// 设置参数类
    private ExecutorService mImageThreadPool = null; // 下载Image的线程池
    // SD卡根路径
    private static String mSdRootPath = null;
    // 手机内存根目录
    private static String mDataRootPath = null;
    // 图片缓存文件夹名称
    private String cacheDir = "/ImageCache";

    private ImageLoder() {
    }

    // 必须先调用该方法初始化一些变量
    private void initImageLoder(Context context) {
        this.context = context;
        config = new Config();
        // 获取系统分配给每个应用程序的最大内存，每个应用系统分配32M
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        config.memCacheSize = maxMemory / 8;
        // 创建 LruCache
        mMemoryCache = new LruCache<String, Bitmap>(config.memCacheSize) {
            // 必须重写此方法，来测量Bitmap的大小
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        // 初始化获取手机缓存目录路径
        mDataRootPath = context.getCacheDir().getPath();
        mSdRootPath = context.getExternalCacheDir().getPath();// 手机内存应用的缓存目录
        // mSdRootPath = Environment.getExternalStorageDirectory().getPath();
        // //手机内存根目录
        // 创建 DiskLruCache
        initDiskLruCache();
    }

    ;

    /**
     * 初始化 DiskLruCache
     */
    private void initDiskLruCache() {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            try {
                diskLruCache = DiskLruCache.open(new File(getCacheStoragePath()), info.versionCode, 1,
                        config.disCacheSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取缓存目录路径
     *
     * @return
     */
    public String getCacheStoragePath() {
        // 优先使用SD卡
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ? mSdRootPath + cacheDir
                : mDataRootPath + cacheDir;
    }

    /**
     * 内部类创建ImageLoder单例
     *
     * @author zhrjian
     */
    private static class Build {
        private static final ImageLoder imageLoder = new ImageLoder();

        private static ImageLoder getObject(Context context) {
            // 初始化 ImageLoder 使用的变量
            imageLoder.initImageLoder(context);
            return imageLoder;
        }
    }

    /**
     * 获取单例
     *
     * @return
     */
    public static ImageLoder getInstance(Context context) {
        return Build.getObject(context);
    }

    /**
     * 图片加载参数类
     *
     * @author zhrjian
     */
    public static class Config {
        public SaveType type = SaveType.SAVE_TO_BOTH;// 保存方式
        public String cacheDir = "/ImageCache"; // 缓存的文件夹名称
        public int imageQuality = 100;// 图片质量 0~100
        public int threadPoolSize = 2;// 下载线程池大小
        public int inSampleSize = 2;// 图片压缩量 1/2
        public CompressFormat compressFormat = CompressFormat.JPEG;// 保存的图片格式
        public int memCacheSize = 2;// 内存缓存空间的大小
        public int disCacheSize = 10 * 1024 * 1024;// 硬盘缓存空间的大小 ，默认10 M
    }

    /**
     * 获取参数
     *
     * @return
     */
    public Config getConfigObject() {
        return config;
    }

    /**
     * 设置参数
     *
     * @param config
     */
    public void initLoderConfig(Config config) {
        this.config = config;
    }

    /**
     * 加载图片
     *
     * @param url
     * @param listener
     */
    public void load(final String url, final ImageLoaderListener listener) {
        Bitmap bitmap = null;
        // 先从本地获取缓存
        bitmap = getBitmapFromLocal(url);
        if (bitmap != null) {
            listener.onLoadSuccessed(bitmap, url);
        } else {
            // 如果没有就从网络下载
            getBitmapFromWeb(url, listener);
        }
    }

    /**
     * 加载图片
     *
     * @param url
     * @param view
     */
    public void load(final String url, final View view) {
        Bitmap bitmap = null;
        // 先从本地获取缓存
        bitmap = getBitmapFromLocal(url);

        final Handler myHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0x123:
                        view.setBackground(new BitmapDrawable(null, (Bitmap) msg.obj));
                        break;
                    case 0x124:
                        Toast.makeText(context, (String) msg.obj, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        if (bitmap != null) {
            view.setBackground(new BitmapDrawable(null, bitmap));
        } else {
            // 如果没有就从网络下载
            getBitmapFromWeb(url, new ImageLoaderListener() {

                @Override
                public void onLoadSuccessed(Bitmap bitmap, String url) {
                    Message msg = myHandler.obtainMessage();
                    msg.obj = bitmap;
                    msg.what = 0x123;
                    myHandler.sendMessage(msg);
                }

                @Override
                public void onLoadFailed(String msg) {
                    Message msg2 = myHandler.obtainMessage();
                    msg2.obj = msg;
                    msg2.what = 0x124;
                    myHandler.sendMessage(msg2);
                }
            });
        }
    }

    /**
     * 使用MD5加密字符串
     *
     * @param name
     * @return
     */
    public String toMd5Name(String name) {
        String cacheKey = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(name.getBytes());
            cacheKey = bytesToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return cacheKey;
    }

    /**
     * 把byte数组转换成16进制的字符串
     *
     * @param bytes
     * @return
     */
    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * 从本地获取 Bitmap 资源
     *
     * @param url
     * @return
     */
    private Bitmap getBitmapFromLocal(String url) {
        Bitmap bitmap = null;
        final String subUrl = toMd5Name(url);
        Log.i("load-http", url);
        // (1)先从内存中获取
        bitmap = getBitmapFromMemCache(subUrl);
        if (bitmap != null) {
            Log.i("load-local", "内存中有");
            return bitmap;
        } else {
            Log.i("load-local", "内存中没有");
        }
        // (2.2)DiskLruCache
        if (diskLruCache.isClosed()) {
            initDiskLruCache();
        }
        DiskLruCache.Snapshot snapShot = null;
        // 查找key对应的缓存
        try {
            snapShot = diskLruCache.get(subUrl);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        if (snapShot != null) {
            FileInputStream fileInputStream = (FileInputStream) snapShot.getInputStream(0);
            bitmap = BitmapFactory.decodeStream(fileInputStream);
            if (bitmap != null) {
                // 将Bitmap 加入内存缓存
                addBitmapToMemoryCache(subUrl, bitmap);
                Log.i("load", "手机存储中有");
            }
        } else {
            Log.i("load", "手机存储中没有");
        }
        return bitmap;
    }

    /**
     * 从网络下载图片
     *
     * @param url
     * @param listener
     */
    private void getBitmapFromWeb(final String url, final ImageLoaderListener listener) {
        final String subUrl = toMd5Name(url);
        getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                HttpURLConnection con = null;
                try {
                    URL mImageUrl = new URL(url);
                    con = (HttpURLConnection) mImageUrl.openConnection();
                    con.setConnectTimeout(10 * 1000);
                    con.setReadTimeout(10 * 1000);
                    con.setDoInput(true);
                    con.setDoOutput(true);
                    bitmap = BitmapFactory.decodeStream(con.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
                if (bitmap != null) {
                    Log.d("从网络下载", "下载成功：" + url);
                    // 将 Bitmap 加入内存缓存 ---------
                    addBitmapToMemoryCache(subUrl, bitmap);
                    // DiskLruCache 写入缓存 ---------
                    try {
                        if (diskLruCache.isClosed()) {
                            initDiskLruCache();
                        }
                        DiskLruCache.Editor editor = diskLruCache.edit(subUrl);
                        if (editor != null) {
                            OutputStream outputStream = editor.newOutputStream(0);
                            bitmap.compress(config.compressFormat, config.imageQuality, outputStream);
                            editor.commit();
                            if (outputStream != null) {
                                outputStream.close();
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    listener.onLoadSuccessed(bitmap, url);
                } else {
                    listener.onLoadFailed("下载失败：" + url);
                    Log.d("从网络下载", "下载失败：" + url);
                }
            }
        });
    }

    /**
     * 取消正在下载的任务
     */
    public synchronized void cancelTask() {
        if (mImageThreadPool != null) {
            mImageThreadPool.shutdownNow();
            mImageThreadPool = null;
        }
    }

    /**
     * 针对DiskLruCache，将内存中的操作记录同步到日志文件journal
     *
     * @throws IOException
     */
    public void flush() throws IOException {
        diskLruCache.flush();
    }

    /**
     * 移除缓存
     *
     * @param key
     * @throws IOException
     */
    public void remove(String key) throws IOException {
        diskLruCache.remove(key);
    }

    /**
     * 关闭缓存
     *
     * @throws IOException
     */
    public void close() throws IOException {
        diskLruCache.flush();
        diskLruCache.close();
    }

    /**
     * 关闭缓存并删除全部缓存文件
     *
     * @throws IOException
     */
    public void delAllCache() throws IOException {
        diskLruCache.delete();
    }

    /**
     * 异步下载图片的回调接口
     *
     * @author zhrjian
     */
    public interface ImageLoaderListener {
        void onLoadSuccessed(Bitmap bitmap, String url);

        void onLoadFailed(String msg);
    }

    /**
     * 获取线程池的方法，因为涉及到并发的问题，我们加上同步锁
     *
     * @return
     */
    private ExecutorService getThreadPool() {
        if (mImageThreadPool == null) {
            synchronized (ExecutorService.class) {
                if (mImageThreadPool == null) {
                    // 为了下载图片更加的流畅，我们用了n个线程来下载图片
                    mImageThreadPool = Executors.newFixedThreadPool(config.threadPoolSize);
                }
            }
        }
        return mImageThreadPool;
    }

    /**
     * 添加Bitmap到内存缓存
     *
     * @param key
     * @param bitmap
     */
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null && bitmap != null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    /**
     * 从内存缓存中获取一个Bitmap
     *
     * @param key
     * @return
     */
    private Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }
}
