package com.addisonelliott.segmentedbutton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * Empty, transparent view used as a "dummy" or filler view
 *
 * This view has a desired size of (minimumWidth, minimumHeight) but will not expand past this size if the space is
 * available. This is contrary to View's implementation, which will expand to fill the available space.
 *
 * The difference is in the onMeasure function between View and EmptyView, specifically for the case of
 * MeasureSpec.AT_MOST.
 *
 * View
 * <pre>
 * setMeasuredDimension(getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec),
 *     getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec));
 * </pre>
 * where getDefaultSize is defined as:
 * <pre>
 * public static int getDefaultSize(int size, int measureSpec) {
 *     ...
 *         case MeasureSpec.AT_MOST:
 *         case MeasureSpec.EXACTLY:
 *             result = specSize;
 *             break;
 *     ...
 * }
 * </pre>
 *
 * Now, rather EmptyView uses resolveSize function and here is how it implements MeasureSpec.AT_MOST
 * <pre>
 * public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
 *     ...
 *         case MeasureSpec.AT_MOST:
 *             if (specSize < size) {
 *                 result = specSize | MEASURED_STATE_TOO_SMALL;
 *             } else {
 *                 result = size;
 *             }
 *             break;
 *
 *         case MeasureSpec.EXACTLY:
 *             result = specSize;
 *             break;
 *     ...
 * }
 * </pre>
 *
 * For the case of getDefaultSize for View, AT_MOST will return the maximum value but resolveSize will return UP TO
 * the max size, but it will prefer the desired value.
 */
class EmptyView extends View {

    public EmptyView(Context context) {
        super(context);
    }

    public EmptyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Desired size is the suggested minimum size
        // Resolve size based on the measure spec and go from there
        // resolveSize
        // View.onMeasure uses getDefaultSize which is similar to resolveSize except in the case of
        // MeasureSpec.AT_MOST, the maximum value will be returned. In other words, View will expand to fill the
        // available area while resolveSize will only use the desired size.
        final int widthSize = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int heightSize = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        setMeasuredDimension(widthSize, heightSize);
    }
}