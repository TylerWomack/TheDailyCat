package com.tylerapps.tylerwomack.thedailycat;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Tyler on 10/27/2016.
 */
public class ImageManager implements ManageImages{

    private ArrayList<String> urls; //urls linked to images, which can be later turned into image Bitmaps.
    private int mostRecentlyLoadedDiskSlot; //an integer linked to the most recent "save file" that we loaded a wallpaper from.
    private int mostRecentlySavedDiskSlot; ////an integer linked to the most recent "save file" that we saved a wallpaper.
    private int diskSize = 20; //the number of wallpapers we are going to save on disk for future use.
    private String sourceSelection = "Reddit"; //the website we are getting our image urls from - typically Reddit or Instagram.
    private String secondarySelection = "cats"; //the subreddit (if using Reddit) or the instagram account name, if using Instagram, that we are getting our image urls from.
    WallpaperManager wpm;
    private Bitmap bitmap;

    /**
     * ensures that the instance variables above are the same as the ones stored in sharedPreferences.
     * @param c
     */
    public void updateParameters(Context c){
        SharedPreferences preferences = c.getSharedPreferences("MyPreferences", 0);
        mostRecentlyLoadedDiskSlot = preferences.getInt("mostRecentlyLoadedDiskSlot", -2);
        mostRecentlySavedDiskSlot = preferences.getInt("currentSaveFile", -2);
        sourceSelection = preferences.getString("SourceSelection", "Reddit");
        secondarySelection = preferences.getString("SecondarySelection", "cats");
        diskSize = preferences.getInt("diskSize", 20);
    }


    /**
     * fills the save slots with images, nearly to capacity (leaves two slots unfilled as a buffer between the most recently saved and most recently loaded indices)
     * @param c
     */
    public void refillDisk(Context c){
        int imagesRemaining = 0;
        //the initial values for these two values when installed is -2, implying we have 0 images on disk.
        if (mostRecentlySavedDiskSlot == -2 || mostRecentlyLoadedDiskSlot == -2){
            imagesRemaining = 0;
        }else {
            imagesRemaining = checkDiskRemaining();
        }

        updateParameters(c);
        //I added in the -2 because I want to leave a little buffer - I don't want to have the most recently saved slot == the most recently loaded slot. That situation could confuse the
        //check images remaining function. By leaving a little room and not filling the fully allotted slots, I'll avoid approaching the situation
        // where the mostRecentlySaved parameter in SharedPreferences == mostRecentlyLoaded (which would cause the program to be unsure if the disk has 0, or 20 images remaining)
        int toDownload = (diskSize - imagesRemaining - 2);

        if (toDownload > 0){
            for (int i = 0; i < toDownload; i++){
                Bitmap bitmap = convertNextUrlToBitmap(c);
                if(bitmap != null)
                saveToDisk(c, bitmap);
            }
        }
    }

    /**
     * if there are less than nine images on disk, calls refillDisk on a background thread.
     * @param c
     * @param imagesRemainingOnDisk
     */
    public void checkAndRefillDiskInBackground(final Context c, final int imagesRemainingOnDisk){

        Thread thread = new Thread(){

            public void run(){
                try{
                    if (imagesRemainingOnDisk < 9){
                        refillDisk(c);
                    }
                         }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        thread.start();
    }


    /**
     * Checks the images remaining on disk. If there are a sufficient number of images on disk, grabs one and sets it as your wallpaper.
     * If there aren't, it calls a method which refills the disk in a background thread.
     * If there are one or fewer images remaining on disk, it retrieves a new image by itself without relying on previously stored images - a time consuming but
     * safer approach and a quicker approach if you're optimizing for getting the very next image as fast as possible.
     *
     * @param c Context
     */
    public void newImageHandler(Context c){
        updateParameters(c);
        int imagesRemainingOnDisk = checkDiskRemaining();

        if (imagesRemainingOnDisk > 1){
            changeWallpaper(getImageFromDisk(c), c);
            //refills disk on a background thread if there are less than nine images remaining.
            checkAndRefillDiskInBackground(c, imagesRemainingOnDisk - 1);
        }else {
            //I'm using convertNextUrlToBitmap here if there are one or fewer images remaining on disk. 
            // This code will almost never run - checkAndRefillDisk begins working and refilling when the disk is half empty,
            //meaning that this will only fire when the user first installs the app or if checkAndRefill can't keep up with volume.
            //I call it here mainly for the initial install situation (empty disk) because it is a quicker way to get that first image.
            changeWallpaper(convertNextUrlToBitmap(c), c);
            checkAndRefillDiskInBackground(c, imagesRemainingOnDisk);
        }
    }

    /**
     * Using the next available slot indicated by SharedPreferences, this loads the next wallpaper from our saved files.
     * @param c Context
     * @return the Bitmap wallpaper
     */
    public Bitmap getImageFromDisk(Context c){
        updateParameters(c);
        //if we've loaded the image from the last index already, reached the end of the saved slots then...
        if (mostRecentlyLoadedDiskSlot > diskSize -1 ){
            //-1 because the line directly below moves to the next index before loading the image. We want it to load from index 0 at that point.
            mostRecentlyLoadedDiskSlot = -1;
        }

        //loads bitmap from file.
        File myFile = new File(c.getFilesDir(), "myFile" + String.valueOf(mostRecentlyLoadedDiskSlot + 1));
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(myFile.getAbsolutePath(),bmOptions);
        SharedPreferences preferences = c.getSharedPreferences("MyPreferences", 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("mostRecentlyLoadedDiskSlot", mostRecentlyLoadedDiskSlot + 1);
        editor.apply();
        mostRecentlyLoadedDiskSlot++;
        return bitmap;
    }

    /**
     * Downloads the list of urls from the subreddit 'hiddenCatOfTheDay' (an admin-only subreddit I use to load the cat of the day image), convert it to a bitmap on a background thread, and changes the users wallpaper using the image.
     * @param c
     */
    public void getCatOfTheDay(final Context c){

        ArrayList<String> catOfTheDayUrls = downloadList(c, "Reddit", "HiddenCatOfTheDay");

        final Context context = c;
        final String url = catOfTheDayUrls.get(0);
        //clears urls because I only want one image - the cat of the day - and because I want to ensure that urls is reset and cleaned before being refilled with the normal, default images.
        urls.clear();
        
        //starting a background thread for Picasso to convert URL to bitmap.
        Thread thread = new Thread(){

            public void run(){
                try{
                    UrlToBitmap urlToBitmap = new UrlToBitmap();
                    bitmap = urlToBitmap.get(url, context);
                    if (bitmap != null){
                        changeWallpaper(bitmap, context);
                    }else {
                        newImageHandler(c);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        };

        thread.start();

        try{
            thread.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * This method converts the next available String url into a bitmap on a background thread by calling the class UrlToBitmap.
     * If there are no available urls, it gets more by calling downloadList.
     * calls 
      * @param c Context
     * @return an image Bitmap.
     */
    public Bitmap convertNextUrlToBitmap(Context c){

            if (urls == null)
                downloadList(c, sourceSelection, secondarySelection);

            if (urls.size() == 0)
                downloadList(c, sourceSelection, secondarySelection);

            String url = urls.get(0);
            urls.remove(0);

            //I disabled the dataFilter here because I don't believe this project needs it, because a data-saving setting hasn't been built for this project,
            // and because I don't want to limit the variety of images available to the user. However, I'm leaving this commented code here for future reference.
            //if (dataFilter(c, url)){

                final Context context = c;
                final String myUrl = url;


                Thread thread = new Thread(){

                    public void run(){
                        try{
                            UrlToBitmap urlToBitmap = new UrlToBitmap();
                            bitmap = urlToBitmap.get(myUrl, context);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                };

                thread.start();

                try{
                    thread.join();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                return bitmap;
            //}
    }

    /**
     * This saves a bitmap to disk. Using shared preferences it determines the next save slot available, and begins overwriting previously saved wallpapers if
     * it has reached the allotted limit (diskSize), which by default is 20 images.
     * @param c Context
     * @param bitmap a bitmap image (for our purposes, an unused wallpaper)
     */
    public void saveToDisk(Context c, Bitmap bitmap) {
        updateParameters(c);
        SharedPreferences preferences = c.getSharedPreferences("MyPreferences", 0);
        SharedPreferences.Editor editor = preferences.edit();

        if (mostRecentlySavedDiskSlot < diskSize){
            editor.putInt("currentSaveFile", mostRecentlySavedDiskSlot + 1);
            editor.apply();

            SaveToFile stf = new SaveToFile();
            stf.saveToFile(bitmap, c);
        }

        if (mostRecentlySavedDiskSlot == diskSize){
            editor.putInt("currentSaveFile", 0);
            editor.apply();

            SaveToFile stf = new SaveToFile();
            stf.saveToFile(bitmap, c);
        }
    }

    /**
     * A way to test image sizes before downloading them. Allows for the creation of settings that save user data. Allows for a preference "dataFilterLimit" to be set (limit is number of bytes in an image). Default is 2000000, or 2 Mb. Tests a url without downloading, and
     * if the size of the image the url links to is less than the limit, returns true. Else, returns false.
     * @param c Context
     * @param url
     * @return Boolean - if the size of the image linked by the url is below the limit or False if it isn't.
     */
    public Boolean dataFilter(Context c, String url){
        SharedPreferences sharedPreferences = c.getSharedPreferences("MyPreferences", 0);
        int dataFilterLimit = sharedPreferences.getInt("dataFilterLimit", 2000000);

        DataSavingFilter dsf = new DataSavingFilter();
        int size = dsf.checkSize(url);
        Log.d("test", String.valueOf(size));
        if (size > dataFilterLimit){
            return false;
        }else{
            return true;
        }
    }

    /**
     *
     * @return the number of images remaining on disk that have not been used as a wallpaper yet.
     */
    @Override
    public int checkDiskRemaining(){

        int imagesRemaining = 0;

        if (mostRecentlySavedDiskSlot > mostRecentlyLoadedDiskSlot){
            imagesRemaining = mostRecentlySavedDiskSlot - mostRecentlyLoadedDiskSlot;
        }

        if (mostRecentlySavedDiskSlot < mostRecentlyLoadedDiskSlot){
            imagesRemaining = diskSize - (mostRecentlyLoadedDiskSlot - mostRecentlySavedDiskSlot);

        }
        return imagesRemaining;
    }

    /**
     *
     * @param bitmap the bitmap of the image you want your phone wallpaper to change to.
     * @param c Context
     */
    @Override
    public void changeWallpaper(Bitmap bitmap, Context c) {
        try{
            wpm = WallpaperManager.getInstance(c);
            wpm.setBitmap(bitmap);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param c Context
     * @param sourceSelection - A String indicating the name of the website you're pulling from - typically Reddit or Instagram
     * @param secondarySelection - A String indicating the name of the secondary location of the images, for example, a subreddit or instagram account name.
     * @return a randomized ArrayList of url Strings that link to various images retrieved from the source you selected.
     */
    public ArrayList<String> downloadList(Context c, String sourceSelection, String secondarySelection){

        DownloadUrlList dul = new DownloadUrlList();
        urls = dul.download(sourceSelection, secondarySelection);
        urls = randomizeUrlList(urls);
        return urls;
    }

    /**
     * Randomizes the order of the urls in our list, to add unpredictability to the order the backgrounds change.
     * @param urls the urls of various images we have scraped.
     * @return an arraylist of url Strings, now in a random order.
     */
    private ArrayList<String> randomizeUrlList(ArrayList<String> urls){
        ArrayList<String> list = urls;
        Collections.shuffle(urls);
        return list;
    }
}


