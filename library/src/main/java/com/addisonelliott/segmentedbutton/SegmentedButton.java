package com.addisonelliott.segmentedbutton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressLint("RtlHardcoded")
public class SegmentedButton extends View {

    // region Variables & Constants
    private static final String TAG = "SegmentedButton";

    @IntDef(flag = true, value = {
            Gravity.LEFT,
            Gravity.RIGHT,
            Gravity.TOP,
            Gravity.BOTTOM,
    })
    @Retention(RetentionPolicy.SOURCE)
    private @interface GravityOptions {}
    // TODO Use this eventually

    // General purpose rectangle to prevent memory allocation in onDraw
    private RectF rectF;

    // Text paint variables contains paint info for unselected and selected text
    private TextPaint textPaint;
    // Static layout used for positioning and drawing unselected and selected text
    private StaticLayout textStaticLayout;
    // Maximum text width assuming all text is on one line, this is used in onMeasure to calculate the desired width
    private int textMaxWidth;

    // Position (X/Y) of the text and drawable
    private PointF textPosition, drawablePosition;

    // Clip path used to round background drawable edges to create rounded button group
    private Path backgroundClipPath;
    // Radius of the segmented button group used for creating background clip path
    private int backgroundRadius;
    // Whether this button is on the left or right side of the segmented group, determines which side to round out
    private boolean isLeftButton, isRightButton;

    // Horizontal relative clip position from 0.0f to 1.0f.
    // Value is scaled by the width of this view to get the actual clip X coordinate
    private float relativeClipPosition;
    // Whether or not the clipping is occurring from the left (true) or right (false). In simpler terms, if true,
    // then the start clipping relative position is 0.0f, otherwise, if clipping from the right, the position is 1.0f
    private boolean isClippingLeft;

    // Drawable for the background, this will be a ColorDrawable in the case a solid color is given
    private Drawable backgroundDrawable;
    // Drawable for the background when selected, this will be a ColorDrawable in the case a solid color is given
    private Drawable selectedBackgroundDrawable;

    // Color of the ripple to display over the button (default value is gray)
    private int rippleColor;

    // TODO Annotate default values

    // RippleDrawable is used for drawing ripple animation when tapping buttons on Lollipop and above devices (API 21+)
    private RippleDrawable rippleDrawableLollipop;
    // Backport for RippleDrawable for API 16-20 devices
    private codetail.graphics.drawables.RippleDrawable rippleDrawable;

    // Color filters used for tinting the button drawable in normal and when button is selected, will be null for no
    // tint
    private PorterDuffColorFilter drawableColorFilter, selectedDrawableColorFilter;

    // Drawable to draw for the button. Can be drawn beside text or without text at all
    private Drawable drawable;
    // Padding for the drawable in pixels, this will only be applied between the drawable and text (default value is 0)
    private int drawablePadding;
    // Whether or not there is a tint color for the drawable when unselected and/or selected
    private boolean hasDrawableTint, hasSelectedDrawableTint;
    // Tint color for the drawable when unselected and selected
    private int drawableTint, selectedDrawableTint;
    // Whether or not a width or height was specified for the drawable
    private boolean hasDrawableWidth, hasDrawableHeight;
    // Width and height for the drawable, in pixels
    private int drawableWidth, drawableHeight;
    // Determines where to draw the drawable in relation to the text, can be one of GravityOptions types
    private int drawableGravity;

    // Whether or not we have text, false indicates text should be empty
    private boolean hasText;
    // Text to display for button (default value is an empty string meaning no text will be shown)
    private String text;
    // Whether or not we have a selected text color
    private boolean hasSelectedTextColor;
    // Text color and selected text color (default value is gray for unselected, white for selected text colors)
    private int textColor, selectedTextColor;
    // Font size of the text in pixels (default value is 14sp)
    private float textSize;
    // Typeface to use for displaying the text, this is created from the fontFamily & textStyle attributes
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

        // Create general purpose rectangle, prevents memory allocation during onDraw
        rectF = new RectF();

        // Required in order for this button to 'consume' the ripple touch event
        setClickable(true);
    }

    private void getAttributes(Context context, @Nullable AttributeSet attrs) {
        // According to docs for obtainStyledAttributes, attrs can be null and I assume that each value will be set
        // to the default
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SegmentedButton);

        // Load background if available, this can be a drawable or a color
        // In the instance of a color, a ColorDrawable is created and used instead
        // Note: Not well documented but getDrawable will return a ColorDrawable if a color is specified
        if (ta.hasValue(R.styleable.SegmentedButton_background)) {
            backgroundDrawable = ta.getDrawable(R.styleable.SegmentedButton_background);
        }

        // Load background on selection if available, can be drawable or color
        if (ta.hasValue(R.styleable.SegmentedButton_selectedBackground)) {
            selectedBackgroundDrawable = ta.getDrawable(R.styleable.SegmentedButton_selectedBackground);
        }

        // Parse ripple color value and update the ripple
        setRipple(ta.getColor(R.styleable.SegmentedButton_rippleColor, Color.GRAY));

        // Load drawable if available, otherwise variable will be null
        if (ta.hasValue(R.styleable.SegmentedButton_drawable)) {
            drawable = ta.getDrawable(R.styleable.SegmentedButton_drawable);
        }
        drawablePadding = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawablePadding, 0);
        hasDrawableTint = ta.hasValue(R.styleable.SegmentedButton_drawableTint);
        drawableTint = ta.getColor(R.styleable.SegmentedButton_drawableTint, -1);
        hasSelectedDrawableTint = ta.hasValue(R.styleable.SegmentedButton_selectedDrawableTint);
        selectedDrawableTint = ta.getColor(R.styleable.SegmentedButton_selectedDrawableTint, -1);
        hasDrawableWidth = ta.hasValue(R.styleable.SegmentedButton_drawableWidth);
        hasDrawableHeight = ta.hasValue(R.styleable.SegmentedButton_drawableHeight);
        drawableWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawableWidth, -1);
        drawableHeight = ta.getDimensionPixelSize(R.styleable.SegmentedButton_drawableHeight, -1);
        drawableGravity = ta.getInteger(R.styleable.SegmentedButton_drawableGravity, Gravity.LEFT);

        hasText = ta.hasValue(R.styleable.SegmentedButton_text);
        text = ta.getString(R.styleable.SegmentedButton_text);
        textColor = ta.getColor(R.styleable.SegmentedButton_textColor, Color.GRAY);
        hasSelectedTextColor = ta.hasValue(R.styleable.SegmentedButton_selectedTextColor);
        selectedTextColor = ta.getColor(R.styleable.SegmentedButton_selectedTextColor, Color.WHITE);

        // Convert 14sp to pixels for default value on text size
        final float px14sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14.0f,
                context.getResources().getDisplayMetrics());
        textSize = ta.getDimension(R.styleable.SegmentedButton_textSize, px14sp);

        final boolean hasFontFamily = ta.hasValue(R.styleable.SegmentedButton_android_fontFamily);
        final int textStyle = ta.getInt(R.styleable.SegmentedButton_textStyle, Typeface.NORMAL);

        // If a font family is present then load typeface with text style from that
        if (hasFontFamily) {
            // Note: TypedArray.getFont is used for Android O & above while ResourcesCompat.getFont is used for below
            // Experienced an odd bug in the design viewer of Android Studio where it would not work with only using
            // the ResourcesCompat.getFont function. Unsure of the reason but this fixes it
            if (VERSION.SDK_INT >= VERSION_CODES.O) {
                textTypeface = Typeface.create(ta.getFont(R.styleable.SegmentedButton_android_fontFamily), textStyle);
            } else {
                final int fontFamily = ta.getResourceId(R.styleable.SegmentedButton_android_fontFamily, 0);
                textTypeface = Typeface.create(ResourcesCompat.getFont(context, fontFamily), textStyle);
            }
        } else {
            textTypeface = Typeface.create((Typeface) null, textStyle);
        }

        ta.recycle();
    }

    private void initText() {
        // Text position is calculated regardless of if text exists
        // Not worth extra effort of not setting two float values
        textPosition = new PointF();

        // If there is no text then do not bother
        if (!hasText) {
            return;
        }

        // Create text paint that will be used to draw the text on the canvas
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        textPaint.setTypeface(textTypeface);

        // Initial kickstart to setup the text layout by assuming the text will be all in one line
        textMaxWidth = (int) textPaint.measureText(text);
        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            textStaticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, textMaxWidth).build();
        } else {
            textStaticLayout = new StaticLayout(text, textPaint, textMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0,
                    false);
        }
    }

    private void initDrawable() {
        // Drawable position is calculated regardless of if drawable exists
        // Not worth extra effort of not setting two float values
        drawablePosition = new PointF();

        // If there is no drawable then do not bother
        if (drawable == null) {
            return;
        }

        // If drawable has a tint color, then create a color filter that will be applied to it
        if (hasDrawableTint) {
            drawableColorFilter = new PorterDuffColorFilter(drawableTint, PorterDuff.Mode.SRC_IN);
        }

        // If selected drawable has a tint color, then create a color filter that will be applied to it
        if (hasSelectedDrawableTint) {
            selectedDrawableColorFilter = new PorterDuffColorFilter(selectedDrawableTint, PorterDuff.Mode.SRC_IN);
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

        // Measured width & height
        int width, height;

        // Calculate drawable width, 0 if null, drawableWidth if set, otherwise intrinsic width
        final int drawableWidth = drawable != null ? hasDrawableWidth ? this.drawableWidth
                : drawable.getIntrinsicWidth() : 0;
        // For the text width, assume that it is in a single line with no wrapping which would be textMaxWidth
        // This variable is used to calculate the desired width and the desire is for it all to be in a single line
        final int textWidth = hasText ? textMaxWidth : 0;

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
        //      - MeasureSpec.AT_MOST: Set width to desired size but dont exceed specified size
        //      - MeasureSpec.UNSPECIFIED: Set width to desired size
        width = resolveSize(desiredWidth, widthMeasureSpec);

        // With width calculated, recalculate the text parameters to get new height (wrapping may occur)
        measureTextWidth(width, drawableWidth);

        // Repeat measuring process for height now
        // Note that the height is the static layout height which may or may not be multi-lined
        // Calculate drawable height, 0 if null, drawableHeight if set, otherwise intrinsic height
        final int drawableHeight = drawable != null ? hasDrawableHeight ? this.drawableHeight
                : drawable.getIntrinsicHeight() : 0;
        final int textHeight = hasText ? textStaticLayout.getHeight() : 0;

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
        //      - MeasureSpec.AT_MOST: Set height to desired size but dont exceed specified size
        //      - MeasureSpec.UNSPECIFIED: Set height to desired size
        height = resolveSize(desiredHeight, heightMeasureSpec);

        // Required to be called to notify the View of the width & height decided
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Recalculate the background clip path since width & height have changed
        setupBackgroundClipPath();

        // Calculate new positions and bounds for text & drawable
        updateSize();
    }

    private void measureTextWidth(int width, int drawableWidth) {
        // TODO Comment me
        // Measures the text width given entire width of the segmented button

        // If there is no text, then we don't need to do anything
        if (!hasText) {
            return;
        }

        // Set drawable width to be the drawable width if the drawable has horizontal gravity, otherwise the drawable
        // width doesnt matter
        // Text width is equal to the total width minus padding and drawable width
        // But, if the maximum text width is smaller, just use that and we will manually pad it later
        int newDrawableWidth = Gravity.isHorizontal(drawableGravity) ? drawableWidth : 0;
        int textWidth = Math.min(width - getPaddingLeft() - getPaddingRight() - newDrawableWidth, textMaxWidth);

        // Odd case where there is not enough space for the padding and drawable width so we just return
        if (textWidth < 0) {
            return;
        }

        // Create new static layout with width
        // Old way of creating static layout was deprecated but I dont think there is any speed difference between
        // the two
        if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            textStaticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, textWidth).build();
        } else {
            textStaticLayout = new StaticLayout(text, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0,
                    false);
        }
    }

    /**
     * Calculate new bounds for all elements in the button
     */
    private void updateSize() {
        final int width = getWidth(), height = getHeight();
        final int textWidth = textStaticLayout != null ? textStaticLayout.getWidth() : 0,
                textHeight = textStaticLayout != null ? textStaticLayout.getHeight() : 0;
        final int drawableWidth = drawable != null ? hasDrawableWidth ? this.drawableWidth
                : drawable.getIntrinsicWidth() : 0;
        final int drawableHeight = drawable != null ? hasDrawableHeight ? this.drawableHeight
                : drawable.getIntrinsicHeight() : 0;

        // Calculates the X/Y positions of the text and drawable now that the measured size is known
        if (Gravity.isHorizontal(drawableGravity)) {
            // Calculate Y position for horizontal gravity, i.e. center the drawable and/or text if necessary
            // Fancy way of centering the two objects vertically, the last 2 if statements are special cases where
            // either the drawable or text is taking up the full height so there is no need to calculate the center
            textPosition.y = getPaddingTop()
                    + (height - getPaddingTop() - getPaddingBottom() - textHeight) / 2.0f;
            drawablePosition.y = getPaddingTop()
                    + (height - getPaddingTop() - getPaddingBottom() - drawableHeight) / 2.0f;

            // Calculate the starting X position with horizontal gravity
            // If the exact amount of width is used (meaning useDesiredWidth is true), then the start position is set
            // to be the left padding. Otherwise, the start position is half of the remaining space to center it
            final float startPosition = (width - textWidth - drawableWidth - drawablePadding) / 2.0f;

            // Position the drawable & text based on the gravity
            if (drawableGravity == Gravity.LEFT) {
                textPosition.x = startPosition + drawableWidth + drawablePadding;
                drawablePosition.x = startPosition;
            } else if (drawableGravity == Gravity.RIGHT) {
                textPosition.x = startPosition;
                drawablePosition.x = startPosition + textWidth + drawablePadding;
            }
        } else {
            // Calculate X position for vertical gravity, i.e. center the drawable and/or text horizontally if necessary
            // Fancy way of centering the two objects horizontally, the last 2 if statements are special cases where
            // either the drawable or text is taking up the full height so there is no need to calculate the center
            textPosition.x = getPaddingLeft()
                    + (width - getPaddingLeft() - getPaddingRight() - textWidth) / 2.0f;
            drawablePosition.x = getPaddingLeft()
                    + (width - getPaddingLeft() - getPaddingRight() - drawableWidth) / 2.0f;

            // Calculate the starting Y position with vertical gravity
            // If the exact amount of height is used (meaning useDesiredHeight is true), then the start position is set
            // to be the top padding. Otherwise, the start position is half of the remaining space to center it
            final float startPosition = (height - textHeight - drawableHeight - drawablePadding) / 2.0f;

            // Position the drawable & text based on the gravity
            if (drawableGravity == Gravity.TOP) {
                textPosition.y = startPosition + drawableHeight + drawablePadding;
                drawablePosition.y = startPosition;
            } else if (drawableGravity == Gravity.BOTTOM) {
                textPosition.y = startPosition;
                drawablePosition.y = startPosition + textHeight + drawablePadding;
            }
        }

        // Set bounds of drawable if it exists
        if (drawable != null) {
            drawable.setBounds((int) drawablePosition.x, (int) drawablePosition.y,
                    (int) drawablePosition.x + drawableWidth, (int) drawablePosition.y + drawableHeight);
        }

        // Set bounds of background drawable if it exists
        if (backgroundDrawable != null) {
            backgroundDrawable.setBounds(0, 0, width, height);
        }

        // Set bounds of selected background drawable if it exists
        if (selectedBackgroundDrawable != null) {
            selectedBackgroundDrawable.setBounds(0, 0, width, height);
        }

        if (rippleDrawableLollipop != null) {
            rippleDrawableLollipop.setBounds(0, 0, width, height);
        }

        if (rippleDrawable != null) {
            rippleDrawable.setBounds(0, 0, width, height);
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
        if (backgroundDrawable != null) {
            backgroundDrawable.draw(canvas);
        }

        // Draw text (unselected)
        if (hasText) {
            canvas.save();
            canvas.translate(textPosition.x, textPosition.y);
            textPaint.setColor(textColor);
            textStaticLayout.draw(canvas);
            canvas.restore();
        }

        // Draw drawable (unselected)
        if (drawable != null) {
            drawable.setColorFilter(drawableColorFilter);
            drawable.draw(canvas);
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
        if (selectedBackgroundDrawable != null) {
            selectedBackgroundDrawable.draw(canvas);
        }

        // Draw text (selected)
        if (hasText) {
            canvas.save();
            canvas.translate(textPosition.x, textPosition.y);
            // If a selected text color was specified, then use that, otherwise we want to default to the original
            // text color
            textPaint.setColor(hasSelectedTextColor ? selectedTextColor : textColor);
            textStaticLayout.draw(canvas);
            canvas.restore();
        }

        // Draw drawable (unselected)
        if (drawable != null) {
            // If a selected drawable tint was used, then use that, but if it wasn't specified we want to stick with
            // the normal tint color.
            drawable.setColorFilter(hasSelectedDrawableTint ? selectedDrawableColorFilter : drawableColorFilter);
            drawable.draw(canvas);
        }

        canvas.restore();

        if (rippleDrawableLollipop != null) {
            rippleDrawableLollipop.draw(canvas);
        }

        if (rippleDrawable != null) {
            rippleDrawable.draw(canvas);
        }
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
    public void clipLeft(@FloatRange(from = 0.0, to = 1.0) float relativePosition) {
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
    public void clipRight(@FloatRange(from = 0.0, to = 1.0) float relativePosition) {
        // Clipping from the right side, set to false
        isClippingLeft = false;

        // Update relative clip position
        relativeClipPosition = relativePosition;

        // Redraw
        invalidate();
    }

    // endregion

    // region Ripple-related

    @SuppressLint("NewApi")
    @Override
    public void drawableHotspotChanged(final float x, final float y) {
        // This function is called when the hotspot for the drawable changes such as when the user taps on this view,
        // it will call this with the coordinates.
        // Normally the super class handles this automatically for the background drawable but the ripple drawable is
        // not the background in this instance
        super.drawableHotspotChanged(x, y);

        // Update the hotspot for the ripple drawable
        if (rippleDrawableLollipop != null) {
            rippleDrawableLollipop.setHotspot(x, y);
        }

        // Update the hotspot for the ripple drawable
        if (rippleDrawable != null) {
            rippleDrawable.setHotspot(x, y);
        }
    }

    @Override
    protected void drawableStateChanged() {
        // This function is called when the state of this view changes such as when it is clicked, enabled, disabled,
        // etc. This is meant to update the state of the drawable.
        // Normally the super class handles this automatically for the background drawable but the ripple drawable is
        // not the background in this instance
        super.drawableStateChanged();

        // Update the state for the ripple drawable
        if (rippleDrawableLollipop != null) {
            rippleDrawableLollipop.setState(getDrawableState());
        }

        // Update the state for the ripple drawable
        if (rippleDrawable != null) {
            rippleDrawable.setState(getDrawableState());
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull final Drawable who) {
        // Very obscure and difficult to find but it is noted in the source code docstring for this function
        // Return true if the drawable is the ripple drawable (backport or regular)
        // Normally the super class handles this automatically for the background drawable but the ripple drawable is
        // not the background in this instance
        return who == rippleDrawableLollipop || who == rippleDrawable || super.verifyDrawable(who);
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
        rectF.set(0, 0, getWidth(), getHeight());

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
            backgroundClipPath.addRoundRect(rectF,
                    new float[]{br, br, br, br, br, br, br, br}, Direction.CW);
        } else if (isLeftButton) {
            // Add radius on left side only
            backgroundClipPath = new Path();
            backgroundClipPath.addRoundRect(rectF, new float[]{br, br, 0, 0, 0, 0, br, br}, Direction.CW);
        } else if (isRightButton) {
            // Add radius on right side only
            backgroundClipPath = new Path();
            backgroundClipPath.addRoundRect(rectF, new float[]{0, 0, br, br, br, br, 0, 0}, Direction.CW);
        } else {
            backgroundClipPath = null;
        }

        // Canvas.clipPath, used in onDraw for drawing the background clip path (rounding the edges for left-most and
        // right-most buttons) is not supported with hardware acceleration until API 18
        // Thus, switch to software acceleration if the background clip path is not null (meaning the edges are
        // rounded) and the current version is less than 18
        // Otherwise, switch to hardware acceleration
        if (backgroundClipPath != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        } else {
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    @SuppressWarnings("SameParameterValue")
    void setIsLeftButton(boolean isLeftButton) {
        this.isLeftButton = isLeftButton;
    }

    void setIsRightButton(boolean isRightButton) {
        this.isRightButton = isRightButton;
    }

    void setBackgroundRadius(int backgroundRadius) {
        this.backgroundRadius = backgroundRadius;
    }

    /**
     * Sets the background of the button to be the drawable background if it does not have a background already and
     * the drawable is not null
     *
     * If either one of those conditions are met, then the background is not changed
     *
     * This is a package-private function used by SegmentedButtonGroup to pass its 'global' background down to the
     * buttons
     *
     * @param drawable Drawable to set as the background
     */
    void setDefaultBackground(@Nullable Drawable drawable) {
        if (backgroundDrawable == null && drawable != null) {
            // Make sure to clone the drawable so that we can set the bounds on it
            backgroundDrawable = drawable.getConstantState().newDrawable();
        }
    }

    /**
     * Sets the selected background of the button to be the drawable background if it does not have a background
     * already and the drawable is not null
     *
     * If either one of those conditions are met, then the background is not changed
     *
     * This is a package-private function used by SegmentedButtonGroup to pass its 'global' background down to the
     * buttons
     *
     * @param drawable Drawable to set as the background
     */
    void setDefaultSelectedBackground(@Nullable Drawable drawable) {
        if (selectedBackgroundDrawable == null && drawable != null) {
            // Make sure to clone the drawable so that we can set the bounds on it
            selectedBackgroundDrawable = drawable.getConstantState().newDrawable();
        }
    }

    void setRipple(boolean enabled) {
        // TODO Note that this is package-private because I dont want people enabling or disabling the ripple effect
        // on a button by button basis

        if (enabled) {
            // Recreate the ripple drawable and setup with the ripple color
            setRipple(rippleColor);
        } else {
            // Set both ripple drawables to null so that we do not draw the ripple
            rippleDrawableLollipop = null;
            rippleDrawable = null;
        }
    }

    public void setRipple(@ColorInt int color) {
        rippleColor = color;

        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            rippleDrawableLollipop = new RippleDrawable(ColorStateList.valueOf(rippleColor), null, null);
            rippleDrawableLollipop.setCallback(this);
            rippleDrawableLollipop.setBounds(0, 0, getWidth(), getHeight());

            // Disable/nullify the pre-lollipop RippleDrawable backport
            rippleDrawable = null;
        } else {
            rippleDrawable = new codetail.graphics.drawables.RippleDrawable(ColorStateList.valueOf(rippleColor), null,
                    null);
            rippleDrawable.setCallback(this);
            rippleDrawable.setBounds(0, 0, getWidth(), getHeight());

            // Disable/nullify the lollipop RippleDrawable
            rippleDrawableLollipop = null;
        }

        invalidate();
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;

        requestLayout();
    }

    // endregion

    // region Unused

    // TODO Create getters and setters, test them though!

    // TODO Go through and add docstrings to each function

//    /**
//     * Typeface.NORMAL: 0
//     * Typeface.BOLD: 1
//     * Typeface.ITALIC: 2
//     * Typeface.BOLD_ITALIC: 3
//     *
//     * @param typeface you can use above variations using the bitwise OR operator
//     */
//    public void setTypeface(Typeface typeface) {
//        textPaint.setTypeface(typeface);
//    }
//
//    /**
//     * @param location is .ttf file's path in assets folder. Example: 'fonts/my_font.ttf'
//     */
//    public void setTypeface(String location) {
//        if (null != location && !location.equals("")) {
//            Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), location);
//            textPaint.setTypeface(typeface);
//        }
//    }
//
//    void setSelectorColor(int color) {
//        mPaint.setColor(color);
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
//        drawable = drawable;
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
//    boolean hasTextColorOnSelection() {
//        return hasTextColorOnSelection;
//    }

    // endregion
}
