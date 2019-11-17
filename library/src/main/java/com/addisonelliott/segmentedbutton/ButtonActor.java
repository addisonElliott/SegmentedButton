package com.addisonelliott.segmentedbutton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;

/**
 * Empty, transparent view used as a "dummy" or filler view
 *
 * This view is used as spacers for the SegmentedButtonGroup LinearLayout that handles the dividers
 *
 * Each view has a model SegmentedButton that it mimics the width & height of. The width & height is adjusted for the
 * divider width to appropriate position the dividers in between the two buttons.
 */
class ButtonActor extends View
{
    // Button to mimics size of
    private SegmentedButton button = null;

    // Divider width
    private int dividerWidth = 0;

    public ButtonActor(Context context)
    {
        super(context);
    }

    public ButtonActor(Context context, @Nullable AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ButtonActor(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public void setButton(SegmentedButton button)
    {
        this.button = button;
    }

    public void setDividerWidth(int width)
    {
        dividerWidth = width;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // Desired size is the suggested minimum size
        // Resolve size based on the measure spec and go from there
        // resolveSize
        // View.onMeasure uses getDefaultSize which is similar to resolveSize except in the case of
        // MeasureSpec.AT_MOST, the maximum value will be returned. In other words, View will expand to fill the
        // available area while resolveSize will only use the desired size.

        // Width and height to set the view to
        int width;
        int height;

        if (button != null)
        {
            // Calculate the amount to offset the model buttons parent width by
            //
            // (dividerWidth / 2) is subtracted from the width for each divider present
            // Buttons with neighboring buttons on both sides, this adds up to dividerWidth
            // Left-most and right-most buttons only have one divider, so this is (dividerWidth / 2)
            // And if there is EXACTLY one button being shown, no dividers are present and so the offset is 0
            int widthOffset;
            if (button.isLeftButton() && button.isRightButton())
                widthOffset = 0;
            else if (button.isLeftButton() || button.isRightButton())
                widthOffset = dividerWidth / 2;
            else
                widthOffset = dividerWidth;

            // Calculate the width & height based on the desired width & height and the measure specs
            width = resolveSize(button.getMeasuredWidth() - widthOffset, widthMeasureSpec);
            height = resolveSize(button.getMeasuredHeight(), heightMeasureSpec);
        }
        else
        {
            // Fallback option to calculate the size based on suggested minimum width & height
            width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
            height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        }

        setMeasuredDimension(width, height);
    }
}
