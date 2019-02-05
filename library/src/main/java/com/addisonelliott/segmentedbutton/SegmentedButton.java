package com.addisonelliott.segmentedbutton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

public class SegmentedButton extends View {

    // region Variables & Constants
    private static final String TAG = "SegmentedButton";

    private float mClipAmount;
    private boolean clipLeftToRight;

    private TextPaint mTextPaint;
    private StaticLayout mStaticLayout;
    private int mTextMaxWidth;
    private Rect mTextBounds = new Rect();
    private int mRadius, mBorderSize;
    private boolean hasBorderLeft, hasBorderRight;

    // private RectF rectF = new RectF();
    private RectF mRectF;
    private Paint mPaint;

    private PointF textPosition, drawablePosition;

    private PorterDuffColorFilter mBitmapNormalColor, mBitmapClipColor;

    private Drawable mDrawable;

    private boolean hasDrawable, hasText;
    private int drawableGravity;

    // Custom attributes
    private int drawableTintOnSelection, textColorOnSelection, textColor, rippleColor, buttonWidth, drawable,
            drawableTint, drawableWidth, drawableHeight, drawablePadding;
    private boolean hasTextColorOnSelection, hasRipple, hasWidth, hasWeight, hasDrawableTintOnSelection,
            hasDrawableWidth, hasDrawableHeight, hasDrawableTint;
    private float buttonWeight, textSize;
    private String text;
    private Typeface textTypeface;

    // endregion

    // region Constructor

    public SegmentedButton(Context context) {
        super(context);

        init(context, null);
    }

    public SegmentedButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public SegmentedButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        // Retrieve custom attributes
        getAttributes(context, attrs);

        initText();
        initDrawable(context);

        textPosition = new PointF();
        drawablePosition = new PointF();

        mRectF = new RectF();
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
    }

    private void getAttributes(Context context, @Nullable AttributeSet attrs) {
        // According to docs for obtainStyledAttributes, attrs can be null and I assume that each value will be set
        // to the default
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SegmentedButton);

        hasRipple = ta.hasValue(R.styleable.SegmentedButton_rippleColor);
        rippleColor = ta.getColor(R.styleable.SegmentedButton_rippleColor, 0);

        hasDrawable = ta.hasValue(R.styleable.SegmentedButton_drawable);
        drawable = ta.getResourceId(R.styleable.SegmentedButton_drawable, 0);
        drawablePadding = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawablePadding, 0);
        hasDrawableTint = ta.hasValue(R.styleable.SegmentedButton_drawableTint);
        drawableTint = ta.getColor(R.styleable.SegmentedButton_drawableTint, -1);
        hasDrawableTintOnSelection = ta.hasValue(R.styleable.SegmentedButton_drawableTint_onSelection);
        drawableTintOnSelection = ta.getColor(R.styleable.SegmentedButton_drawableTint_onSelection, Color.WHITE);
        hasDrawableWidth = ta.hasValue(R.styleable.SegmentedButton_drawableWidth);
        hasDrawableHeight = ta.hasValue(R.styleable.SegmentedButton_drawableHeight);
        drawableWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawableWidth, -1);
        drawableHeight = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawableHeight, -1);
        // TODO No drawable keep aspect ratio?
        drawableGravity = ta.getInteger(R.styleable.SegmentedButton_drawableGravity, Gravity.LEFT);

        hasText = ta.hasValue(R.styleable.SegmentedButton_text);
        text = ta.getString(R.styleable.SegmentedButton_text);
        textColor = ta.getColor(R.styleable.SegmentedButton_textColor, Color.GRAY);
        hasTextColorOnSelection = ta.hasValue(R.styleable.SegmentedButton_textColor_onSelection);
        textColorOnSelection = ta.getColor(R.styleable.SegmentedButton_textColor_onSelection, Color.WHITE);
        textSize = ta.getDimension(R.styleable.SegmentedButton_textSize, ConversionHelper.spToPx(getContext(), 14));

        boolean hasFontFamily = ta.hasValue(R.styleable.SegmentedButton_android_fontFamily);
        int textStyle = ta.getInt(R.styleable.SegmentedButton_textStyle, Typeface.NORMAL);

        // If a font family is present then load typeface with textstyle from that
        if (hasFontFamily) {
            // Note: TypedArray.getFont is used for Android O & above while ResourcesCompat.getFont is used for below
            // Experienced an odd bug in the design viewer of Android Studio where it would not work with only using
            // the ResourcesCompat.getFont function. Unsure of the reason but this fixes it
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                textTypeface = Typeface.create(ta.getFont(R.styleable.SegmentedButton_android_fontFamily),
                        textStyle);
            } else {
                int fontFamily = ta.getResourceId(R.styleable.SegmentedButton_android_fontFamily, 0);

                textTypeface = Typeface.create(ResourcesCompat.getFont(context, fontFamily), textStyle);
            }
        } else {
            textTypeface = Typeface.create((Typeface) null, textStyle);
        }

        try {
            hasWeight = ta.hasValue(R.styleable.SegmentedButton_android_layout_weight);
            buttonWeight = ta.getFloat(R.styleable.SegmentedButton_android_layout_weight, 0);
            buttonWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButton_android_layout_width, 0);

        } catch (Exception ex) {
            hasWeight = true;
            buttonWeight = 1;
        }

        hasWidth = !hasWeight && buttonWidth > 0;

        ta.recycle();
    }

    private void initText() {
        // If there is no text then do not bother
        if (!hasText) {
            return;
        }

        // Create text paint that will be used to draw the text on the canvas
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(textColor);
        mTextPaint.setTypeface(textTypeface);

        // TODO Look into making this onMeasure probably
        // Initial kickstart to setup the text layout by assuming the text will be all in one line
        mTextMaxWidth = (int) mTextPaint.measureText(text);
        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            mStaticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), mTextPaint, mTextMaxWidth).build();
        } else {
            mStaticLayout = new StaticLayout(text, mTextPaint, mTextMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0,
                    false);
        }
    }

    private void initDrawable(Context context) {
        if (hasDrawable) {
            mDrawable = ContextCompat.getDrawable(context, drawable);
        }

        if (hasDrawableTint) {
            mBitmapNormalColor = new PorterDuffColorFilter(drawableTint, PorterDuff.Mode.SRC_IN);
        }

        if (hasDrawableTintOnSelection) {
            mBitmapClipColor = new PorterDuffColorFilter(drawableTintOnSelection, PorterDuff.Mode.SRC_IN);
        }
    }

    // endregion

    // region Layout & Measure

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        // For the text width, assume that it is in a single line with no wrapping which would be mTextMaxWidth
        // This variable is used to calculate the desired width and the desire is for it all to be in a single line
        final int drawableWidth = hasDrawable ? hasDrawableWidth ? this.drawableWidth : mDrawable.getIntrinsicWidth()
                : 0;
        final int textWidth = hasText ? mTextMaxWidth : 0;

        // Measured width & height
        int width = 0;
        int height = 0;

        // Desired width will always have left & right padding regardless of horizontal/vertical gravity for the
        // drawable and text.
        int desiredWidth = getPaddingLeft() + getPaddingRight();

        if (Gravity.isHorizontal(drawableGravity)) {
            // When drawable and text are inline horizontally, then the total desired width is:
            //     padding left + text width (assume one line) + drawable padding + drawable width + padding right
            desiredWidth += textWidth + drawablePadding + drawableWidth;
        } else {
            // When drawable and text are on top of each other, the total desired width is:
            //     padding left + max(text width, drawable width) + padding right
            desiredWidth += Math.max(textWidth, drawableWidth);
        }

        // Set the measured width based on width mode
        // EXACTLY means set it to exactly the given size, AT_MOST means to set it to the desired width but dont let
        // it exceed the given size, and UNSPECIFIED means to set it to the desired width
        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                width = widthSize;
                break;

            case MeasureSpec.AT_MOST:
                width = Math.min(desiredWidth, widthSize);
                break;

            case MeasureSpec.UNSPECIFIED:
                width = desiredWidth;
                break;
        }

        // With width calculated, recalculate the text parameters to get new height (wrapping may occur)
        measureTextWidth(width, drawableWidth);

        // Repeat measuring process for height now
        // Note that the height is the static layout height which may or may not be multi-lined
        final int drawableHeight = hasDrawable ? hasDrawableHeight ? this.drawableHeight
                : mDrawable.getIntrinsicHeight() : 0;
        final int textHeight = hasText ? mStaticLayout.getHeight() : 0;

        int desiredHeight = getPaddingTop() + getPaddingBottom();

        if (Gravity.isHorizontal(drawableGravity)) {
            // When drawable and text are horizontal, the total desired height is:
            //     padding left + max(text width, drawable width) + padding right
            desiredHeight += Math.max(textHeight, drawableHeight);
        } else {
            // When drawable and text are vertical, then the total desired height is:
            //     padding left + text width (assume one line) + drawable padding + drawable width + padding right
            desiredHeight += textHeight + drawablePadding + drawableHeight;
        }

        // Set the measured height based on height mode
        // EXACTLY means set it to exactly the given size, AT_MOST means to set it to the desired height but dont let
        // it exceed the given size, and UNSPECIFIED means to set it to the desired height
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                height = heightSize;
                break;

            case MeasureSpec.AT_MOST:
                height = Math.min(desiredHeight, widthSize);
                break;

            case MeasureSpec.UNSPECIFIED:
                height = desiredHeight;
                break;
        }

        Log.d(TAG, String.format("onMeasure called with mode (%d, %d) and size (%d, %d). Desired size (%d, %d) "
                        + "Resulting size (%d, %d).", widthMode, heightMode, widthSize, heightSize, desiredWidth,
                desiredHeight, width, height));

        // Calculate the position for text & drawable now that we know width & height
        // useDesired[Width|Height] parameter is used to indicate whether there is extra space (excluding padding)
        // between the drawable or text size and the measured size. If true, this will center the object(s)
        // appropriately
        calculatePositions(width, height, textWidth, textHeight, drawableWidth, drawableHeight, desiredWidth >= width,
                desiredHeight >= height);

        // Required to be called to notify the View of the width & height decided
        setMeasuredDimension(width, height);
    }

    // Measures the text width given entire width of the segmented button
    private void measureTextWidth(int width, int drawableWidth) {
        // If there is no text, then we don't need to do anything
        if (!hasText) {
            return;
        }

        // Set drawable width to be the drawable width if the drawable has horizontal gravity, otherwise the drawable
        // width doesnt matter
        // Text width is equal to the total width minus padding and drawable width
        // But, if the maximum text width is smaller, just use that and we will manually pad it later
        int newDrawableWidth = Gravity.isHorizontal(drawableGravity) ? drawableWidth : 0;
        int textWidth = Math.min(width - getPaddingLeft() - getPaddingRight() - newDrawableWidth, mTextMaxWidth);

        // Odd case where there is not enough space for the padding and drawable width so we just return
        if (textWidth < 0) {
            return;
        }

        // Create new static layout with width
        // Old way of creating static layout was deprecated but I dont think there is any speed difference between
        // the two
        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            mStaticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), mTextPaint, textWidth).build();
        } else {
            mStaticLayout = new StaticLayout(text, mTextPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0,
                    false);
        }
    }

    // Calculate X/Y positions of the drawable and text objects
    private void calculatePositions(int measuredWidth, int measuredHeight, int textWidth, int textHeight,
            int drawableWidth, int drawableHeight, boolean useDesiredWidth, boolean useDesiredHeight) {
        // Calculates the X/Y positions of the text and drawable now that the measured size is known
        if (Gravity.isHorizontal(drawableGravity)) {
            // Calculate Y position for horizontal gravity, i.e. center the drawable and/or text if necessary
            // Fancy way of centering the two objects vertically, the last 2 if statements are special cases where
            // either the drawable or text is taking up the full height so there is no need to calculate the center
            if (!useDesiredHeight) {
                textPosition.y =
                        getPaddingTop() + (measuredHeight - getPaddingTop() - getPaddingBottom() - textHeight) / 2.0f;
                drawablePosition.y = getPaddingTop()
                        + (measuredHeight - getPaddingTop() - getPaddingBottom() - drawableHeight) / 2.0f;
            } else if (textHeight < drawableHeight) {
                textPosition.y = getPaddingTop() + (drawableHeight - textHeight) / 2.0f;
                drawablePosition.y = getPaddingTop();
            } else {
                textPosition.y = getPaddingTop();
                drawablePosition.y = getPaddingTop() + (textHeight - drawableHeight) / 2.0f;
            }

            // Calculate the starting X position with horizontal gravity
            // If the exact amount of width is used (meaning useDesiredWidth is true), then the start position is set
            // to be the left padding. Otherwise, the start position is half of the remaining space to center it
            final float startPosition = useDesiredWidth ? getPaddingLeft() :
                    (measuredWidth - textWidth - drawableWidth - drawablePadding) / 2.0f;

            // Position the drawable & text based on the gravity
            if (drawableGravity == Gravity.LEFT) {
                textPosition.x = startPosition + drawableWidth + drawablePadding;
                drawablePosition.x = startPosition;
            } else if (drawableGravity == Gravity.RIGHT) {
                textPosition.x = startPosition;
                drawablePosition.x = startPosition + textWidth + drawablePadding;
            }

            Log.d(TAG, String.format("calculatePositions called with horizontal gravity. drawableSize: (%d, %d), "
                            + "textSize: (%d, %d), padding (%d, %d, %d, %d), drawablePadding: %d, remainingSpace %f, "
                            + "textPosition (%f, %f), drawablePosition (%f, %f)", drawableWidth, drawableHeight, textWidth,
                    textHeight, getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom(),
                    drawablePadding, startPosition, textPosition.x, textPosition.y, drawablePosition.x,
                    drawablePosition.y));
        } else {
            // Calculate X position for vertical gravity, i.e. center the drawable and/or text horizontally if necessary
            // Fancy way of centering the two objects horizontally, the last 2 if statements are special cases where
            // either the drawable or text is taking up the full height so there is no need to calculate the center
            if (!useDesiredWidth) {
                textPosition.x = getPaddingLeft()
                        + (measuredWidth - getPaddingLeft() - getPaddingRight() - textWidth) / 2.0f;
                drawablePosition.x = getPaddingLeft()
                        + (measuredWidth - getPaddingLeft() - getPaddingRight() - drawableWidth) / 2.0f;
            } else if (textWidth < drawableWidth) {
                textPosition.x = getPaddingLeft() + (drawableWidth - textWidth) / 2.0f;
                drawablePosition.x = getPaddingLeft();
            } else {
                textPosition.x = getPaddingLeft();
                drawablePosition.x = getPaddingLeft() + (textWidth - drawableWidth) / 2.0f;
            }

            // Calculate the starting Y position with vertical gravity
            // If the exact amount of height is used (meaning useDesiredHeight is true), then the start position is set
            // to be the top padding. Otherwise, the start position is half of the remaining space to center it
            final float startPosition = useDesiredHeight ? getPaddingTop()
                    : (measuredHeight - textHeight - drawableHeight - drawablePadding) / 2.0f;

            // Position the drawable & text based on the gravity
            if (drawableGravity == Gravity.TOP) {
                textPosition.y = startPosition + drawableHeight + drawablePadding;
                drawablePosition.y = startPosition;
            } else if (drawableGravity == Gravity.BOTTOM) {
                textPosition.y = startPosition;
                drawablePosition.y = startPosition + textHeight + drawablePadding;
            }

            Log.d(TAG, String.format("calculatePositions called with vertical gravity. drawableSize: (%d, %d), "
                            + "textSize: (%d, %d), padding (%d, %d, %d, %d), drawablePadding: %d, remainingSpace %f, "
                            + "textPosition (%f, %f), drawablePosition (%f, %f)", drawableWidth, drawableHeight, textWidth,
                    textHeight, getPaddingLeft(), getPaddingRight(), getPaddingTop(), getPaddingBottom(),
                    drawablePadding, startPosition, textPosition.x, textPosition.y, drawablePosition.x,
                    drawablePosition.y));
        }

        if (hasDrawable) {
            mDrawable.setBounds((int) drawablePosition.x, (int) drawablePosition.y,
                    (int) drawablePosition.x + drawableWidth, (int) drawablePosition.y + drawableHeight);
        }
    }

    // endregion

    // region Drawing

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();
        final int height = getHeight();

        // Draw background
//        canvas.save();
//        mRectF.set(0.0f, 0.0f, width, height);
//        canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaint);
//        canvas.restore();

        // Draw text (unselected)
        if (hasText) {
            canvas.save();
            canvas.translate(textPosition.x, textPosition.y);
            mTextPaint.setColor(textColor);
            mStaticLayout.draw(canvas);
            canvas.restore();
        }

        // Draw drawable (unselected)
        if (hasDrawable) {
            canvas.save();
            mDrawable.setColorFilter(mBitmapNormalColor);
            mDrawable.draw(canvas);
            canvas.restore();
        }

//        int width = canvas.getWidth();
//        int height = canvas.getHeight();
//
//        canvas.save();
//
//        if (clipLeftToRight) {
//            canvas.translate(-width * (mClipAmount - 1), 0);
//        } else {
//            canvas.translate(width * (mClipAmount - 1), 0);
//        }
//
//        mRectF.set(hasBorderLeft ? mBorderSize : 0, mBorderSize, hasBorderRight ? width - mBorderSize : width,
//                height - mBorderSize);
//        canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaint);
//
//        canvas.restore();
//
//        canvas.save();
//
//        if (hasText) {
//            canvas.translate(text_X, text_Y);
//            if (hasTextColorOnSelection) {
//                mTextPaint.setColor(textColor);
//            }
//            mStaticLayout.draw(canvas);
//
//            canvas.restore();
//        }
//        canvas.save();
//
//        // Bitmap normal
//        if (hasDrawable) {
//            drawDrawableWithColorFilter(canvas, mBitmapNormalColor);
//        }
//        // NORMAL -end
//
//        // CLIPPING
//        if (clipLeftToRight) {
//            canvas.clipRect(width * (1 - mClipAmount), 0, width, height);
//        } else {
//            canvas.clipRect(0, 0, width * mClipAmount, height);
//        }
//
//        // CLIP -start
//        // Text clip
//        canvas.save();
//
//        if (hasText) {
//            canvas.translate(text_X, text_Y);
//            if (hasTextColorOnSelection) {
//                mTextPaint.setColor(textColorOnSelection);
//            }
//            mStaticLayout.draw(canvas);
//            canvas.restore();
//        }
//
//        // Bitmap clip
//        if (hasDrawable) {
//            drawDrawableWithColorFilter(canvas, mBitmapClipColor);
//        }
//        // CLIP -end
//
//        canvas.restore();
    }

    public void clipToLeft(float clip) {
        clipLeftToRight = false;
        mClipAmount = 1.0f - clip;
        invalidate();
    }

    public void clipToRight(float clip) {
        clipLeftToRight = true;
        mClipAmount = clip;
        invalidate();
    }

    // endregion

    // region Unused

//    /**
//     * Typeface.NORMAL: 0
//     * Typeface.BOLD: 1
//     * Typeface.ITALIC: 2
//     * Typeface.BOLD_ITALIC: 3
//     *
//     * @param typeface you can use above variations using the bitwise OR operator
//     */
//    public void setTypeface(Typeface typeface) {
//        mTextPaint.setTypeface(typeface);
//    }
//
//    /**
//     * @param location is .ttf file's path in assets folder. Example: 'fonts/my_font.ttf'
//     */
//    public void setTypeface(String location) {
//        if (null != location && !location.equals("")) {
//            Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), location);
//            mTextPaint.setTypeface(typeface);
//        }
//    }
//
//    void setSelectorColor(int color) {
//        mPaint.setColor(color);
//    }
//
//    void setSelectorRadius(int radius) {
//        mRadius = radius;
//    }
//
//    void setBorderSize(int borderSize) {
//        mBorderSize = borderSize;
//    }
//
//    void hasBorderLeft(boolean hasBorderLeft) {
//        this.hasBorderLeft = hasBorderLeft;
//    }
//
//    void hasBorderRight(boolean hasBorderRight) {
//        this.hasBorderRight = hasBorderRight;
//    }
//
//    /**
//     * Sets button's drawable by given drawable object and its position
//     *
//     * @param resId is your drawable's resource id
//     */
//    public void setDrawable(int resId) {
//        setDrawable(ContextCompat.getDrawable(getContext(), resId));
//    }
//
//    /**
//     * Sets button's drawable by given drawable object and its position
//     *
//     * @param drawable is your drawable object
//     */
//    public void setDrawable(Drawable drawable) {
//        mDrawable = drawable;
//        hasDrawable = true;
//        requestLayout();
//    }
//
//    /**
//     * Sets button's drawable by given drawable id and its position
//     *
//     * @param gravity specifies button's drawable position relative to text position.
//     *                These values can be given to position:
//     *                {DrawableGravity.LEFT} sets drawable to the left of button's text
//     *                {DrawableGravity.TOP} sets drawable to the top of button's text
//     *                {DrawableGravity.RIGHT} sets drawable to the right of button's text
//     *                {DrawableGravity.BOTTOM} sets drawable to the bottom of button's text
//     */
//    public void setGravity(DrawableGravity gravity) {
//        drawableGravity = gravity;
//    }
//
//    /**
//     * removes drawable's tint
//     */
//    public void removeDrawableTint() {
//        hasDrawableTint = false;
//    }
//
//    public void removeDrawableTintOnSelection() {
//        hasDrawableTintOnSelection = false;
//    }
//
//    public void removeTextColorOnSelection() {
//        hasTextColorOnSelection = false;
//    }
//
//    /**
//     * If button has any drawable, it sets drawable's tint color without changing drawable's position.
//     *
//     * @param color is used to set drawable's tint color
//     */
//    public void setDrawableTint(int color) {
//        drawableTint = color;
//    }
//
//    /**
//     * @return button's current ripple color
//     */
//    public int getRippleColor() {
//        return rippleColor;
//    }
//
//    /**
//     * @return true if the button has a ripple effect
//     */
//    public boolean hasRipple() {
//        return hasRipple;
//    }
//
//    /**
//     * @return button's text color when selector is on the button
//     */
//    public int getTextColorOnSelection() {
//        return textColorOnSelection;
//    }
//
//    /**
//     * @param textColorOnSelection set button's text color when selector is on the button
//     */
//    public void setTextColorOnSelection(int textColorOnSelection) {
//        this.textColorOnSelection = textColorOnSelection;
//    }
//
//    /**
//     * @return drawable's tint color when selector is on the button
//     */
//    public int getDrawableTintOnSelection() {
//        return drawableTintOnSelection;
//    }
//
//    /**
//     * @return drawable's tint color
//     */
//    public int getDrawableTint() {
//        return drawableTint;
//    }
//
//    /**
//     * @return true if button's drawable is not empty
//     */
//    public boolean hasDrawableTint() {
//        return hasDrawableTint;
//    }
//
//    /**
//     * @return true if button's drawable has tint when selector is on the button
//     */
//    public boolean hasDrawableTintOnSelection() {
//        return hasDrawableTintOnSelection;
//    }
//
//    /**
//     *
//     */
//    boolean hasWeight() {
//        return hasWeight;
//    }
//
//    float getWeight() {
//        return buttonWeight;
//    }
//
//    int getButtonWidth() {
//        return buttonWidth;
//    }
//
//    boolean hasWidth() {
//        return hasWidth;
//    }
//
//    boolean hasTextColorOnSelection() {
//        return hasTextColorOnSelection;
//    }

    // endregion
}
