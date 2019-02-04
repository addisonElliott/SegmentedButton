package com.addisonelliott.segmentedbutton;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.LinearLayout;

class RoundHelper {

    static void makeRound(View view, int dividerColor, int dividerRadius, int dividerSize) {
        GradientDrawable gradient = getGradientDrawable(dividerColor, dividerRadius, dividerSize);
        BackgroundHelper.setBackground(view, gradient);
    }

    static void makeDividerRound(LinearLayout layout, int dividerColor, int dividerRadius, int dividerSize,
            Drawable drawable) {
        GradientDrawable gradient = null;
        if (null != drawable) {
            if (drawable instanceof GradientDrawable) {
                gradient = (GradientDrawable) drawable;
                gradient.setSize(dividerSize, 0);
                gradient.setCornerRadius(dividerRadius);
            } else {
                layout.setDividerDrawable(drawable);
            }
        } else {
            gradient = getGradientDrawable(dividerColor, dividerRadius, dividerSize);
        }
        layout.setDividerDrawable(gradient);
    }

    private static GradientDrawable getGradientDrawable(int dividerColor, int dividerRadius, int dividerSize) {
        GradientDrawable gradient =
                new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{dividerColor, dividerColor});
        gradient.setShape(GradientDrawable.RECTANGLE);
        gradient.setCornerRadius(dividerRadius);
        gradient.setSize(dividerSize, 0);
        return gradient;
    }
}
