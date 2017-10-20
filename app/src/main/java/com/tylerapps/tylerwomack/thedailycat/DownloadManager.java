package com.tylerapps.tylerwomack.thedailycat;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

/**
 * Created by Tyler on 10/18/2016.
 */

public class DownloadManager extends IntentService {

    String html;

    public DownloadManager() {
        super("DownloadManager");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SharedPreferences preferences = getSharedPreferences("MyPreferences", 0);
        //retrieves the variable that tells us if the user is going to use Reddit or Instagram (SourceSelection)
        final String sourceSelection = preferences.getString("SourceSelection", "Reddit");
        //retrieves the subreddit the user has chosen. Note: How are we going to allow multiple selections?
        final String subreddit = preferences.getString("Subreddit", "earthporn");
        //retrieves the instagram account the user wants to follow
        final String instagramAccount = preferences.getString("InstagramAccount", "kingjames");
        //Wifi check below - if disabled if Wifi mode is active and you don't have wifi, the program doesn't continue
        Boolean disableIfNotWifi = preferences.getBoolean("disableIfNotWifi", false);

        if (disableIfNotWifi == true) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected() == false) {

                Log.d("No Wifi", "Not changing wallpaper because you don't have wifi right now.");
                //this ends the entire method if you don't have wifi.

                return;
            }
        }

        //End of wifi check


        final ArrayList<String> siteAndSectionData = new ArrayList<String>();
        siteAndSectionData.add(sourceSelection);


        if (sourceSelection.equals("Reddit")) {
            siteAndSectionData.add(subreddit);
        } else if (sourceSelection.equals("Instagram")) {
            siteAndSectionData.add(instagramAccount);
        }

        htmlThreadHandler(siteAndSectionData);


        extractUrls extractUrls = new extractUrls();
        //creates a new string array where we are going to return our URLs from extractURL - limit 300
        String[] returned = new String[300];
        //passing in our html to extractURLs, running extract() which should return a string array of urls
        returned = extractUrls.extract(html);

        //returned - those are your urls.

        //finding how many slots in returned[] are actually filled with urls
        int urlCount = 0;
        for (int i = 0; i < returned.length; i++) {
            if (returned[i] != null) {
                //Log.d("urls returned", returned[i]);
                urlCount++;
            }
        }

        //This is part of the randomizer, max is the upper limit on the random int equation
        int max = urlCount;

        Random random = new Random();

        if (max != 0) {

            //randomInt is used to select a random url from the returned array of urls
            int randomInt = random.nextInt(max);

            SharedPreferences sharedPreferences = getSharedPreferences("MyPreferences", 0);
            int dataFilterLimit = sharedPreferences.getInt("dataFilterLimit", 2000000);

            DataSavingFilter dataSavingFilter = new DataSavingFilter();
            randomInt = dataSavingFilter.filter(max, dataFilterLimit, returned);
            //checking to see if your dataSavingsFilter returned -1 (indicating that none of the urls passed your datafilter)

            if (randomInt > -1) {
                UrlToBitmap urlToBitmap = new UrlToBitmap();
                //calling the set method of setBackground, passing in our random url and our context
                //setBackground.set(returned[randomInt], getApplicationContext());
                Log.d("url setting background", returned[randomInt]);

                //setBackground.set(returned[randomInt], getApplicationContext());
                urlToBitmap.get(returned[randomInt], getApplicationContext());

            }


        } else {
            Log.d("Error", "No urls returned");
            return;
        }
    }

    public void htmlThreadHandler(ArrayList<String> arrayList) {
        final ArrayList<String> siteAndSectionData = arrayList;
        Thread thread = new Thread() {
            Object x = null;

            public void run() {
                try {
                    x = new getHTML().execute(siteAndSectionData).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                if (x != null) {
                    html = x.toString();
                } else {
                    Log.d("error", "getHTML returned null");
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
