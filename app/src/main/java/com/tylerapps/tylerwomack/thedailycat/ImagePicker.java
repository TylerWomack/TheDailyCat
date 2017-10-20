package com.tylerapps.tylerwomack.thedailycat;

/**
 * Created by Tyler Womack on 9/27/2017.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import java.io.IOException;

/**
 * Created by Tyler Womack on 9/19/2017.
 */

public class ImagePicker extends Activity {
    Uri uri = null;
    private int PICK_IMAGE_REQUEST = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 5053252;
    private static final int MY_PERMISSIONS_REQUEST_MANAGE_DOCUMENTS = 5938281;
    public static final int KITKAT_VALUE = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imagepicker);
        //pickImage();
        test();
    }


    public void pickImage() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        //ImageView imageView = (ImageView) findViewById(R.id.imageView);
        //Picasso.with(this).load(uri).into(imageView);

        //used to load image into imageview here...
    }

    public void test() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, KITKAT_VALUE);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, KITKAT_VALUE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri myUri = data.getData();
            uri = myUri;
            Intent result = new Intent();
            result.setData(uri);
            setResult(500, result);
            finish();
        }

        if (requestCode == KITKAT_VALUE) {
            if (resultCode == Activity.RESULT_OK) {

                Uri myUri = data.getData();
                uri = myUri;

                //fuck yeah...
                int orientation = getOrientation(this, uri);

                Intent result = new Intent();
                result.putExtra("orientation", orientation);
                result.setData(uri);
                setResult(500, result);
                finish();
            }
        }
    }

    private int getOrientation(Context context, Uri photoUri) {

        String path = FileUtility.getRealPathFromURI(context, photoUri);

        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int rotationInDegrees = exifToDegrees(rotation);

        return rotationInDegrees;


        /*
        //new String[]{MediaStore.Images.ImageColumns.HEIGHT}
        //MediaStore.Images.ImageColumns.ORIENTATION
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.HEIGHT}, null, null, null);

        if (cursor.getCount() != 1) {
            cursor.close();
            return -1;
        }

        cursor.moveToFirst();
        int orientation = cursor.getInt(0);
        cursor.close();
        cursor = null;
        return orientation;

        */

    }


    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

}
