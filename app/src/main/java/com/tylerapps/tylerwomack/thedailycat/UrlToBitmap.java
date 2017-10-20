
package com.tylerapps.tylerwomack.thedailycat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;

//Created by Tyler on 8/9/2016.


public class UrlToBitmap implements Target {

    public UrlToBitmap() {
    }

    private Target target = new Target() {

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Log.d("test", "Bitmap failed");
        }

        @Override
        public void onPrepareLoad(Drawable drawable) {
            Log.d("test", "onPrepareLoad");
        }
    };

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }

    public Bitmap get(String url, final Context context) {
        Bitmap bitmap = null;

        try {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x;
            int height = size.y;

            DownloadTask downloadTask = new DownloadTask(context, url, width, height);
            bitmap = downloadTask.doInBackground();

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    class DownloadTask extends AsyncTask<String, Void, Bitmap> {
        private Context mContext;
        private String mUrl;
        int mWidth;
        int mHeight;

        public DownloadTask(Context context, String url, int width, int height) {
            mContext = context;
            mUrl = url;
            mWidth = width;
            mHeight = height;

        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap bitmap = null;
            try {

                bitmap = Picasso.with(mContext).load(mUrl).config(Bitmap.Config.RGB_565).resize(mWidth, mHeight).onlyScaleDown().centerCrop().get();
                return bitmap;

            } catch (IOException e) {
                e.printStackTrace();
            }
            //add code to crash gracefully here
            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
        }
    }
}

