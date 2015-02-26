package com.ioiometer;

import android.content.res.Resources;

/**
 * Abstract helper class that provides some useful static methods to convert UI units.
 * @author Johannes Rieke
 */
public abstract class Helper {

    public static float dpToPx(float dp) {
        return (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    public static float pxToDp(float px) {
        return (px / Resources.getSystem().getDisplayMetrics().density);
    }

    public static float spToPx(float sp) {
        return (sp * Resources.getSystem().getDisplayMetrics().scaledDensity);
    }

    public static float pxToSp(float px) {
        return (px / Resources.getSystem().getDisplayMetrics().scaledDensity);
    }

}
