package com.tylerapps.tylerwomack.thedailycat;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by Tyler on 10/18/2016.
 */

public class DownloadUrlList {
    //used to extend IntentService
    String sourceSelection;
    String html;
    String secondarySelection;

    public ArrayList<String> download(String source, String secondary) {
        sourceSelection = source;
        secondarySelection = secondary;

        final ArrayList<String> siteAndSectionData = new ArrayList<String>();

        siteAndSectionData.add(sourceSelection);
        siteAndSectionData.add(secondarySelection);

        htmlThreadHandler(siteAndSectionData);
        extractUrls extractUrls = new extractUrls();
        String[] returned = new String[300];
        returned = extractUrls.extract(html);
        ArrayList<String> urlList = new ArrayList<String>(Arrays.asList(returned));

        Set<String> hs = new HashSet<>();
        hs.addAll(urlList);
        urlList.clear();
        urlList.addAll(hs);

        //urlList has now been cleaned: Duplicates removed by converting to and from a regular Set.

        return urlList;
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
