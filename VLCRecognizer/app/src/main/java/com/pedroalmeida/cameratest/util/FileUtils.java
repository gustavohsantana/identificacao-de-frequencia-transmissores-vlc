package com.pedroalmeida.cameratest.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

/**
 * Created by Pedro on 22/08/2017.
 */

public abstract class FileUtils {

    public static String savePicture(final Context context, final Uri uri) {
        InputStream in = null;
        OutputStream out = null;
        String path = null;
        try {
            final int chunkSize = 1024;  // We'll read in one kB at a time
            byte[] imageData = new byte[chunkSize];

            String root = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
            File createDir = new File(root + "iSystem" + File.separator);
            if (!createDir.exists()) {
                if (!createDir.mkdir()) {
                    return null;
                }
            }
            File file = new File(root + "iSystem" + File.separator + "wallpaper.jpg");

            file.createNewFile();

            in = context.getContentResolver().openInputStream(uri);
            out = new FileOutputStream(file);  // I'm assuming you already have the File object for where you're writing to

            int bytesRead;
            while ((bytesRead = in.read(imageData)) > 0) {
                out.write(Arrays.copyOfRange(imageData, 0, Math.max(0, bytesRead)));
            }

            path = file.getPath();
        } catch (Exception ex) {
            Log.e("FileUtils", ex.getMessage());
            return null;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }

                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return path;
    }


    public static Bitmap decodeSampledBitmapFromStorage(final Context context, String pathName/*,
                                                        int reqWidth, int reqHeight*/) {

        int reqWidth = ViewUtils.getScreenWidth(context);
        int reqHeight = ViewUtils.getScreenHeight(context);

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pathName, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth,
                                             int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }


}
