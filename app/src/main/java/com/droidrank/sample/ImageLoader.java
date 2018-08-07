package com.droidrank.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by PerryGarg on 05-08-2018.
 */

public class ImageLoader {

    private static final int CONNECTION_TIMEOUT = 3000;
    private static final int REQUIRED_SIZE = 70;
    MemoryCache memoryCache = new MemoryCache();
    FileCache fileCache;
    private Map<String, PhotoToLoad> imageViews = Collections.synchronizedMap(new WeakHashMap<String, PhotoToLoad>());
    ExecutorService executorService;
    private ImageCallback imageCallback;

    public ImageLoader(Context context, ImageCallback imageCallback) {
        this.imageCallback = imageCallback;
        fileCache = new FileCache(context);
        executorService = Executors.newFixedThreadPool(5);
    }

    final int stub_id = R.mipmap.ic_launcher;

    public void displayBitmap(String url, Integer imgCount) {

        Bitmap bitmap = memoryCache.get(url);
        if (bitmap != null){
            if(imageCallback != null)
                imageCallback.updatePhoto(bitmap, imgCount);
        }
        else {
            PhotoToLoad photoToLoad = imageViews.get(url);
            if (photoToLoad == null || !photoToLoad.status) {
                if (photoToLoad == null)
                    photoToLoad = new PhotoToLoad(url, imgCount);

                photoToLoad.status = true;
                imageViews.put(url, photoToLoad);
                queuePhoto(url);
            }
        }
    }

    private void queuePhoto(String url) {
        executorService.submit(new PhotosLoader(imageViews.get(url)));
    }

    private Bitmap getBitmap(String url) {
        File f = fileCache.getFile(url);

        //from SD cache
        Bitmap b = decodeFile(f);
        if (b != null)
            return b;

        //from web
        try {
            Bitmap bitmap = null;
            URL imageUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
            conn.setConnectTimeout(CONNECTION_TIMEOUT);
            conn.setReadTimeout(CONNECTION_TIMEOUT);
            conn.setInstanceFollowRedirects(true);
            InputStream is = conn.getInputStream();
            OutputStream os = new FileOutputStream(f);
            Utility.CopyStream(is, os);
            os.close();
            bitmap = decodeFile(f);
            return bitmap;
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (ex instanceof OutOfMemoryError)
                memoryCache.clear();
            return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f) {
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            //Find the correct scale value. It should be the power of 2.

            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
        }
        return null;
    }

    //Task for the queue
    private class PhotoToLoad {
        public String url;
        public Integer imgCount;
        public boolean status = true;

        public PhotoToLoad(String u, Integer imgCount) {
            url = u;
            this.imgCount = imgCount;
            this.status = status;
        }
    }

    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;

        PhotosLoader(PhotoToLoad photoToLoad) {
            this.photoToLoad = photoToLoad;
        }

        @Override
        public void run() {
//            if (imageViewReused(photoToLoad))
//                return;
            Bitmap bmp = getBitmap(photoToLoad.url);
            memoryCache.put(photoToLoad.url, bmp);
//            if (imageViewReused(photoToLoad))
//                return;

            if (imageCallback != null) {
                photoToLoad.status = false;
                imageCallback.updatePhoto(bmp, photoToLoad.imgCount);
            }
//            BitmapDisplayer bd = new BitmapDisplayer(bmp, photoToLoad);
//            Activity a = (Activity) photoToLoad.imageView.getContext();
//            a.runOnUiThread(bd);
        }
    }

    boolean imageViewReused(PhotoToLoad photoToLoad) {
//        String tag = imageViews.get(photoToLoad.imgCount);
//        if (tag == null || !tag.equals(photoToLoad.url))
//            return true;
        return false;
    }

    public void clearCache() {
        memoryCache.clear();
        fileCache.clear();
    }

}