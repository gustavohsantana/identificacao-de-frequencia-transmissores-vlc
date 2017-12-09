package com.pedroalmeida.cameratest.processing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.pedroalmeida.cameratest.util.AppConstants;
import com.pedroalmeida.cameratest.util.ExifUtil;
import com.pedroalmeida.cameratest.util.ViewUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Pedro on 04/12/2017.
 */

public class ImageController {

    private static final String TAG = "PROCESSING IMAGE";

    private Context mContext;

    public ImageController(Context context) {
        mContext = context;
    }

    public String process(File file) {
        try {
            Log.d(TAG, "process: STARTING...");
            Bitmap originalPicture = BitmapFactory.decodeFile(file.getAbsolutePath());
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inScaled = false;
//        Bitmap originalPicture = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.teste, options);

            if (originalPicture == null) {
                return "ERROR";
            }

            Bitmap rotatedBitmap = ExifUtil.rotateBitmap(file.getAbsolutePath(), originalPicture);
            Bitmap pictureStep1 = getJustLightSource(rotatedBitmap);
            Log.d(TAG, "process: Step1 finish");
            Bitmap pictureStep2 = getImageBands(pictureStep1);
            Log.d(TAG, "process: Step2 finish");
            int distance = getDistanceBetweenBands2(pictureStep2);
            Log.d(TAG, "process: Step3 finish");

            //If can't found any bands on image
            if (distance == 0) {
                return "LOCALIZAÇÃO DESCONHECIDA";
            }


            double scanRate = 24000;
            double frequency = 1 / (((1 / scanRate) * distance) * 2);
            Log.d(TAG, "process: Frequency = " + frequency);

            if (frequency > 350 && frequency < 650) {
                return "SALA 18";
            } else if (frequency > 650 && frequency < 950) {
                return "SALA 10";
            } else {
                return "LOCALIZAÇÃO DESCONHECIDA";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "OCORREU UM ERRO";
        }
    }

    private Bitmap getJustLightSource(Bitmap originalPicture) {
        if (originalPicture == null) {
            return null;
        }

        Bitmap copyBitmap = originalPicture.copy(originalPicture.getConfig(), false);

        Bitmap blurImage = ViewUtils.blurImage(mContext, copyBitmap, AppConstants.BLUR_RADIUS);
        blurImage = ViewUtils.blurImage(mContext, blurImage, AppConstants.BLUR_RADIUS);

        Bitmap binarizedImage = getOTSUImage(blurImage);

        CannyEdgeDetector detector = new CannyEdgeDetector();
        detector.setLowThreshold(0.5f);
        detector.setHighThreshold(1f);
        detector.setSourceImage(binarizedImage);
        detector.process();
        Bitmap edges = detector.getEdgesImage();

        return getRegion(edges, originalPicture);
    }

    private Bitmap getImageBands(Bitmap pictureStep1) {
        if (pictureStep1 == null) {
            return null;
        }

        Bitmap binarizedImage = getOTSUImage(pictureStep1);

        CannyEdgeDetector detector = new CannyEdgeDetector();
        detector.setLowThreshold(0.5f);
        detector.setHighThreshold(1f);
        detector.setSourceImage(binarizedImage);
        detector.process();
        return detector.getEdgesImage();
    }

    private int getDistanceBetweenBands(Bitmap pictureStep2) {

        int width = pictureStep2.getWidth();
        int height = pictureStep2.getHeight();

        int count = 0;
        int pixelNum = 0;
        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < height; i += 15) {
            for (int j = 0; j < width; j++) {
                //get pixel value
                int p = pictureStep2.getPixel(j, i);

                //get red
                int r = (p >> 16) & 0xff;
                //get green
                int g = (p >> 8) & 0xff;
                //get blue
                int b = p & 0xff;

                int lum = Math.round(0.299f * r + 0.587f * g + 0.114f * b);
                if (lum != 0) {
                    count++;
                }

                if (count != 0) {
                    pixelNum++;
                }

                if (count > 1 && lum >= 100) {
                    if (pixelNum > 1 && pixelNum < 100) {
                        list.add(pixelNum);
                    }

                    pixelNum = 0;
                }
            }
        }

        // return Moda ( the distance most appears )
        int mostPopNumber = 0;
        int tmpLastCount = 0;

        for (int i = 0; i < list.size() - 1; i++) {
            int tmpActual = list.get(i);
            int tmpCount = 0;
            for (int j = 0; j < list.size(); j++) {
                if (tmpActual == list.get(j)) {
                    tmpCount++;
                }
            }
            // >= for the last one
            if (tmpCount > tmpLastCount) {
                tmpLastCount = tmpCount;
                mostPopNumber = tmpActual;
            }
        }

        Log.d(TAG, "Distancia encontrada: " + mostPopNumber + "px");
        Log.d(TAG, "Interceptacoes: " + count);
        return mostPopNumber;
    }

    private int getDistanceBetweenBands2(Bitmap pictureStep2) {
        if (pictureStep2 == null) {
            return 0;
        }

        int width = pictureStep2.getWidth();
        int height = pictureStep2.getHeight();

        List<Integer> listDistance = new ArrayList<>();

        for (int i = 0; i < height; i += 15) {
            int pxInitial = -1;

            for (int j = 0; j < width; j++) {
                //get pixel value
                int p = pictureStep2.getPixel(j, i);
                int luminosidade = getLuminosidade(p);

                if (luminosidade > 100) {
                    if (pxInitial == -1) {
                        pxInitial = j;
                    } else {
                        listDistance.add(j - pxInitial);

                        pxInitial = -1;
                    }
                }
            }
        }

        // return Moda ( the distance most appears )
        int mostPopNumber = 0;
        int tmpLastCount = 0;

        for (int distance : listDistance) {
            int tmpCount = 0;
            for (int j = 0; j < listDistance.size(); j++) {
                if (distance == listDistance.get(j)) {
                    tmpCount++;
                }
            }
            // >= for the last one
            if (tmpCount > tmpLastCount) {
                tmpLastCount = tmpCount;
                mostPopNumber = distance;
            }
        }

        Log.d(TAG, "Distancia encontrada: " + mostPopNumber + "px");
        return mostPopNumber;
    }

    private int getLuminosidade(int px) {
        //get red
        int r = (px >> 16) & 0xff;
        //get green
        int g = (px >> 8) & 0xff;
        //get blue
        int b = px & 0xff;

        return Math.round(0.299f * r + 0.587f * g + 0.114f * b);
    }

    @NonNull
    private Bitmap getOTSUImage(Bitmap sourceImage) {
        Bitmap binarizedImage = convertToMutable(sourceImage);
        // I will look at each pixel and use the function shouldBeBlack to decide
        // whether to make it black or otherwise white
        for (int i = 0; i < binarizedImage.getWidth(); i++) {
            for (int c = 0; c < binarizedImage.getHeight(); c++) {
                int pixel = binarizedImage.getPixel(i, c);
                if (shouldBeBlack(pixel))
                    binarizedImage.setPixel(i, c, Color.BLACK);
                else
                    binarizedImage.setPixel(i, c, Color.WHITE);
            }
        }
        return binarizedImage;
    }

    private Bitmap getRegion(Bitmap edges, Bitmap originalImage) {

        int width = edges.getWidth();
        int height = edges.getHeight();

        int maxXCoordinate = 0;
        int minXCoordinate = 9999;
        int maxYCoordinate = 0;
        int minYCoordinate = 9999;

        for (int j = 0; j < width; j++) {
            for (int i = 0; i < height; i++) {
                //get pixel value
                int p = edges.getPixel(j, i);
                int lum = getLuminosidade(p);

                if (lum >= 100) {
                    if (j > maxXCoordinate)
                        maxXCoordinate = j;
                    if (j < minXCoordinate)
                        minXCoordinate = j;
                    if (i > maxYCoordinate)
                        maxYCoordinate = i;
                    if (i < minYCoordinate)
                        minYCoordinate = i;
                }
            }
        }

        Log.d(TAG, "XMIN: " + minXCoordinate);
        Log.d(TAG, "XMAX: " + maxXCoordinate);
        Log.d(TAG, "YMIN: " + minYCoordinate);
        Log.d(TAG, "YMAX: " + maxYCoordinate);

        int x;
        int y;
        int widthOut = maxXCoordinate - minXCoordinate;
        int heightOut = maxYCoordinate - minYCoordinate;

        x = (minXCoordinate - 10) > 0 ? (minXCoordinate - 10) : minXCoordinate;
        y = (minYCoordinate - 10) > 0 ? (minYCoordinate - 10) : minYCoordinate;
        widthOut = (maxXCoordinate + 20) > width ? widthOut : (widthOut + 20);
        heightOut = (maxYCoordinate + 20) > height ? heightOut : (heightOut + 20);

        if (widthOut <= 0 || heightOut <= 0) {
            return null;
        }

        return Bitmap.createBitmap(originalImage, x, y, widthOut, heightOut);

//        return edges.getSubimage(minYCordernate, minXCordernate,(maxYCordenate-minYCordernate), (maxXCordenate-minXCordernate));
    }

    /**
     * @param imgIn - Source image. It will be released, and should not be used more
     * @return a copy of imgIn, but muttable.
     * @author Derzu
     * @see "stackoverflow.com/a/9194259/833622"
     * <p>
     * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
     * more memory that there is already allocated.
     */
    private static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }

    /**
     * @param pixel the pixel that we need to decide on
     * @return boolean indicating whether this pixel should be black
     */
    private static boolean shouldBeBlack(int pixel) {
        int alpha = Color.alpha(pixel);
        int redValue = Color.red(pixel);
        int blueValue = Color.blue(pixel);
        int greenValue = Color.green(pixel);
        if (alpha == 0x00) //if this pixel is transparent let me use TRASNPARENT_IS_BLACK
            return AppConstants.TRASNPARENT_IS_BLACK;
        // distance from the white extreme
        double distanceFromWhite = Math.sqrt(Math.pow(0xff - redValue, 2) + Math.pow(0xff - blueValue, 2) + Math.pow(0xff - greenValue, 2));
        // distance from the black extreme //this should not be computed and might be as well a function of distanceFromWhite and the whole distance
        double distanceFromBlack = Math.sqrt(Math.pow(0x00 - redValue, 2) + Math.pow(0x00 - blueValue, 2) + Math.pow(0x00 - greenValue, 2));
        // distance between the extremes //this is a constant that should not be computed :p
        double distance = distanceFromBlack + distanceFromWhite;
        // distance between the extremes
        return ((distanceFromWhite / distance) > AppConstants.SPACE_BREAKING_POINT);
    }

}
