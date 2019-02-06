package com.addisonelliott.segmentedbutton;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Purpose of this class is to fill the height of the parent
 *
 * For some reason this doesnt happen for a view inside FrameLayout so this is used instead
 * TODO Explain better
 */
class BackgroundView extends View {

    public BackgroundView(Context context) {
        super(context);
    }

    public BackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Desired size is the suggested minimum size
        // Resolve size based on the measure spec and go from there
        final int widthSize = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int heightSize = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);
    }
}
