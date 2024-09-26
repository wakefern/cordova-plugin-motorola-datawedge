package com.bluefletch.motorola;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import android.graphics.ImageFormat;

public class ImageProcessing {

    private static final String IMG_FORMAT_YUV = "YUV";
    private static final String IMG_FORMAT_Y8 = "Y8";
    private static final int IMG_FORMAT_NV21 = ImageFormat.NV21;

    // private static ImageProcessing instance = null;
    protected static String TAG = ImageProcessing.class.getSimpleName();

    // public static ImageProcessing getInstance() {

    // if (instance == null) {
    // instance = new ImageProcessing();
    // }
    // return instance;
    // }

    // private ImageProcessing() {
    // // Private Constructor
    // }

    public static String generateBitmapFromNGSimulscanObj(Context context,Cursor cursor) {
        String nextURI = cursor.getString(cursor.getColumnIndex("next_data_uri"));
        byte[] binaryData = null;
        if (nextURI.isEmpty()) { // No data chunks. All data are available in one chunk
            Log.e(TAG, "all data in 1 chunk");
            binaryData = cursor.getBlob(cursor.getColumnIndex("field_raw_data"));
        } else {
            Log.e(TAG, "more chunks");
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final String fullDataSize = cursor
                        .getString(cursor.getColumnIndex("full_data_size"));
                int bufferSize = cursor.getInt(cursor
                        .getColumnIndex("data_buffer_size"));
                baos.write(cursor.getBlob(cursor
                        .getColumnIndex("field_raw_data"))); // Read the first chunk from
                                                             // initial set
                while (!nextURI.isEmpty()) {
                    Cursor imageDataCursor = context.getContentResolver()
                            .query(Uri.parse(nextURI), null,
                                    null, null);
                    if (imageDataCursor != null) {
                        imageDataCursor.moveToFirst();
                        bufferSize += imageDataCursor.getInt(imageDataCursor.getColumnIndex("data_buffer_size"));
                        byte[] bufferData = imageDataCursor.getBlob(imageDataCursor.getColumnIndex("field_raw_data"));
                        baos.write(bufferData);
                        nextURI = imageDataCursor.getString(imageDataCursor.getColumnIndex("next_data_uri"));
                    }
                    imageDataCursor.close();

                }
                binaryData = baos.toByteArray();
                baos.close();
            } catch (final Exception ex) {

            }
        }

        int imgWidth = 0;
        int imgHeight = 0;

        String[] columnNames = cursor.getColumnNames();

        Log.e(TAG, "Fields in the cursor:");
        for (String columnName : columnNames) {
            Log.e(TAG, columnName);
        }

        imgWidth = cursor.getInt(cursor.getColumnIndex("field_image_width"));
        imgHeight = cursor.getInt(cursor.getColumnIndex("field_image_height"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuvImage = new YuvImage(binaryData, ImageFormat.NV21, imgWidth, imgHeight, null);
        yuvImage.compressToJpeg(new Rect(0, 0, imgWidth, imgHeight),  50, out);
        byte[] imageBytes = out.toByteArray();

        Log.e(TAG, "byteArray being returned");
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    public static String generateBitmapFromWorkflowObj(Context context, JSONObject jsonObject) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] binaryData = {};
        try {
            if (jsonObject.has("binaryData")) {
                binaryData = jsonObject.getString("binaryData").getBytes();
            } else {
                String uri = jsonObject.getString("uri");
                Cursor cursor = context.getContentResolver().query(Uri.parse(uri), null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    try {
                        baos.write(cursor.getBlob(cursor.getColumnIndex("raw_data")));
                        String nextURI = cursor.getString(cursor.getColumnIndex("next_data_uri"));
                        while (nextURI != null && !nextURI.isEmpty()) {
                            Cursor cursorNextData = context.getContentResolver().query(Uri.parse(nextURI),
                                    null, null, null);
                            if (cursorNextData != null) {
                                cursorNextData.moveToFirst();
                                baos.write(cursorNextData.getBlob(cursorNextData.getColumnIndex("raw_data")));
                                nextURI = cursorNextData.getString(cursorNextData.getColumnIndex("next_data_uri"));
                                cursorNextData.close();
                            }
                        }
                        binaryData = baos.toByteArray();
                        cursor.close();

                    } catch (IOException e) {

                    }
                }
            }

        } catch (JSONException e) {
            // some exception handler code. ContentResolver resolver =
            Log.e(TAG, e.toString());
        }

        int width = 0;
        int height = 0;
        int stride = 0;
        int orientation = 0;
        String imageFormat = "";
        try {
            width = jsonObject.getInt("width");
            height = jsonObject.getInt("height");
            stride = jsonObject.getInt("stride");
            orientation = jsonObject.getInt("orientation");
            imageFormat = jsonObject.getString("imageformat");
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
        }

        Log.e(TAG, binaryData.toString());
        Bitmap bitmap = getBitmap(binaryData, imageFormat, orientation, stride, width, height);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        Log.e(TAG, "byteArray being returned");
        return Base64.encodeToString(byteArray, Base64.DEFAULT);

    }

    public static Bitmap getBitmap(byte[] data, String imageFormat, int orientation, int stride, int width,
            int height) {
        if (imageFormat.equalsIgnoreCase(IMG_FORMAT_YUV)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int uvStride = ((stride + 1) / 2) * 2; // calculate UV channel stride to support odd strides
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, new int[] { stride, uvStride });
            yuvImage.compressToJpeg(new Rect(0, 0, stride, height), 100, out);
            yuvImage.getYuvData();
            byte[] imageBytes = out.toByteArray();
            if (orientation != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            } else {
                return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            }
        } else if (imageFormat.equalsIgnoreCase(IMG_FORMAT_Y8)) {
            return convertYtoJPG_CPU(data, orientation, stride, height);
        }

        return null;
    }

    private static Bitmap convertYtoJPG_CPU(byte[] data, int orientation, int stride, int height) {
        int mLength = data.length;
        int[] pixels = new int[mLength];
        for (int i = 0; i < mLength; i++) {
            int p = data[i] & 0xFF;
            pixels[i] = 0xff000000 | p << 16 | p << 8 | p;
        }
        if (orientation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            Bitmap bitmap = Bitmap.createBitmap(pixels, stride, height, Bitmap.Config.ARGB_8888);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } else {
            return Bitmap.createBitmap(pixels, stride, height, Bitmap.Config.ARGB_8888);
        }
    }
}