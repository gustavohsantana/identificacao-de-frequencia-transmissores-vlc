package com.pedroalmeida.cameratest.util;

/**
 * Created by Pedro on 04/12/2017.
 */

public class AppConstants {

    /* BLUR CONSTANT */
    public static final float BLUR_RADIUS = 25;

    /**
     * Boolean that tells me how to treat a transparent pixel (Should it be black?)
     */
    public static final boolean TRASNPARENT_IS_BLACK = false;

    /**
     * This is a point that will break the space into Black or white
     * In real words, if the distance between WHITE and BLACK is D;
     * then we should be this percent far from WHITE to be in the black region.
     * Example: If this value is 0.5, the space is equally split.
     */
    public static final double SPACE_BREAKING_POINT = 0.7;
}
