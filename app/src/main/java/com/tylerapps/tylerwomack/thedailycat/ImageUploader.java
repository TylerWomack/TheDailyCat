package com.tylerapps.tylerwomack.thedailycat;

/**
 * Created by Tyler Womack on 9/27/2017.
 */

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

/**
 * This class is used for the Cat of the Day contest - it allows users to upload images and post them to Twitter.
 */
public class ImageUploader extends Activity implements View.OnClickListener {

    private static final String PREF_NAME = "sample_twitter_pref";
    private static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    private static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    private static final String PREF_KEY_TWITTER_LOGIN = "is_twitter_loggedin";
    private static final String PREF_USER_NAME = "twitter_user_name";

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5053252;

    /* Any number for uniquely distinguish your request */
    public static final int WEBVIEW_REQUEST_CODE = 100;
    private ProgressDialog pDialog;
    private static Twitter twitter;
    private static RequestToken requestToken;
    private static SharedPreferences mSharedPreferences;
    private EditText mShareEditText;
    private TextView userName;
    private View loginLayout;
    private View shareLayout;
    private String consumerKey = "F0layqOqX91JcKmZN6vrcABdI";
    private String consumerSecret = "LztFC9ZJL2rOb0bGQVJ25Mi7EwqDLrU35vsb2WX8neoMB7hY90";
    private String callbackUrl = null;
    private String oAuthVerifier = null;
    private Uri uri = null;
    private int orientation = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initTwitterConfigs();
        //launches the image picker - allows user to select an image.
        Intent i = new Intent(this, ImagePicker.class);
        startActivityForResult(i, 5);

        /* Enabling strict mode */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.twitter_view);

        mShareEditText = (EditText) findViewById(R.id.share_text);
        mShareEditText.setText(getResources().getString(R.string.default_tweet));
        loginLayout = (RelativeLayout) findViewById(R.id.login_layout);
        shareLayout = (LinearLayout) findViewById(R.id.share_layout);
        mShareEditText = (EditText) findViewById(R.id.share_text);
        userName = (TextView) findViewById(R.id.user_name);

        /* register button click listeners */
        findViewById(R.id.btn_login).setOnClickListener(this);
        findViewById(R.id.btn_share).setOnClickListener(this);
        findViewById(R.id.btn_choose_another).setOnClickListener(this);

		/* Check if required twitter keys are set */
        if (TextUtils.isEmpty(consumerKey) || TextUtils.isEmpty(consumerSecret)) {
            Toast.makeText(this, "Twitter key and secret not configured",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        	/* Initialize application preferences */
        mSharedPreferences = getSharedPreferences(PREF_NAME, 0);

        boolean isLoggedIn = mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);

		/*  if already logged in, then hide login layout and show share layout */
        if (isLoggedIn) {
            loginLayout.setVisibility(View.GONE);
            shareLayout.setVisibility(View.VISIBLE);

            String username = mSharedPreferences.getString(PREF_USER_NAME, "");
            userName.setText(getResources().getString(R.string.hello)
                    + " " + username);

        } else {
            loginLayout.setVisibility(View.VISIBLE);
            shareLayout.setVisibility(View.GONE);

            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(callbackUrl)) {
                String verifier = uri.getQueryParameter(oAuthVerifier);
                try {

					/* Getting oAuth authentication token */
                    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

					/* Getting user id form access token */
                    long userID = accessToken.getUserId();
                    final User user = twitter.showUser(userID);
                    final String username = user.getName();

					/* save updated token */
                    saveTwitterInfo(accessToken);

                    loginLayout.setVisibility(View.GONE);
                    shareLayout.setVisibility(View.VISIBLE);
                    userName.setText(getString(R.string.hello) + username);

                } catch (Exception e) {
                    Log.e("Failed to login", e.getMessage());
                }
            }
        }
    }

    /**
     * Handles the click events for three buttons - login [to Twitter], share (tweet), or choose another [image].
     *
     * @param v the button that was clicked
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                //if the user clicked the login to Twitter button, it calls login method and starts the process
                loginToTwitter();
                break;
            case R.id.btn_share:
                //asking permission to grab pictures first. Going to try to continue the method in the onRequestPermissionsResult method.
                askForExternalStorage();
                break;
            case R.id.btn_choose_another:
                //this button is available at the end, when the image and text is loaded and the user is ready to press 'tweet.' This restarts the process and allows them to upload a new image.
                Intent intent = new Intent(this, ImageUploader.class);
                startActivity(intent);
                break;
        }
    }

    /**
     * Fires off a tweet.
     */
    public void callUpdateTwitterStatus() {
        final String status = mShareEditText.getText().toString();
        if (status.trim().length() > 0) {
            new updateTwitterStatus().execute(status);
        } else {
            Toast.makeText(this, "Message is empty!!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Called after the user has either granted or denied permission to read external storage. If the permission is granted, it calls the method callUpdateTwitterStatus();
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callUpdateTwitterStatus();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

    /**
     * asks for external storage. If external storage permissions already exist, it calls the method callUpdateTwitterStatus.
     */
    public void askForExternalStorage() {

        if (Build.VERSION.SDK_INT > 22) {

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
                // app-defined int constant that should be quite unique
            } else {
                //calling this here because normally callUpdateTwitterStatus is only called in the result of the permission selection.
                //however if we already have the permission, that is never triggered, and we'd need to call it here.
                callUpdateTwitterStatus();
            }
        }
    }

    /**
     * updates the twitter status
     */
    class updateTwitterStatus extends AsyncTask<String, String, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(ImageUploader.this);
            pDialog.setMessage("Posting to twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //this part does the actual setting of the image, and posting to twitter
        protected Void doInBackground(String... args) {

            String status = args[0];
            try {
                ConfigurationBuilder builder = new ConfigurationBuilder();
                builder.setOAuthConsumerKey(consumerKey);
                builder.setOAuthConsumerSecret(consumerSecret);

                // Access Token
                String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
                // Access Token Secret
                String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");

                AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                // Update status
                StatusUpdate statusUpdate = new StatusUpdate(status);

                Bitmap tempmap = UriToBitmap(uri);
                File file = convertBitmapToFile(tempmap);
                //fixes orientation of the image, so it is right side up
                file = fixOrientation(file, orientation);
                //repeatedly compresses then tests an image file. Through trial and error (no thanks to Twitter's lack of documentation!) I learned that the max file size allowed on their api is about 3.5 Mb
                //this rachets up the aggressiveness of compression until the file is small enough to be accepted by Twitter's api (without sacrificing quality by compressing an already compressed image)
                File compressedFile = testAndCompressUntilSmallEnough(file);

                //adds the image to the status.
                statusUpdate.setMedia(compressedFile);

                twitter4j.Status response = twitter.updateStatus(statusUpdate);
                Log.d("Status", response.getText());
            } catch (TwitterException e) {
                Log.d("Failed to post!", e.getMessage());
            }
            return null;
        }

        /**
         * @param file        the image file that we are testing/fixing the orientation of
         * @param orientation the current orientation of the image in question.
         * @return a new image file, with the orientation fixed so it is upright and not on its side - a problem in earlier iterations of this app.
         */
        private File fixOrientation(File file, int orientation) {

            //converts the file to a bitmap.
            String filePath = file.getPath();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);

            //fixes orientation of bitmap if orientation == 90
            if (orientation == 90) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            }

            return convertBitmapToFile(bitmap);
        }

        /**
         * gets a bitmap from a uri and return it.
         *
         * @param uri the unique resource identifier for our image.
         * @return an image Bitmap.
         */
        private Bitmap UriToBitmap(Uri uri) {

            InputStream is = null;
            try {
                is = getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;

        }

        //compresses the size of the image file by a percentage
        private File compressImageFile(File file, int percentage) {

            String filePath = file.getPath();
            Bitmap bitmap = BitmapFactory.decodeFile(filePath);
            // Create a file in the Internal Storage
            String fileName = "MyFile";
            FileOutputStream outputStream = null;
            try {
                outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            } catch (Exception e) {
                e.printStackTrace();
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, percentage, outputStream);
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File compressedFile = new File(getApplicationContext().getFilesDir(), "MyFile");
            return compressedFile;
        }

        private File convertBitmapToFile(Bitmap bitmap) {


            // Create a file in the Internal Storage
            String fileName = "tempFile";
            FileOutputStream outputStream = null;
            try {
                outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            } catch (Exception e) {
                e.printStackTrace();
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            try {
                if (outputStream != null)
                    outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File toReturn = new File(getApplicationContext().getFilesDir(), "tempFile");
            return toReturn;

        }

        @Override
        protected void onPostExecute(Void result) {

			/* Dismiss the progress dialog after sharing */
            pDialog.dismiss();

            Toast.makeText(ImageUploader.this, "Posted to Twitter!", Toast.LENGTH_SHORT).show();

            // Clearing EditText field
            mShareEditText.setText("");
        }

        //keeps compressing a file until it is smaller than 3 mb.
        private File testAndCompressUntilSmallEnough(File file) {
            File toReturn;

            toReturn = compressImageFile(file, 96);
            long newSize = toReturn.length();

            if (newSize > 3000000) {
                toReturn = compressImageFile(file, 94);
                newSize = toReturn.length();
            }

            if (newSize > 3000000) {
                toReturn = compressImageFile(file, 92);
            }

            return toReturn;
        }

    }


    /**
     * Saving user information, after user is authenticated for the first time.
     * You don't need to show user to login, until user has a valid access toen
     */
    private void saveTwitterInfo(AccessToken accessToken) {

        long userID = accessToken.getUserId();

        User user;
        try {
            user = twitter.showUser(userID);

            String username = user.getName();

			/* Storing oAuth tokens to shared preferences */
            SharedPreferences.Editor e = mSharedPreferences.edit();
            e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
            e.putString(PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
            e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
            e.putString(PREF_USER_NAME, username);
            e.commit();

        } catch (TwitterException e1) {
            e1.printStackTrace();
        }
    }

    /* Reading twitter essential configuration parameters from strings.xml */
    private void initTwitterConfigs() {
        consumerKey = getString(R.string.twitter_consumer_key);
        consumerSecret = getString(R.string.twitter_consumer_secret);
        callbackUrl = getString(R.string.twitter_callback);
        oAuthVerifier = getString(R.string.twitter_oauth_verifier);
    }

    /**
     * launches the twitter login process
     */
    private void loginToTwitter() {
        boolean isLoggedIn = mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);

        if (!isLoggedIn) {
            final ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(consumerKey);
            builder.setOAuthConsumerSecret(consumerSecret);

            final Configuration configuration = builder.build();
            final TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();

            try {
                requestToken = twitter.getOAuthRequestToken(callbackUrl);

                /*
                 *  Loading twitter login page on webview for authorization
                 *  Once authorized, results are received at onActivityResult
                 *  */

                final Intent intent = new Intent(this, WebViewActivity.class);
                intent.putExtra(WebViewActivity.EXTRA_URL, requestToken.getAuthenticationURL());
                startActivityForResult(intent, WEBVIEW_REQUEST_CODE);

            } catch (TwitterException e) {
                e.printStackTrace();
            }
        } else {

            loginLayout.setVisibility(View.GONE);
            shareLayout.setVisibility(View.VISIBLE);
        }
    }

    //this fires for two different reasons: when an image is returned, or when twitter authorization worked.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Uri myUri;
        //500 is the result code I created for image selection - if the result code is 500, the user has selected an image, and we have been passed the image uri and the orientation of the image.
        if (resultCode == 500) {
            //gets the image, fixes the orientation, and loads it into the image view on the twitter_view so the user sees the image they're about to tweet.
            myUri = data.getData();
            uri = myUri;
            Bundle bundle = data.getExtras();
            orientation = bundle.getInt("orientation");
            File file = new File(getImagePath(uri));
            file = fixOrientation(file, orientation);
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            Picasso.with(this).load(file).into(imageView);
        }

        //if the user has been authorized properly and logged into Twitter:
        if (resultCode == Activity.RESULT_OK) {

            //changes the UI from a twitter login screen to the screen that is ready to tweet out an image.

            String verifier = data.getExtras().getString(oAuthVerifier);
            try {
                AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);

                long userID = accessToken.getUserId();
                final User user = twitter.showUser(userID);
                String username = user.getName();

                saveTwitterInfo(accessToken);
                loginLayout.setVisibility(View.GONE);
                shareLayout.setVisibility(View.VISIBLE);
                userName.setText(ImageUploader.this.getResources().getString(
                        R.string.hello) + " " + username);

            } catch (Exception e) {
                Log.e("Twitter Login Failed", e.getMessage());
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * gets the file path of a uri (unique resource identifier), for our purposes an image id of sorts.
     *
     * @param uri the unique resource identifier of the image the user selected
     * @return the full path of the uri in question
     */
    public String getImagePath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null)
            cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }

    /**
     * @param file        the image file that we are testing/fixing the orientation of
     * @param orientation the current orientation of the image in question.
     * @return a new image file, with the orientation fixed so it is upright and not on its side - a problem in earlier iterations of this app.
     */
    public File fixOrientation(File file, int orientation) {

        //converts the file to a bitmap.
        String filePath = file.getPath();
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        //fixes orientation of bitmap if orientation == 90
        if (orientation == 90) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        }
        return convertBitmapToFile(bitmap);
    }

    /**
     * This is a utility function I use to convert a bitmap to a file - for example, to fix the orientation of an image the user selected, a file is necessary.
     *
     * @param bitmap the bitmap of the image the user selected
     * @return a file of the image the user selected
     */
    private File convertBitmapToFile(Bitmap bitmap) {
        // Create a file in the Internal Storage
        String fileName = "tempFile";
        FileOutputStream outputStream = null;
        try {
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        try {
            if (outputStream != null)
                outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File toReturn = new File(getApplicationContext().getFilesDir(), "tempFile");
        return toReturn;

    }
}
