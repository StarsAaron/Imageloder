package com.rujian.imageloderlibrary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

/**
 * 文件操作工具类
 *
 * @author zhrjian
 */
public class BitmapFileCacheUtil {
    //SD卡根路径
    private static String mSdRootPath = null;
    //手机内存根目录
    private static String mDataRootPath = null;
    //图片缓存文件夹名称
    private String cacheDir = "/ImageCache";

    public BitmapFileCacheUtil(Context context) {
        //初始化获取手机缓存目录路径
        mDataRootPath = context.getCacheDir().getPath();
        mSdRootPath = context.getExternalCacheDir().getPath();//手机内存应用的缓存目录
//		mSdRootPath = Environment.getExternalStorageDirectory().getPath(); //手机内存根目录
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    /**
     * 获取缓存目录路径
     *
     * @return
     */
    public String getCacheStoragePath() {
        //优先使用SD卡
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ?
                mSdRootPath + cacheDir : mDataRootPath + cacheDir;
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
     * 保存 bitmap 到文件
     *
     * @param fileName 带后缀的文件名
     * @param bitmap
     * @param format   图片格式 png jpg webp
     * @param quality  图片质量 Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality. Some formats, like PNG which is lossless, will ignore the quality setting
     * @throws IOException
     */
    public void saveBitmapToFile(String fileName, Bitmap bitmap, CompressFormat format, int quality) throws IOException {
        if (bitmap == null) {
            return;
        }
        String savePath = getCacheStoragePath();
        File fileDir = new File(savePath);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        File saveFile = new File(savePath, fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
        bitmap.compress(format, quality, fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();//关闭输出流
    }

    /**
     * 保存 bitmap 到文件 (使用默认)
     *
     * @param fileName 带后缀的文件名
     * @param bitmap
     * @throws IOException
     */
    public void saveBitmapToFileDefault(String fileName, Bitmap bitmap) throws IOException {
        saveBitmapToFile(fileName, bitmap, CompressFormat.JPEG, 80);
    }

    /**
     * 从文件中获取Bitmap
     *
     * @param fileName 带后缀的文件名
     * @param opts     BitmapFactory的配置，可以调整Bitmap的大小等
     * @return
     */
    public Bitmap getBitmapFromFile(String fileName, Options opts) {
        Bitmap bitmap = null;
        if (isFileExisted(fileName)) {
            bitmap = BitmapFactory.decodeFile(getCacheStoragePath() + File.separator + fileName, opts);
        }
        return bitmap;
    }

    /**
     * 从文件中获取Bitmap（使用默认BitmapFactory.Options）
     * @param fileName
     * @return
     */
    public Bitmap getBitmapFromFile(String fileName) {
        Log.d("getBitmapFromFile", getCacheStoragePath() + File.separator + fileName);
        Bitmap bitmap = null;
        if (isFileExisted(fileName)) {
            bitmap = BitmapFactory.decodeFile(getCacheStoragePath() + File.separator + fileName);
        }
        return bitmap;
    }

    /**
     * 检测文件是否存在
     * @param fileName
     * @return
     */
    public boolean isFileExisted(String fileName) {
        File file = new File(getCacheStoragePath() + File.separator + fileName);
        return file.exists();
    }

    /**
     * 获取文件大小
     *
     * @param fileName 文件名
     * @return
     */
    public long getFileSize(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            Log.d("FileUtil-getFileSize", "文件名为空");
            return -1;
        }
        File file = new File(getCacheStoragePath() + File.separator + fileName);
        if (!file.exists()) {
            Log.d("FileUtil-getFileSize", "文件不存在");
            return -1;
        }
        if (file.isDirectory()) {
            Log.d("FileUtil-getFileSize", "传入参数应该是文件名不是文件夹");
            return -1;
        } else {
            return file.length();
        }
    }

    /**
     * 删除缓存文件夹的文件
     */
    public boolean deleteFile() {
        File file = new File(getCacheStoragePath());
        int count = 0;
        if (!file.exists()) {
            Log.d("FileUtil-deleteFile", "文件夹不存在");
            return true;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File ff : files) {
                boolean is = ff.delete();//删除文件夹下的子文件
                if (is) {
                    count++;
                    Log.d("FileUtil-deleteFile", "删除了文件：" + ff.getName());
                } else {
                    Log.d("FileUtil-deleteFile", "无法删除文件：" + ff.getName());
                }
            }
            if (count == files.length) {
                Log.d("FileUtil-deleteFile", "全部删除完成");
            } else {
                Log.d("FileUtil-deleteFile", "还有文件没有删除");
            }
        }
        file.delete();
        return false;
    }
}
