package com.tylerapps.tylerwomack.thedailycat;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Tyler on 8/8/2016.
 */
public class getHTML extends AsyncTask<ArrayList<String>, Void, Object> {

    String urlContent;

    @Override
    protected Object doInBackground(ArrayList<String>... passing) {
        ArrayList<String> siteAndSectionData = passing[0];

        String url = "";
        int lookbackRange = 720;

        Random random = new Random();
        int randomDay = random.nextInt(lookbackRange);


        //This part messes with time, and figuring out how to mess with the instagram url to get historical pics
        Long timeRightNow = System.currentTimeMillis();

        double daysPassed = (((float) timeRightNow - (float) 1473895467597L) / (float) 86400000L);

        Log.d("days passed", String.valueOf(daysPassed));

        double initialNumber = 13393800;
        double todaysNumber = initialNumber + (daysPassed * 7275);

        int soughtNumber = (int) (todaysNumber - (randomDay * 7275));

        String id = String.valueOf(soughtNumber) + "00000000000";

        Log.d("try this as maxid", id);
        if (siteAndSectionData.contains("Reddit")) {
            url = "https://www.reddit.com/r/" + siteAndSectionData.get(1) + "/top/";

        } else if (siteAndSectionData.contains("Instagram")) {
            url = "https://www.instagram.com/" + siteAndSectionData.get(1) + "/?max_id=" + id;

        }


        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = null;
        try {

            response = client.newCall(request).execute();
            urlContent = response.body().string();


        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlContent;

    }
}
