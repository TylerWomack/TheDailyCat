package com.tylerapps.tylerwomack.thedailycat;

import android.util.Log;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Tyler on 8/9/2016.
 */
public class extractUrls {

    String masterUrls[] = new String[400];

    String imgur2Urls[] = new String[100];
    String imgurUrls[] = new String[100];
    String redditUrls[] = new String[100];
    String redditPngUrls[] = new String[100];
    String flickrUrls[] = new String[100];
    String instagramUrls[] = new String[100];


    protected String[] extract(String html) {

        if (html == null) {
            Log.e("download error", "Wasn't able to get html string");
            return null;
        }

        imgur2Urls = isValid(html, imgur2Pattern);

        //cleans up the imgur2Urls (imgur.com, not i.imgur.com)
        for (int i = 0; i < imgur2Urls.length; i++) {
            if (imgur2Urls[i] != null) {
                imgur2Urls[i] = imgur2Urls[i].substring(0, imgur2Urls[i].length() - 1);
                String temporarySplit1 = imgur2Urls[i].substring(0, 7) + "i." + imgur2Urls[i].substring(7, imgur2Urls[i].length()) + ".jpg";
                imgur2Urls[i] = temporarySplit1;

            }
        }


        int imgur2Counter = 0;
        for (int i = 0; i < imgur2Urls.length; i++) {
            if (imgur2Urls[i] != null)
                imgur2Counter++;
        }

        imgurUrls = isValid(html, imgurPattern);

        int imgurCounter = 0;
        for (int i = 0; i < imgurUrls.length; i++) {
            if (imgurUrls[i] != null)
                imgurCounter++;
        }


        redditUrls = isValid(html, redditPattern);

        int redditCounter = 0;
        for (int i = 0; i < redditUrls.length; i++) {
            if (redditUrls[i] != null)
                redditCounter++;
        }


        redditPngUrls = isValid(html, redditPatternPng);

        int redditPngCounter = 0;
        for (int i = 0; i < redditUrls.length; i++) {
            if (redditPngUrls[i] != null)
                redditPngCounter++;
        }


        flickrUrls = isValid(html, flickrPattern);

        int flickrCounter = 0;
        for (int i = 0; i < flickrUrls.length; i++) {
            if (flickrUrls[i] != null)
                flickrCounter++;
        }

        instagramUrls = isValid(html, instagramPattern);

        int instagramCounter = 0;
        for (int i = 0; i < instagramUrls.length; i++) {
            if (instagramUrls[i] != null)
                instagramCounter++;
        }


        int masterCounter = 0;

        //loads the imgur2Urls
        for (int i = 0; i < imgur2Counter; i++) {
            masterUrls[i] = imgur2Urls[i];
        }
        masterCounter = imgur2Counter;

        //loads the imgurUrls
        for (int i = 0; i < imgurCounter; i++) {
            masterUrls[masterCounter + i] = imgurUrls[i];
        }
        masterCounter = masterCounter + imgurCounter;


        for (int i = 0; i < redditCounter; i++) {
            masterUrls[masterCounter + i] = redditUrls[i];
        }
        masterCounter = masterCounter + redditCounter;

        for (int i = 0; i < redditPngCounter; i++) {
            masterUrls[masterCounter + i] = redditPngUrls[i];
        }
        masterCounter = masterCounter + redditPngCounter;


        for (int i = 0; i < flickrCounter; i++) {
            masterUrls[masterCounter + i] = flickrUrls[i];
        }
        masterCounter = masterCounter + flickrCounter;

        for (int i = 0; i < instagramCounter; i++) {
            masterUrls[masterCounter + i] = instagramUrls[i];
        }

        return masterUrls;

    }

    private static final Pattern matchAny = Pattern.compile("(http:\\/\\/[imgur.com|reddit.com|flickr.com]\\/.{1,60}?[\\.jpg|\\\"])");

    private static final Pattern instagramPattern = Pattern.compile("(https:..{1,200}?n.jpg)");
    private static final Pattern imgur2Pattern = Pattern.compile("(http:..imgur.com..{1,60}?\\\")");
    private static final Pattern imgurPattern = Pattern.compile("(http:..i.imgur.com..{1,60}?.jpg)");
    private static final Pattern redditPattern = Pattern.compile("(https:..i.redd.it..{1,60}?.jpg)");
    private static final Pattern redditPatternPng = Pattern.compile("(https:..i.redd.it..{1,60}?.png)");
    private static final Pattern flickrPattern = Pattern.compile("(https:..c..staticflickr.com..{1,60}?.jpg)");

    private String[] isValid(String string, Pattern pattern) {

        String[] urls = new String[100];
        int start = 0;
        Matcher matcher = pattern.matcher(string);

        for (int u = 0; u < 100; u++) {
            if (matcher.find(start)) {
                int i = matcher.groupCount();
                int last = matcher.end();

                for (int q = 0; q < i; q++) {
                    start = last;

                    //testing if array contains url already

                    if (Arrays.asList(urls).contains(matcher.group(q))) {
                        u--;
                    } else {
                        urls[u] = matcher.group(q);
                    }

                }
            } else {
                return urls;
            }
        }
        return urls;
    }
}
