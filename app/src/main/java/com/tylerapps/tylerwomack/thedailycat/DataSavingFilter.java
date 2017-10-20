package com.tylerapps.tylerwomack.thedailycat;

import android.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

/**
 * Created by Tyler on 9/13/2016.
 */

/**
 * tests urls (in random order) and returns the index of the first one that is smaller a given number of bytes.
 */
public class DataSavingFilter {

    int size;

    /**
     * @param lastIndexofUrl  the number of urls that have been passed to us
     * @param dataFilterLimit the upper size, in bytes, that we will allow to be downloaded.
     * @param urls            an array of urls that are linked to images that we can test.
     * @return indexofUrlThatPassed - the index of a url that passed our filter. If none passed, returns -1.
     */
    public int filter(int lastIndexofUrl, int dataFilterLimit, String[] urls) {

        //if this method returns -1, it signals to the calling method that none of your images passed the filter.
        int indexOfUrlThatPassed = -1;

        //creates an integer array that ascends from 0 to max.
        int[] placeholder = new int[lastIndexofUrl];
        for (int i = 0; i < lastIndexofUrl; i++) {
            placeholder[i] = i;
        }

        //randomly shuffles the above array containing integers. Adds randomness to which images we are testing, so we don't end up testing the same ones over and over if they keep failing.
        shuffleArray(placeholder);

        //we used the placeholder to create randomness, and to give us an array of indices to test.
        //now, we use it to test our array of url strings, one by one until one of them passes our filter and is deemed small enough to download.
        for (int i = 0; i < placeholder.length; i++) {
            int size = checkPictureSize(urls[placeholder[i]]);
            if (size > dataFilterLimit) {
                i++;
            } else if (size > -1) {
                indexOfUrlThatPassed = placeholder[i];
                //here, we've found a url that passed the test, so we're going to return it.
                return indexOfUrlThatPassed;
            }
        }
        return indexOfUrlThatPassed;
    }

    /**
     * shuffles an int[] and returns it.
     *
     * @param ar the int[] to shuffle
     */
    static int[] shuffleArray(int[] ar) {
        Random rnd = new Random();
        for (int i = ar.length - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            if (i == index) {
                ++i;
            } else {
                int a = ar[index];
                ar[index] = ar[i];
                ar[i] = a;
            }
        }
        return ar;
    }


    public int checkSize(String url) {
        checkSizeInBackground(url);
        return size;
    }

    /**
     * quickly checks the size of a image url without downloading it
     *
     * @param url the url we are testing
     * @return the size of the image in question, in bytes
     */
    public int checkPictureSize(String url) {

        URL testUrl = null;
        try {
            //Log.d("test url", url);
            testUrl = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection conn = null;
        try {
            conn = testUrl.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // now you get the content length
        int contentLength = conn.getContentLength();

        return contentLength;
    }

    /**
     * creates a new thread to call checkPictureSize.
     *
     * @param url
     */
    public void checkSizeInBackground(String url) {
        final String passIn = url;
        Thread thread = new Thread() {

            public void run() {
                try {

                    size = checkPictureSize(passIn);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
        //this prevents the program from proceeding until it has retrieved the HTML
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
