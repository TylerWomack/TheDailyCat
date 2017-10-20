package com.tylerapps.tylerwomack.thedailycat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Tyler Womack on 8/31/2017.
 */

public class PhoneOnReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences preferences = context.getSharedPreferences("MyPreferences", 0);
        Boolean daily = preferences.getBoolean("daily", true);
        long lastChanged = preferences.getLong("LastChanged", 0);
        Boolean disabled = preferences.getBoolean("disabled", false);
        Boolean hourly = preferences.getBoolean("hourly", false);
        Boolean weekly = preferences.getBoolean("weekly", false);
        long lastCatOfTheDay = preferences.getLong("lastCatOfTheDay", 0);

        //this section determines if we need to set a new CatOfTheDay image. If we don't, it moves on and grabs a picture from Reddit if appropriate.
        if (weekly == true && (System.currentTimeMillis() - lastCatOfTheDay) > 604800000) {
            ImageManager im = new ImageManager();
            im.getCatOfTheDay(context);
            SharedPreferences.Editor editor = context.getSharedPreferences("MyPreferences", MODE_PRIVATE).edit();
            editor.putLong("lastCatOfTheDay", System.currentTimeMillis());
            editor.apply();
            return;
        } else if (daily == true || hourly == true) {

            if (DateUtils.isToday(lastCatOfTheDay)) {

            } else {
                ImageManager im = new ImageManager();
                im.getCatOfTheDay(context);
                SharedPreferences.Editor editor = context.getSharedPreferences("MyPreferences", MODE_PRIVATE).edit();
                editor.putLong("lastCatOfTheDay", System.currentTimeMillis());
                editor.apply();
                return;
            }
        }


        //the code below triggers a wallpaper change if it hasn't been changed in the past hour, day, or week, depending on settings.
        if (daily == true && disabled == false) {
            if (DateUtils.isToday(lastChanged)) {

            } else {
                ImageManager im = new ImageManager();
                im.newImageHandler(context);

                SharedPreferences.Editor editor = context.getSharedPreferences("MyPreferences", MODE_PRIVATE).edit();
                editor.putLong("LastChanged", System.currentTimeMillis());
                editor.apply();
            }
        } else if (disabled == false && hourly == true) {
            if (((System.currentTimeMillis() - lastChanged) - 3600000) > 0) {
                //if the time elapsed is greater than an hour
                ImageManager im = new ImageManager();
                im.newImageHandler(context);
                SharedPreferences.Editor editor = context.getSharedPreferences("MyPreferences", MODE_PRIVATE).edit();
                editor.putLong("LastChanged", System.currentTimeMillis());
                editor.apply();

            }
        } else if (disabled == false && weekly == true) {
            if (((System.currentTimeMillis() - lastChanged) - 604800000) > 0) {
                ImageManager im = new ImageManager();
                im.newImageHandler(context);
                SharedPreferences.Editor editor = context.getSharedPreferences("MyPreferences", MODE_PRIVATE).edit();
                editor.putLong("LastChanged", System.currentTimeMillis());
                editor.apply();
            }
        }
    }
}