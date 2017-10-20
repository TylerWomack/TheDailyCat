package com.tylerapps.tylerwomack.thedailycat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5053252;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simplest_layout);

        RadioGroup rGroup = (RadioGroup) findViewById(R.id.radioGroup);

        SharedPreferences preferences = getSharedPreferences("MyPreferences", 0);
        if (preferences.getBoolean("hourly", false) == true) {
            rGroup.check(R.id.Hourly);
        } else if (preferences.getBoolean("daily", true) == true) {
            rGroup.check(R.id.Daily);
        } else if (preferences.getBoolean("weekly", false) == true) {
            rGroup.check(R.id.Weekly);
        }

        final LinearLayout changeCatButton = (LinearLayout) findViewById(R.id.nextCatButton);

        changeCatButton.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(final View view, MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    changeButtonColor();
                    changeButtonPicture();
                    if (Build.VERSION.SDK_INT > 20) {
                        changeCatButton.setElevation(0);
                    }
                    Log.d("TouchTest", "Touch down");
                } else if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    if (Build.VERSION.SDK_INT > 20) {
                        changeCatButton.setElevation(16);
                    }
                    Log.d("TouchTest", "Touch up");
                    new Thread() {
                        public void run() {
                            try {
                                changePhoto(view);
                            } catch (Exception e) {
                                //handle failure here
                            }
                        }
                    }.start();
                }
                return true;
            }
        });

        // This overrides the radiogroup onCheckListener
        rGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // This will get the radiobutton that has changed in its check state
                RadioButton checkedRadioButton = (RadioButton) group.findViewById(checkedId);
                // This puts the value (true/false) into the variable
                boolean isChecked = checkedRadioButton.isChecked();
                // If the radiobutton that has changed in check state is now checked...
                if (isChecked) {
                    // Changes the textview's text to "Checked: example radiobutton text"
                    if (checkedRadioButton.getText().equals("Hourly")) {
                        SharedPreferences preferences = getSharedPreferences("MyPreferences", 0);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("hourly", true);
                        editor.putBoolean("weekly", false);
                        editor.putBoolean("daily", false);
                        editor.apply();
                    } else if (checkedRadioButton.getText().equals("Daily")) {
                        SharedPreferences preferences = getSharedPreferences("MyPreferences", 0);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("daily", true);
                        editor.putBoolean("weekly", false);
                        editor.putBoolean("hourly", false);
                        editor.apply();
                    } else if (checkedRadioButton.getText().equals("Weekly")) {
                        SharedPreferences preferences = getSharedPreferences("MyPreferences", 0);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putBoolean("weekly", true);
                        editor.putBoolean("daily", false);
                        editor.putBoolean("hourly", false);
                        editor.apply();
                    }
                }
            }
        });
    }

    public void changeButtonColor() {
        LinearLayout changeCatButton = (LinearLayout) findViewById(R.id.nextCatButton);
        GradientDrawable shape = (GradientDrawable) changeCatButton.getBackground();
        int[] colors = getResources().getIntArray(R.array.buttonColors);
        int randomColor = colors[new Random().nextInt(colors.length)];
        shape.setColor(randomColor);
    }

    public void changeButtonPicture() {
        final ImageView catEmogi = (ImageView) findViewById(R.id.catemogi);
        Random r = new Random();
        int randomEmoji = r.nextInt(5);
        if (randomEmoji == 0) {
            catEmogi.setImageResource(R.drawable.catemogi);
        } else if (randomEmoji == 1) {
            catEmogi.setImageResource(R.drawable.catemoji2);
        } else if (randomEmoji == 2) {
            catEmogi.setImageResource(R.drawable.catemoji3);
        } else if (randomEmoji == 3) {
            catEmogi.setImageResource(R.drawable.catemoji4);
        } else if (randomEmoji == 4) {
            catEmogi.setImageResource(R.drawable.catemoji5);
        }

    }

    /**
     * asks permission to read external storage. If permission exists or is granted, launches the image uploading activity.
     *
     * @param v
     */
    public void launchImageUploader(View v) {
        askForExternalStorage();
    }

    public void askForExternalStorage() {
        if (android.os.Build.VERSION.SDK_INT > 22) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                }
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant that should be quite unique
            } else {
                //this is either called here, or possibly in the request permissions result (depending on if they gave you permission.
                //Here, obviously, you have permission, so you go ahead and launch the activity.
                Intent intent = new Intent(this, ImageUploader.class);
                startActivity(intent);
            }
        }
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //if permission granted
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE && grantResults[0] == 0) {
            Intent intent = new Intent(this, ImageUploader.class);
            startActivity(intent);

        } else if (requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE && grantResults[0] == -1) {
            Toast.makeText(getApplicationContext(), "Sorry, we can't upload a picture without that permission!", Toast.LENGTH_LONG).show();
        }
    }


    //calls checkForCatOfTheDay, which changes the photo to the cat of the day. If it didn't change to the cat of the day, it grabs the next picture
    //from Reddit/cats
    public void changePhoto(View v) {
        final View view = v;
        Boolean changed = checkForCatOfTheDay(v);

        if (changed == false) {
            ImageManager imageManager = new ImageManager();
            imageManager.newImageHandler(v.getContext());
        }

        //including this so the toasts appear on the main ui thread.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(view.getContext(), "Your background has been changed!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //changes the background to the cat of the day if appropriate. Returns true if changed, false otherwise.
    public boolean checkForCatOfTheDay(View v) {
        Boolean changed = false;
        SharedPreferences preferences = getSharedPreferences("MyPreferences", 0);
        long lastCatOfTheDay = preferences.getLong("lastCatOfTheDay", 0);
        if (!DateUtils.isToday(lastCatOfTheDay)) {
            changed = true;
            SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences("MyPreferences", MODE_PRIVATE).edit();
            editor.putLong("lastCatOfTheDay", System.currentTimeMillis() - 600000);
            editor.apply();

            ImageManager imageManager = new ImageManager();
            imageManager.getCatOfTheDay(v.getContext());
            Toast.makeText(getApplicationContext(), "Cat of the Day!", Toast.LENGTH_SHORT);
        }
        return changed;
    }
}
