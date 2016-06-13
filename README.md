# Imageloder
图片下载缓存库

    这个是图片下载库，在参考了一些关于缓存技术的博客之后做出来的总结，
比如博客：http://blog.csdn.net/xiaanming/article/details/9825113 等感谢博主们的知识分享，让我受益匪浅。
    本库只是知识性的总结，希望给想了解这方面知识的朋友一个参考，里面有详细的解释。
    这个库使用的技术是DiskLruCache 和 LruCache 缓存，从网络地址下载图片，保存在内存中也同时以文件的形式保
存在本地，优先查找本地的资源如果找不到就会从网络获取再缓存到本地。

使用的步骤也很简单
（1）获取图片加载类
ImageLoder imageLoder = ImageLoder.getInstance(context);
（2）获取图片
有两个加载图片的方法，只是传入参数不同
url ：图片的网络地址
listener：图片加载回调
view：需要显示图片的控件
public void load(String url, ImageLoaderListener listener) 
public void load(String url, View view) 

也可以自己自定义一些参数传给ImageLoder
通过 ImageLoder 的 getConfigObject() 方法可以获取当前的配置参数，通过initLoderConfig(Config config) 方法设置参数。
可以设置的参数有：
SaveType type = SaveType.SAVE_TO_BOTH;// 保存方式
String cacheDir = "/ImageCache"; // 缓存的文件夹名称
int imageQuality = 100;// 图片质量 0~100
int threadPoolSize = 2;// 下载线程池大小
int inSampleSize = 2;// 图片压缩量 1/2
CompressFormat compressFormat = CompressFormat.JPEG;// 保存的图片格式
int memCacheSize = 2;// 内存缓存空间的大小
int disCacheSize = 10 * 1024 * 1024;// 硬盘缓存空间的大小 ，默认10 M
