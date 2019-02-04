package com.addisonelliott.segmentedbutton;

import android.content.Context;

class ConversionHelper {

    static float pxToDp(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    static int dpToPx(final Context context, final float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    static float spToPx(final Context context, int size) {
        return size * context.getResources().getDisplayMetrics().scaledDensity;
    }
}
