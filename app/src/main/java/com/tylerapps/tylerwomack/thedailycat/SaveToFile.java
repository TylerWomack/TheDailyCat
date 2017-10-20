package com.tylerapps.tylerwomack.thedailycat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Tyler on 10/8/2016.
 */
public class SaveToFile {


    public void saveToFile(Bitmap bitmap, Context c) {

        SharedPreferences preferences = c.getSharedPreferences("MyPreferences", 0);
        int currentSaveFile = preferences.getInt("currentSaveFile", 0);

        try {

            File myFile = new File(c.getFilesDir(), "myFile" + String.valueOf(currentSaveFile));
            FileOutputStream fos = new FileOutputStream(myFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
