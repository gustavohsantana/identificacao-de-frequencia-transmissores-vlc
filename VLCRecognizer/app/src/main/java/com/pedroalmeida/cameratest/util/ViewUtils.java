package com.pedroalmeida.cameratest.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.Display;
import android.view.WindowManager;

/**
 * Created by Pedro on 10/08/2017.
 */

public abstract class ViewUtils {

//    public static void showSnackbar(final View contentView, final String message) {
//        Snackbar snackbar = Snackbar.make(contentView, message, Snackbar.LENGTH_LONG);
//
//        View view = snackbar.getView();
//        TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
//        tv.setTextColor(ContextCompat.getColor(contentView.getContext(), R.color.black));
//        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
//        tv.setTypeface(null, Typeface.ITALIC);
//        view.setBackgroundColor(ContextCompat.getColor(contentView.getContext(), R.color.colorAccent));
//
//        snackbar.show();
//    }

//    public static void showAlertDialog(final Context context, final String message, DialogInterface.OnClickListener yesAction, DialogInterface.OnClickListener noAction) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setMessage(message);
//
//        if (yesAction != null) {
//            builder.setPositiveButton(context.getString(R.string.yes), yesAction);
//        } else {
//            builder.setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                }
//            });
//        }
//
//        if (noAction != null) {
//            builder.setPositiveButton(context.getString(R.string.yes), noAction);
//        } else {
//            builder.setNegativeButton(context.getString(R.string.no), new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int which) {
//                    dialog.dismiss();
//                }
//            });
//        }
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }

    public static int getScreenHeight(final Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    public static int getScreenWidth(final Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    public static int convertDpToPixel(Context context, int dps) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    public static Bitmap blurImage(final Context context, final Bitmap image, float blurRadius) {
        if (null == image) return null;

        Bitmap outputBitmap = Bitmap.createBitmap(image);
        final RenderScript renderScript = RenderScript.create(context);
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, image);
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, outputBitmap);

        //Intrinsic Gausian blur filter
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        theIntrinsic.setRadius(blurRadius);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(outputBitmap);
        return outputBitmap;
    }
}
