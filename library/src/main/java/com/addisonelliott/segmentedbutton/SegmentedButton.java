package com.addisonelliott.segmentedbutton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
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
import androidx.core.content.res.ResourcesCompat;

public class SegmentedButton extends View {

    // region Variables & Constants
    private static final String TAG = "SegmentedButton";

    // Horizontal relative clip position from 0.0f to 1.0f. This value is scaled by views width to get a value from
    // 0.0f to the width of the view
    private float relativeClipPosition;
    // Whether or not the clipping is occurring from the left (true) or right (false). In simpler terms, if true,
    // then the start clipping relative position is 0.0f, otherwise, if clipping from the right, the position is 1.0f
    private boolean isClippingLeft;

    // Clip path used to round background drawable edges to match parent radius
    private Path backgroundClipPath;
    // Radius of the background segmented button group used for creating background clip path
    private int backgroundRadius;
    // Whether this button is on the left or right side of the segmented group, determines which side to round out
    private boolean isLeftButton, isRightButton;

    private TextPaint mTextPaint;
    private StaticLayout mStaticLayout;
    private int mTextMaxWidth;

    private RectF mRectF;
    private Paint mPaint;

    // Position (X/Y) of the text and drawable
    private PointF textPosition, drawablePosition;

    private PorterDuffColorFilter mDrawableNormalColor, mDrawableSelectedColor;

    // Drawable is the icon or image to draw. This can be drawn beside text or without text
    private Drawable mDrawable;
    // Drawable for the background, this will be a ColorDrawable in case a solid color is given
    private Drawable mBackgroundDrawable;
    // Drawable for the background when selected, this will be a ColorDrawable in case a solid color is given
    private Drawable mSelectedBackgroundDrawable;

    // Text and boolean which indicates whether we have text
    private boolean hasText;
    private String text;

    // Text color and selected text color
    // Keep a boolean of whether there is a selected text color so we know if we dont need to draw anything
    // TODO Wrong, no need for hasSelectedTextColor, should just use same selectedTextColor
    // TODO Look into null type value for Color, -1 maybe?
    private int textColor, selectedTextColor;
    private boolean hasSelectedTextColor;

    private int drawableGravity;

    // Custom attributes
    private int selectedDrawableTint, rippleColor, drawableTint,
            drawableWidth, drawableHeight, drawablePadding;
    private boolean hasRipple, hasSelectedDrawableTint,
            hasDrawableWidth, hasDrawableHeight, hasDrawableTint;
    private float textSize;
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
        initDrawable();

        // Setup default values for clip position
        // By default, set to clip from left and have none of the selected view shown
        relativeClipPosition = 0.0f;
        isClippingLeft = true;

        // Setup background clip path parameters
        // This should be changed before onDraw is ever called but they are initialized to be safe
        backgroundRadius = 0;
        isLeftButton = false;
        isRightButton = false;

        // Create general purpose rectangle and paint object
        // These are used by different components when drawing and saves us allocating during onDraw stage
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

        // Load background if available, this can be a drawable or a color
        // In the instance of a color, a ColorDrawable is created and used instead
        // Note: Not well documented but getDrawable will return a ColorDrawable if a color is specified
        if (ta.hasValue(R.styleable.SegmentedButton_background)) {
            mBackgroundDrawable = ta.getDrawable(R.styleable.SegmentedButton_background);
        }

        // Load background on selection if available, can be drawable or color
        if (ta.hasValue(R.styleable.SegmentedButton_selectedBackground)) {
            mSelectedBackgroundDrawable = ta.getDrawable(R.styleable.SegmentedButton_selectedBackground);
        }

        // Load drawable if available, otherwise variable will be null
        if (ta.hasValue(R.styleable.SegmentedButton_drawable)) {
            mDrawable = ta.getDrawable(R.styleable.SegmentedButton_drawable);
        }
        drawablePadding = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawablePadding, 0);
        hasDrawableTint = ta.hasValue(R.styleable.SegmentedButton_drawableTint);
        drawableTint = ta.getColor(R.styleable.SegmentedButton_drawableTint, -1);
        hasSelectedDrawableTint = ta.hasValue(R.styleable.SegmentedButton_selectedDrawableTint);
        selectedDrawableTint = ta.getColor(R.styleable.SegmentedButton_selectedDrawableTint,
                Color.WHITE);
        hasDrawableWidth = ta.hasValue(R.styleable.SegmentedButton_drawableWidth);
        hasDrawableHeight = ta.hasValue(R.styleable.SegmentedButton_drawableHeight);
        drawableWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawableWidth, -1);
        drawableHeight = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawableHeight, -1);
        // TODO No drawable keep aspect ratio?
        drawableGravity = ta.getInteger(R.styleable.SegmentedButton_drawableGravity, Gravity.LEFT);

        hasText = ta.hasValue(R.styleable.SegmentedButton_text);
        text = ta.getString(R.styleable.SegmentedButton_text);
        textColor = ta.getColor(R.styleable.SegmentedButton_textColor, Color.GRAY);
        hasSelectedTextColor = ta.hasValue(R.styleable.SegmentedButton_selectedTextColor);
        selectedTextColor = ta.getColor(R.styleable.SegmentedButton_selectedTextColor, Color.WHITE);
        textSize = ta.getDimension(R.styleable.SegmentedButton_textSize, ConversionHelper.spToPx(getContext(), 14));

        boolean hasFontFamily = ta.hasValue(R.styleable.SegmentedButton_android_fontFamily);
        int textStyle = ta.getInt(R.styleable.SegmentedButton_textStyle, Typeface.NORMAL);

        // If a font family is present then load typeface with text style from that
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

        ta.recycle();
    }

    private void initText() {
        // Text position is calculated regardless of text exists
        // Not worth extra effort of not setting two float values
        textPosition = new PointF();

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

    private void initDrawable() {
        // Drawable position is calculated regardless of text exists
        // Not worth extra effort of not setting two float values
        drawablePosition = new PointF();

        // If there is no drawable then do not bother
        if (mDrawable == null) {
            return;
        }

        if (hasDrawableTint) {
            mDrawableNormalColor = new PorterDuffColorFilter(drawableTint, PorterDuff.Mode.SRC_IN);
        }

        if (hasSelectedDrawableTint) {
            mDrawableSelectedColor = new PorterDuffColorFilter(selectedDrawableTint, PorterDuff.Mode.SRC_IN);
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
        final int drawableWidth = mDrawable != null ? hasDrawableWidth ? this.drawableWidth
                : mDrawable.getIntrinsicWidth() : 0;
        final int textWidth = hasText ? mTextMaxWidth : 0;

        // Measured width & height
        int width, height;

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

        // Resolve width with measure spec and desired width
        // Three options:
        //      - MeasureSpec.EXACTLY: Set width to exactly specified size
        //      - MeasureSpec.AT_MOST: Set width to desired size but dont it exceed specified size
        //      - MeasureSpec.UNSPECIFIED: Set width to desired size
        width = resolveSize(desiredWidth, widthMeasureSpec);

        // With width calculated, recalculate the text parameters to get new height (wrapping may occur)
        measureTextWidth(width, drawableWidth);

        // Repeat measuring process for height now
        // Note that the height is the static layout height which may or may not be multi-lined
        final int drawableHeight = mDrawable != null ? hasDrawableHeight ? this.drawableHeight
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

        // Resolve height with measure spec and desired height
        // Three options:
        //      - MeasureSpec.EXACTLY: Set height to exactly specified size
        //      - MeasureSpec.AT_MOST: Set height to desired size but dont it exceed specified size
        //      - MeasureSpec.UNSPECIFIED: Set height to desired size
        height = resolveSize(desiredHeight, heightMeasureSpec);

        Log.d(TAG, String.format("onMeasure called with mode (%d, %d) and size (%d, %d). Desired size (%d, %d) "
                        + "Resulting size (%d, %d).", widthMode, heightMode, widthSize, heightSize, desiredWidth,
                desiredHeight, width, height));

        // Calculate the position for text & drawable now that we know width & height
        // useDesired[Width|Height] parameter is used to indicate whether there is extra space (excluding padding)
        // between the drawable or text size and the measured size. If true, this will center the object(s)
        // appropriately
        // TODO Move to onSizeChanged, more appropriate right?
        calculatePositions(width, height, textWidth, textHeight, drawableWidth, drawableHeight, desiredWidth >= width,
                desiredHeight >= height);

        // Required to be called to notify the View of the width & height decided
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Recalculate the background clip path since width & height have changed
        setupBackgroundClipPath();
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

        if (mDrawable != null) {
            mDrawable.setBounds((int) drawablePosition.x, (int) drawablePosition.y,
                    (int) drawablePosition.x + drawableWidth, (int) drawablePosition.y + drawableHeight);
        }

        // TODO Move somewhere better
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.setBounds(0, 0, measuredWidth, measuredHeight);
        }

        if (mSelectedBackgroundDrawable != null) {
            mSelectedBackgroundDrawable.setBounds(0, 0, measuredWidth, measuredHeight);
        }
    }

    // endregion

    // region Drawing

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int width = getWidth();
        final int height = getHeight();

        // Apply background clip path if available
        // This will clip the button to the parent segmented group in case there is a radius for rounding the corners
        if (backgroundClipPath != null) {
            canvas.clipPath(backgroundClipPath);
        }

        // Draw background
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.draw(canvas);
        }

        // Draw text (unselected)
        if (hasText) {
            canvas.save();
            canvas.translate(textPosition.x, textPosition.y);
            mTextPaint.setColor(textColor);
            mStaticLayout.draw(canvas);
            canvas.restore();
        }

        // Draw drawable (unselected)
        if (mDrawable != null) {
            mDrawable.setColorFilter(mDrawableNormalColor);
            mDrawable.draw(canvas);
        }

        // Begin drawing selected button view
        canvas.save();

        // Clip canvas for drawing the selected button view
        if (isClippingLeft) {
            // If clipping from left, go from 0.0f -> relativeClipPosition * width horizontally
            canvas.clipRect(0.0f, 0.0f, relativeClipPosition * width, height);
        } else {
            // If clipping from right, go from relativeClipPosition * width -> 1.0f horizontally
            canvas.clipRect(relativeClipPosition * width, 0.0f, width, height);
        }

        // Draw background (selected)
        if (mSelectedBackgroundDrawable != null) {
            mSelectedBackgroundDrawable.draw(canvas);
        }

        // Draw text (selected)
        if (hasSelectedTextColor) {
            canvas.save();
            canvas.translate(textPosition.x, textPosition.y);
            mTextPaint.setColor(selectedTextColor);
            mStaticLayout.draw(canvas);
            canvas.restore();
        }

        canvas.restore();

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
//            drawDrawableWithColorFilter(canvas, mDrawableNormalColor);
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

    /**
     * Horizontally clips selected button view from the left side (0.0f) to relativePosition
     *
     * For example, a relativePosition of 0.0f would mean the entire selected button view would be available and no
     * clipping would occur.
     *
     * However, a relative position of 1.0f would mean the entire selected button view is clipped and the normal
     * button view is entirely visible.
     *
     * This can be thought of as the selected button view being clipped from 0.0f on the left to the relativePosition
     * with 1.0f being all the way on the right.
     *
     * @param relativePosition Position from 0.0f to 1.0f that represents where to end clipping. A value of 0.0f
     *                         would represent no clipping and 1.0f would represent clipping the entire view
     */
    public void clipLeft(float relativePosition) {
        // Clipping from the left side, set to true
        isClippingLeft = true;

        // Update relative clip position
        relativeClipPosition = relativePosition;

        // Redraw
        invalidate();
    }

    /**
     * Horizontally clips selected button view from the right side (1.0f) to relativePosition
     *
     * For example, a relativePosition of 1.0f would mean the entire selected button view would be available and no
     * clipping would occur.
     *
     * However, a relative position of 0.0f would mean the entire selected button view is clipped and the normal
     * button view is entirely visible.
     *
     * This can be thought of as the selected button view being clipped from 0.0f on the left to the relativePosition
     * with 1.0f being all the way on the right.
     *
     * @param relativePosition Position from 0.0f to 1.0f that represents where to end clipping. A value of 1.0f
     *                         would represent no clipping and 0.0f would represent clipping the entire view
     */
    public void clipRight(float relativePosition) {
        // Clipping from the right side, set to false
        isClippingLeft = false;

        // Update relative clip position
        relativeClipPosition = relativePosition;

        // Redraw
        invalidate();
    }

    // endregion

    // region Getters & Setters

    void setupBackgroundClipPath() {
        // If there is no background radius then skip
        if (backgroundRadius == 0) {
            backgroundClipPath = null;
            return;
        }

        // Set rectangle to take up entire view, used to create clip path
        mRectF.set(0, 0, getWidth(), getHeight());

        // Background radius, shorthand variable to make code cleaner
        // Note: In Android Studio previewer, some of the background color a rounded segmented button group along
        // with a background color in individual buttons may appear. Does not appear when running on actual devices
        // however.
        // Recommended approach is to treat button group background and button background as being mutually exclusive.
        // That is use one or the other but not both.
        final float br = backgroundRadius;

        if (isLeftButton && isRightButton) {
            // Add radius on all sides, left & right
            backgroundClipPath = new Path();
            backgroundClipPath.addRoundRect(mRectF,
                    new float[]{br, br, br, br, br, br, br, br}, Direction.CW);
        } else if (isLeftButton) {
            // Add radius on left side only
            backgroundClipPath = new Path();
            backgroundClipPath.addRoundRect(mRectF, new float[]{br, br, 0, 0, 0, 0, br, br}, Direction.CW);
        } else if (isRightButton) {
            // Add radius on right side only
            backgroundClipPath = new Path();
            backgroundClipPath.addRoundRect(mRectF, new float[]{0, 0, br, br, br, br, 0, 0}, Direction.CW);
        } else {
            backgroundClipPath = null;
        }
    }

    void setIsLeftButton(boolean isLeftButton) {
        this.isLeftButton = isLeftButton;

        // TODO Not sure this is best way to handle it
        setupBackgroundClipPath();
    }

    void setIsRightButton(boolean isRightButton) {
        this.isRightButton = isRightButton;

        setupBackgroundClipPath();
    }

    void setBackgroundRadius(int backgroundRadius) {
        this.backgroundRadius = backgroundRadius;

        setupBackgroundClipPath();
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
//        hasSelectedDrawableTint = false;
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
//        return selectedDrawableTint;
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
//    public boolean hasSelectedDrawableTint() {
//        return hasSelectedDrawableTint;
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
