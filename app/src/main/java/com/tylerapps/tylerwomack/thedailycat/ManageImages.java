package com.tylerapps.tylerwomack.thedailycat;

import android.content.Context;
import android.graphics.Bitmap;

/**
 * Created by Tyler on 10/27/2016.
 */
public interface ManageImages {

    public Bitmap getImageFromDisk(Context c);

    public void saveToDisk(Context c, Bitmap bitmap);

    public int checkDiskRemaining();

    public void changeWallpaper(Bitmap bitmap, Context c);

    public Boolean dataFilter(Context c, String url);

    public Bitmap convertNextUrlToBitmap(Context c);

}
