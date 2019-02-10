package com.addisonelliott.segmentedbutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class SegmentedButtonGroup extends LinearLayout {

    // region Variables & Constants
    private static final String TAG = "SegmentedButtonGroup";

    // Animation interpolator styles for animating changing the selected button
    public final static int ANIM_INTERPOLATOR_NONE = -1;
    public final static int ANIM_INTERPOLATOR_FAST_OUT_SLOW_IN = 0;
    public final static int ANIM_INTERPOLATOR_BOUNCE = 1;
    public final static int ANIM_INTERPOLATOR_LINEAR = 2;
    public final static int ANIM_INTERPOLATOR_DECELERATE = 3;
    public final static int ANIM_INTERPOLATOR_CYCLE = 4;
    public final static int ANIM_INTERPOLATOR_ANTICIPATE = 5;
    public final static int ANIM_INTERPOLATOR_ACCELERATE_DECELERATE = 6;
    public final static int ANIM_INTERPOLATOR_ACCELERATE = 7;
    public final static int ANIM_INTERPOLATOR_ANTICIPATE_OVERSHOOT = 8;
    public final static int ANIM_INTERPOLATOR_FAST_OUT_LINEAR_IN = 9;
    public final static int ANIM_INTERPOLATOR_LINEAR_OUT_SLOW_IN = 10;
    public final static int ANIM_INTERPOLATOR_OVERSHOOT = 11;

    // Interface defined for linting purposes to ensure that an animation interpolator value (integer type) is one
    // of the valid values
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ANIM_INTERPOLATOR_NONE, ANIM_INTERPOLATOR_FAST_OUT_SLOW_IN, ANIM_INTERPOLATOR_BOUNCE,
            ANIM_INTERPOLATOR_LINEAR, ANIM_INTERPOLATOR_DECELERATE, ANIM_INTERPOLATOR_CYCLE,
            ANIM_INTERPOLATOR_ANTICIPATE, ANIM_INTERPOLATOR_ACCELERATE_DECELERATE, ANIM_INTERPOLATOR_ACCELERATE,
            ANIM_INTERPOLATOR_ANTICIPATE_OVERSHOOT, ANIM_INTERPOLATOR_FAST_OUT_LINEAR_IN,
            ANIM_INTERPOLATOR_LINEAR_OUT_SLOW_IN, ANIM_INTERPOLATOR_OVERSHOOT})
    public @interface AnimationInterpolator {}

    // This ViewGroup consists of a FrameLayout as it's child which contains three items:
    //     1. Button LinearLayout that contains the SegmentedButtons
    //     2. Divider LinearLayout that contains the dividers between buttons
    //     3. Border view that has the border for the group that is drawn over everything else
    private LinearLayout buttonLayout;

    // Purpose of the divider LinearLayout is to ensure the button dividers are placed in between the buttons and
    // that no extra space is allocated for the divider. If dividers were placed on the buttonLayout instead, then space
    // would be allocated for the dividers and the biggest problem is that the background ends up being gray if there is
    // a divider padding set.
    // This becomes difficult to solve since each button can have its individual background, much easier to just put
    // the dividers in between the buttons and allocate no space
    private LinearLayout dividerLayout;

    // View for placing border on top of the buttons
    // Background view for placing border on top of the buttons, background is transparent to see everything but border
    private BackgroundView borderView;

    // Array containing the buttons
    private ArrayList<SegmentedButton> buttons;

    // Drawable for the background, this will be a ColorDrawable in case a solid color is given
    private Drawable backgroundDrawable;
    // Drawable for the background when selected, this will be a ColorDrawable in case a solid color is given
    private Drawable selectedBackgroundDrawable;

    // Radius for rounding edges of the button group, in pixels (default value is 0)
    private int radius;

    // Position of the currently selected button, zero-indexed (default value is 0)
    // When animating, the position will be the previous value until after animation is finished
    private int position;

    // Whether or not the button can be dragged to a different position (default value is false)
    private boolean draggable;
    // When a user touches down on the currently selected button, this is set to be the difference between their
    // current X location of tapping and the currently selected button's left X coordinate
    // When user drags their finger across the button group, this value will be used to offset the current X location
    // with how much the button should have moved
    //
    // This value will be NaN when dragging is disabled
    private float dragOffsetX;

    // TODO Explain these
    private boolean ripple;
    private boolean hasRippleColor;
    private int rippleColor;

    // Animation interpolator for animating button movement
    // Android has some standard interpolator, e.g. BounceInterpolator, but also easy to create custom interpolator
    private Interpolator selectionAnimationInterpolator;
    // Duration in milliseconds for animating changing the selected button (default value is 500ms)
    private int selectionAnimationDuration;

    // Animation used for storing the current animation for changing the selected button
    ValueAnimator buttonAnimator;
    // Exact position of the currently selected button which includes its location during animation
    // The range is from 0.0f to the number of buttons - 1. (i.e. 0.0f -> 2.0f for 3 buttons)
    // A value of 2.25 would mean the left side of the selected button is 25% of the 3rd button.
    //
    // One particular use for saving this variable is in case the user selects a different button mid-animation, it
    // allows the animation to go from the current position to the newly selected position
    private float currentPosition;
    // Value that contains the last location of the left side of the selected button used for animating
    // For example, if the currentPosition was 2.25, then the lastPosition would be set to 2
    private int lastPosition;

    // TODO Add these
//    private OnPositionChangedListener onPositionChangedListener;
//    private OnClickedButtonListener onClickedButtonListener;

    // endregion

    // region Constructor

    public SegmentedButtonGroup(Context context) {
        super(context);

        init(context, null);
    }

    public SegmentedButtonGroup(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public SegmentedButtonGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SegmentedButtonGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        // Create and set outline provider for the segmented button group
        // This is used to provide an outline for the layout because it may have rounded corners
        // The primary benefit to using this is that shadows will follow the contour of the outline rather than the
        // rectangular bounds
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new OutlineProvider());
        }

        buttons = new ArrayList<>();

        // This FrameLayout is used in order to stack the button layout, border view & divider layout on top of each
        // other rather than horizontally or vertically like this SegmentedButtonGroup would do(it inherits from
        // LinearLayout)
        //
        // Why have a LinearLayout(SegmentedButtonGroup) with only one child of FrameLayout?
        // Although it seems redundant, it is so that SegmentedButton children can be specified in the layout XML of
        // the SegmentedButtonGroup with layout weight parameters. If SegmentedButtonGroup subclassed FrameLayout,
        // the layout weight would be ignored even though the SegmentedButtons are passed to buttonLayout.
        FrameLayout container = new FrameLayout(getContext());
        // Call super addView so that we do not trigger an Exception since only SegmentedButton instances can be
        // added to this view
        super.addView(container, -1, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));

        // Layout that contains all SegmentedButton's
        buttonLayout = new LinearLayout(getContext());
        buttonLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        container.addView(buttonLayout);

        // Create border view
        // This is essentially a dummy view that is drawn on top of the buttonLayout so that the border appears on
        // top of them
        borderView = new BackgroundView(context);
        borderView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(borderView);

        // Create layout that contains dividers for each button
        // This layout will essentially mirror the number of elements, size, weight of each element with the only
        // difference being that the elements will be transparent and that a divider will be placed between each one
        //
        // The benefit to placing the dividers in this dummy layout is so that the dividers appear on top of the
        // buttons without taking up additional width, which is what happens if the dividers are added to the
        // buttonLayout
        dividerLayout = new LinearLayout(getContext());
        dividerLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        dividerLayout.setOrientation(LinearLayout.HORIZONTAL);
        dividerLayout.setClickable(false);
        dividerLayout.setFocusable(false);
        container.addView(dividerLayout);

        // Retrieve custom attributes
        getAttributes(context, attrs);
    }

    private void getAttributes(Context context, @Nullable AttributeSet attrs) {
        // According to docs for obtainStyledAttributes, attrs can be null and I assume that each value will be set
        // to the default
        final TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SegmentedButtonGroup, 0, 0);

        // Load background if available, this can be a drawable or a color
        // In the instance of a color, a ColorDrawable is created and used instead
        // Note: Not well documented but getDrawable will return a ColorDrawable if a color is specified
        if (ta.hasValue(R.styleable.SegmentedButtonGroup_background)) {
            backgroundDrawable = ta.getDrawable(R.styleable.SegmentedButtonGroup_background);
        }

        // Load background on selection if available, can be drawable or color
        if (ta.hasValue(R.styleable.SegmentedButtonGroup_selectedBackground)) {
            selectedBackgroundDrawable = ta.getDrawable(R.styleable.SegmentedButtonGroup_selectedBackground);
        }

        // Note: Must read radius before setBorder call in order to round the border corners!
        radius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_radius, 0);

        // Setup border for button group
        // Width is the thickness of the border, color is the color of the border
        // Dash width and gap, if the dash width is not zero will make the border dashed with a ratio between dash
        // width and gap
        final int borderWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_borderWidth, 0);
        final int borderColor = ta.getColor(R.styleable.SegmentedButtonGroup_borderColor, Color.BLACK);
        final int borderDashWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_borderDashWidth, 0);
        final int borderDashGap = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_borderDashGap, 0);

        // Set the border to the read values, we don't store these border values because they just are just used to
        // create a GradientDrawable to set as the background. Not sure it's that likely that someone will want to
        // retrieve these values
        setBorder(borderWidth, borderColor, borderDashWidth, borderDashGap);

        position = ta.getInt(R.styleable.SegmentedButtonGroup_position, 0);
        draggable = ta.getBoolean(R.styleable.SegmentedButtonGroup_draggable, false);

        // Update clickable property
        // Not updating this property sets the clickable value to false by default but this sets the default to true
        // while keeping the clickable value if specified in the layouot XML
        setClickable(ta.getBoolean(R.styleable.SegmentedButtonGroup_android_clickable, true));

        // TODO Handle me
        ripple = ta.getBoolean(R.styleable.SegmentedButtonGroup_ripple, false);
        hasRippleColor = ta.hasValue(R.styleable.SegmentedButtonGroup_rippleColor);
        rippleColor = ta.getColor(R.styleable.SegmentedButtonGroup_rippleColor, Color.GRAY);

        final int dividerWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerWidth, 1);
        final int dividerRadius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerRadius, 0);
        final int dividerPadding = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerPadding, 0);

        final TypedValue value = new TypedValue();
        if (ta.getValue(R.styleable.SegmentedButtonGroup_divider, value)) {
            if (value.type == TypedValue.TYPE_REFERENCE || value.type == TypedValue.TYPE_STRING) {
                setDivider(ContextCompat.getDrawable(context, value.resourceId), dividerWidth, dividerRadius,
                        dividerPadding);
            } else if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                setDivider(value.data, dividerWidth, dividerRadius, dividerPadding);
            } else {
                throw new IllegalArgumentException("Invalid type for SegmentedButtonGroup divider in layout XML "
                        + "resource. Must be a color or drawable");
            }
        }

        int selectionAnimationInterpolator = ta.getInt(R.styleable.SegmentedButtonGroup_selectionAnimationInterpolator,
                ANIM_INTERPOLATOR_FAST_OUT_SLOW_IN);
        setSelectionAnimationInterpolator(selectionAnimationInterpolator);
        selectionAnimationDuration = ta.getInt(R.styleable.SegmentedButtonGroup_selectionAnimationDuration, 500);

        // Recycle the typed array, required once done using it
        ta.recycle();
    }

    // endregion

    // region Layout & Measure

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (child instanceof SegmentedButton) {
            SegmentedButton button = (SegmentedButton) child;

            // New position of the button will be the size of the buttons before the button is added
            // For example, if there are 5 buttons, then the indices are 0, 1, 2, 3, 4, so the next index is 5!
            final int position = buttons.size();

            // Give radius, default background and default selected background to the button
            // The default backgrounds will only update the background of the button if there is not a background set
            // on that button explicitly
            button.setBackgroundRadius(radius);
            button.setDefaultBackground(backgroundDrawable);
            button.setDefaultSelectedBackground(selectedBackgroundDrawable);

            // If this is the first item, set it as left-most button
            // Otherwise, notify previous button that it is not right-most anymore
            if (position == 0) {
                button.setIsLeftButton(true);
            } else {
                // Update previous button that it is not right-most anymore
                final SegmentedButton oldButton = buttons.get(position - 1);

                oldButton.setIsRightButton(false);

                // Update the background clip path for that button (removes rounding edges since it's not the
                // right-most)
                oldButton.setupBackgroundClipPath();
            }

            // Set current button as right-most regardless
            button.setIsRightButton(true);

            // Sets up the background clip path in order to correctly round background to match the radius
            button.setupBackgroundClipPath();

            // Add the button to the main group instead and store the button in our buttons list
            buttonLayout.addView(button, params);
            buttons.add(button);

            // If the given position to start at is this button, select it
            if (this.position == position) {
                updatePositions(position);
            }

            BackgroundView dividerView = new BackgroundView(getContext());
            dividerLayout.addView(dividerView, params);
            // TODO On update setLayoutParams
        } else {
            throw new IllegalArgumentException("Invalid child view for SegmentedButtonGroup. Only SegmentedButton's "
                    + "are valid children of the group");
        }
    }

    // endregion

    // region Events

    int getButtonPositionFromX(float x) {
        // TODO Comment me

        // Loop through each button
        for (int i = 0; i < buttons.size(); ++i) {
            final SegmentedButton button = buttons.get(i);

            // If x value is less than the right-hand side of the button, this is the selected button
            // Note: No need to check the left side of button because we assume each button is directly connected
            // from left to right
            if (x <= button.getRight()) {
                return i;
            }
        }

        // No reason it should ever reach this part
        throw new IllegalStateException(String.format("X position does not have a button in getButtonPositionFromX "
                + "(X = %f)", x));
    }

    float getButtonPositionFromXF(float x) {
        // TODO Comment me

        // Loop through each button
        for (int i = 0; i < buttons.size(); ++i) {
            final SegmentedButton button = buttons.get(i);

            if (x < button.getRight()) {
                return i + (x - button.getLeft()) / button.getWidth();
            }
        }

        // No reason it should ever reach this part
        throw new IllegalStateException(String.format("X position does not have a button in getButtonPositionFromXF "
                + "(X = %f)", x));
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        // Do not handle touch events if the view is disabled or not clickable
        // Oddly enough, the enabled and clickable states don't do anything unless specifically programmed into the
        // custom views
        if (!isEnabled() || !isClickable()) {
            return false;
        }

        // Selected button position
        final int position = getButtonPositionFromX(ev.getX());

        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                // Go to the selected button on touch up and animate too
                setPosition(position, true);
                break;

            case MotionEvent.ACTION_DOWN:
                // If buttons cannot be dragged or if the user is NOT pressing the currently selected button, then
                // just set the drag offset to be NaN meaning drag is not activated
                if (!draggable || this.position != position) {
                    dragOffsetX = Float.NaN;
                    break;
                }

                // Set drag offset in case user moves finger to initiate drag
                // Drag offset is the difference between finger current X location and the location of the selected
                // button
                //
                // Used in touch move for calculating the location of the button. Think of it as a delta. If the user
                // moves their finger 50px, then the button should move 50px. Thus, this delta will be subtracted
                // from the user's X value to get the location of where the button position should be.
                dragOffsetX = ev.getX() - buttons.get(position).getLeft();
                break;

            case MotionEvent.ACTION_MOVE:
                // Only drag if the drag offset is not NaN
                if (Float.isNaN(dragOffsetX)) {
                    break;
                }

                float newPosition = ev.getX() - dragOffsetX;
                float finalPosition = 0.0f;

                for (int i = 0; i < buttons.size(); ++i) {
                    final SegmentedButton button_ = buttons.get(i);

                    if (newPosition < button_.getRight()) {
                        finalPosition = i + (newPosition - button_.getLeft()) / button_.getWidth();
                        break;
                    }
                }

                Log.v(TAG, String.format("move: %f %f %f %f", ev.getX(), dragOffsetX, newPosition, finalPosition));
                moveSelectedButton(finalPosition);
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    // TODO Better name?
    public void setPosition(final int position, boolean animate) {
        // Do nothing and return if our current position is already the position given
        if (position == this.position && (buttonAnimator != null && !buttonAnimator.isRunning())) {
            return;
        }

        if (!animate || selectionAnimationInterpolator == null) {
            updatePositions(position);
            return;
        }

        // Animate value from current position to the new position
        // Fraction positions such as 1.25 means we are 75% in button 1 and 25% in button 2.
        buttonAnimator = ValueAnimator.ofFloat(currentPosition, position);

        // Buttons are animated from this listener
        buttonAnimator.addUpdateListener(animation -> {
            moveSelectedButton((float) animation.getAnimatedValue());
        });

        buttonAnimator.setDuration(selectionAnimationDuration);
        buttonAnimator.setInterpolator(selectionAnimationInterpolator);
        buttonAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animation, final boolean isReverse) {
                updatePositions(position);
            }
        });
        buttonAnimator.start();
    }

    private void moveSelectedButton(float position) {
        // Update current position to be the animated value
        // This is a float value indicating where the left-side of the button is located
        // For example, a currentPosition of 1.0 would mean all of button 1 was selected
        // But a currentPosition of 1.5 would mean half of button 1 was selected and half of button 2 (its in
        // between animating the value)
        //
        // endPosition is the position where the right side of the button should be and that is always 1 + the
        // current position. For example, a currentPosition of 1.5 means the right half of the button is at 2.5.
        // This is always 1.0 because the button selection width is assumed to be the same size as the button
        currentPosition = position;

        // Get the current button position and extract the offset. For example, a currentPosition of 2.25 would
        // result in a currentButtonPosition of 2 and the currentOffset to 0.25.
        final int currentButtonPosition = (int) currentPosition;
        final float currentOffset = currentPosition - currentButtonPosition;

        // Get the current end button position and extract the offset. For example, a currentEndPosition of 2.25
        // would result in a currentEndButtonPosition of 2 and the currentEndOffset to 0.25.
        final int currentEndButtonPosition = currentButtonPosition + 1;

        // Grab the current button from the position and clip the right side of the button to show the
        // appropriate amount
        //
        // For example, if the currentOffset was 0.25, then the right 75% of the button would be showing. The
        // offset given to the current button is 0.0f -> 1.0f and represents the relative X position to clip the
        // button at (going all the way to the right side of the button, i.e. 0.25 all the way to 1.0)
        final SegmentedButton currentButton = buttons.get(currentButtonPosition);
        currentButton.clipRight(currentOffset);

        // For the end button, we want to clip the left side of the button to match up with the right side of the
        // previous button. However, there is a slight chance the end button position might be out of range so we
        // check if it is first
        if (currentEndButtonPosition >= 0 && currentEndButtonPosition < buttons.size()) {
            // Grab the button directly to the right of the current button and clip the left
            final SegmentedButton currentEndButton = buttons.get(currentEndButtonPosition);
            currentEndButton.clipLeft(currentOffset);

            // TODO Add feature for speed per button to make it not so bad?
        }

        if (lastPosition != currentButtonPosition && lastPosition != currentEndButtonPosition) {
            buttons.get(lastPosition).clipRight(1.0f);
        }

        final int lastEndPosition = lastPosition + 1;

        if (lastEndPosition != currentEndButtonPosition && lastEndPosition != currentButtonPosition
                && lastEndPosition < buttons.size()) {
            buttons.get(lastEndPosition).clipRight(1.0f);
        }

        lastPosition = currentButtonPosition;

        // Notify to redraw buttons
        invalidate();
    }

    private void updatePositions(final int position) {
        this.position = position;
        this.currentPosition = position;
        this.lastPosition = position;

        for (int i = 0; i < buttons.size(); ++i) {
            final SegmentedButton button = buttons.get(i);

            if (i == position) {
                button.clipRight(0.0f);
            } else {
                button.clipRight(1.0f);
            }
        }
    }

    // endregion

    // region Getters & Setters

    public Interpolator getSelectionAnimationInterpolator() {
        return selectionAnimationInterpolator;
    }

    public void setSelectionAnimationInterpolator(@Nullable Interpolator interpolator) {
        selectionAnimationInterpolator = interpolator;
    }

    public void setSelectionAnimationInterpolator(@AnimationInterpolator int interpolator) {
        switch (interpolator) {
            case ANIM_INTERPOLATOR_NONE:
                selectionAnimationInterpolator = null;
                break;

            case ANIM_INTERPOLATOR_FAST_OUT_SLOW_IN:
                selectionAnimationInterpolator = new FastOutSlowInInterpolator();
                break;

            case ANIM_INTERPOLATOR_BOUNCE:
                selectionAnimationInterpolator = new BounceInterpolator();
                break;

            case ANIM_INTERPOLATOR_LINEAR:
                selectionAnimationInterpolator = new LinearInterpolator();
                break;

            case ANIM_INTERPOLATOR_DECELERATE:
                selectionAnimationInterpolator = new DecelerateInterpolator();
                break;

            case ANIM_INTERPOLATOR_CYCLE:
                selectionAnimationInterpolator = new CycleInterpolator(1.0f);
                break;

            case ANIM_INTERPOLATOR_ANTICIPATE:
                selectionAnimationInterpolator = new AnticipateInterpolator();
                break;

            case ANIM_INTERPOLATOR_ACCELERATE_DECELERATE:
                selectionAnimationInterpolator = new AccelerateDecelerateInterpolator();
                break;

            case ANIM_INTERPOLATOR_ACCELERATE:
                selectionAnimationInterpolator = new AccelerateInterpolator();
                break;

            case ANIM_INTERPOLATOR_ANTICIPATE_OVERSHOOT:
                selectionAnimationInterpolator = new AnticipateOvershootInterpolator();
                break;

            case ANIM_INTERPOLATOR_FAST_OUT_LINEAR_IN:
                selectionAnimationInterpolator = new FastOutLinearInInterpolator();
                break;

            case ANIM_INTERPOLATOR_LINEAR_OUT_SLOW_IN:
                selectionAnimationInterpolator = new LinearOutSlowInInterpolator();
                break;

            case ANIM_INTERPOLATOR_OVERSHOOT:
                selectionAnimationInterpolator = new OvershootInterpolator();
                break;
        }
    }

    public void setBorder(int width, @ColorInt int color, int dashWidth, int dashGap) {
        // Width of the border in pixels (default value is 0px or no border)
//        private int borderWidth;
        // Color of the border (default color is black)
//        private int borderColor;
        // Parameters for defining a dashed border line. If dash width is 0px, then the border will be solid
        // The border dash width is the width, in pixels, of the dash while the border dash gap is the width of the gap
        // between dashes, in pixels.
//        private int borderDashWidth, borderDashGap;

        // Border width of 0 indicates to hide borders
        if (width > 0) {
            GradientDrawable borderDrawable = new GradientDrawable();
            // Corner radius is the radius minus half of the border width because the drawable will draw the stroke
            // from the center, so the actual corner radius is reduced
            // If the half border width is left out, the border radius does not follow the curve of the background
            borderDrawable.setCornerRadius(radius - width / 2.0f);
            borderDrawable.setStroke(width, color, dashWidth, dashGap);

            borderView.setBackground(borderDrawable);
        } else {
            borderView.setBackground(null);
        }
    }

    public void setDivider(@Nullable Drawable drawable, int width, int radius, int padding) {
        // TODO Explain these

        // Drawable of null indicates that we want to hide dividers
        if (drawable == null) {
            dividerLayout.setDividerDrawable(null);
            dividerLayout.setShowDividers(SHOW_DIVIDER_NONE);
            return;
        }

        // Set the corner radius and size if the drawable is a GradientDrawable
        // Otherwise just set the divider drawable like normal because we cant set the parameters
        if (drawable instanceof GradientDrawable) {
            GradientDrawable gradient = (GradientDrawable) drawable;
            gradient.setSize(width, 0);
            gradient.setCornerRadius(radius);

            dividerLayout.setDividerDrawable(gradient);
        } else {
            dividerLayout.setDividerDrawable(drawable);
        }

        dividerLayout.setDividerPadding(padding);
        dividerLayout.setShowDividers(SHOW_DIVIDER_MIDDLE);
    }

    public void setDivider(@ColorInt int color, int width, int radius, int padding) {
        // TODO Explain these

        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{color, color});

        drawable.setCornerRadius(radius);
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setSize(width, 0);

        dividerLayout.setDividerDrawable(drawable);
        dividerLayout.setDividerPadding(padding);
        dividerLayout.setShowDividers(SHOW_DIVIDER_MIDDLE);
    }

    // endregion

    // region Untouched

//    private void setBackgroundColor(View v, Drawable d, int c) {
//        if (null != d) {
//            BackgroundHelper.setBackground(v, d);
//        } else {
//            v.setBackgroundColor(c);
//        }
//    }
//
//    private void setDividerAttrs() {
//        if (!hasDivider) {
//            return;
//        }
//        dividerLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
//        // Divider Views
//        RoundHelper.makeDividerRound(dividerLayout, dividerColor, dividerRadius, dividerSize,
//                dividerBackgroundDrawable);
//    }

//    private void setRipple(View v, boolean isClickable) {
//        if (isClickable) {
//            if (hasRippleColor) {
//                RippleHelper.setRipple(v, rippleColor, radius);
//            } else if (ripple) {
//                RippleHelper.setSelectableItemBackground(getContext(), v);
//            } else {
//                for (View button : buttons) {
//                    if (button instanceof SegmentedButton && ((SegmentedButton) button).hasRipple()) {
//                        RippleHelper.setRipple(v, ((SegmentedButton) button).getRippleColor(), radius);
//                    }
//                }
//            }
//        } else {
//            BackgroundHelper.setBackground(v, null);
//        }
//    }
//
//    /**
//     * @param onPositionChangedListener set your instance that you have created to listen any position change
//     */
//    public void setOnPositionChangedListener(OnPositionChangedListener onPositionChangedListener) {
//        this.onPositionChangedListener = onPositionChangedListener;
//    }
//
//    /**
//     * Use this listener if you want to know any position change.
//     * Listener is called when one of segmented button is clicked or setPosition is called.
//     */
//    public interface OnPositionChangedListener {
//
//        void onPositionChanged(int position);
//    }
//
//    /**
//     * @param onClickedButtonListener set your instance that you have created to listen clicked positions
//     */
//    public void setOnClickedButtonListener(OnClickedButtonListener onClickedButtonListener) {
//        this.onClickedButtonListener = onClickedButtonListener;
//    }
//
//    /**
//     * Use this listener if  you want to know which button is clicked.
//     * Listener is called when one of segmented button is clicked
//     */
//    public interface OnClickedButtonListener {
//
//        void onClickedButton(int position);
//    }
//
//    /**
//     * @param position is used to select one of segmented buttons
//     */
//    public void setPosition(int position) {
//        this.position = position;
//
//        if (null == buttons) {
//            lastPosition = toggledPosition = position;
//            lastPositionOffset = toggledPositionOffset = (float) position;
//        } else {
//            toggle(position, animateSelectorDuration, false);
//        }
//    }
//
//    /**
//     * @param position is used to select one of segmented buttons
//     * @param duration determines how long animation takes to finish
//     */
//    public void setPosition(int position, int duration) {
//        this.position = position;
//
//        if (null == buttons) {
//            lastPosition = toggledPosition = position;
//            lastPositionOffset = toggledPositionOffset = (float) position;
//        } else {
//            toggle(position, duration, false);
//        }
//    }
//
//    /**
//     * @param position      is used to select one of segmented buttons
//     * @param withAnimation if true default animation will perform
//     */
//    public void setPosition(int position, boolean withAnimation) {
//        this.position = position;
//
//        if (null == buttons) {
//            lastPosition = toggledPosition = position;
//            lastPositionOffset = toggledPositionOffset = (float) position;
//        } else {
//            if (withAnimation) {
//                toggle(position, animateSelectorDuration, false);
//            } else {
//                toggle(position, 1, false);
//            }
//        }
//    }
//
//    /**
//     * @param selectorColor sets color to selector
//     *                      default: Color.GRAY
//     */
//    public void setSelectorColor(int selectorColor) {
//        this.selectorColor = selectorColor;
//    }
//
//    /**
//     * @param backgroundColor sets background color of whole layout including buttons on top of it
//     *                        default: Color.WHITE
//     */
//    @Override
//    public void setBackgroundColor(int backgroundColor) {
//        this.backgroundColor = backgroundColor;
//    }
//
//    /**
//     * @param ripple applies android's default ripple on layout
//     */
//    public void setRipple(boolean ripple) {
//        this.ripple = ripple;
//    }
//
//    /**
//     * @param rippleColor sets ripple color and adds ripple when a button is hovered
//     *                    default: Color.GRAY
//     */
//    public void setRippleColor(int rippleColor) {
//        this.rippleColor = rippleColor;
//    }
//
//    /**
//     * @param hasRippleColor if true ripple will be shown.
//     *                       if setRipple(boolean) is also set to false, there will be no ripple
//     */
//    public void setRippleColor(boolean hasRippleColor) {
//        this.hasRippleColor = hasRippleColor;
//    }
//
//    /**
//     * @param radius determines how round layout's corners should be
//     */
//    public void setRadius(int radius) {
//        this.radius = radius;
//    }
//
//    /**
//     * @param dividerPadding adjusts divider's top and bottom distance to its container
//     */
//    public void setDividerPadding(int dividerPadding) {
//        this.dividerPadding = dividerPadding;
//    }
//
//    /**
//     * @param animateSelectorDuration sets how long selector animation should last
//     */
//    public void setSelectorAnimationDuration(int animateSelectorDuration) {
//        this.animateSelectorDuration = animateSelectorDuration;
//    }
//
//    /**
//     * @param animateSelector is used to give an animation to selector with the given interpolator constant
//     */
//    public void setSelectorAnimation(int animateSelector) {
//        this.animateSelector = animateSelector;
//    }
//
//    /**
//     * @param interpolatorSelector is used to give an animation to selector with the given one of android's
//     *                             interpolator.
//     *                             Ex: {@link FastOutSlowInInterpolator}, {@link BounceInterpolator}, {@link
//     *                             LinearInterpolator}
//     */
//    public void setInterpolatorSelector(Interpolator interpolatorSelector) {
//        this.interpolatorSelector = interpolatorSelector;
//    }
//
//    /**
//     * @param dividerColor changes divider's color with the given one
//     *                     default: Color.WHITE
//     */
//    public void setDividerColor(int dividerColor) {
//        this.dividerColor = dividerColor;
//        RoundHelper.makeDividerRound(dividerLayout, dividerColor, dividerRadius, dividerSize,
//                dividerBackgroundDrawable);
//    }
//
//    /**
//     * @param dividerSize sets thickness of divider
//     *                    default: 0
//     */
//    public void setDividerSize(int dividerSize) {
//        this.dividerSize = dividerSize;
//        RoundHelper.makeDividerRound(dividerLayout, dividerColor, dividerRadius, dividerSize,
//                dividerBackgroundDrawable);
//    }
//
//    /**
//     * @param dividerRadius determines how round divider should be
//     *                      default: 0
//     */
//    public void setDividerRadius(int dividerRadius) {
//        this.dividerRadius = dividerRadius;
//        RoundHelper.makeDividerRound(dividerLayout, dividerColor, dividerRadius, dividerSize,
//                dividerBackgroundDrawable);
//    }
//
//    /**
//     * @param hasDivider if true divider will be shown.
//     */
//    public void setDivider(boolean hasDivider) {
//        this.hasDivider = hasDivider;
//    }
//
//    /**
//     * @param borderSize sets thickness of border
//     *                   default: 0
//     */
//    public void setBorderSize(int borderSize) {
//        this.borderSize = borderSize;
//    }
//
//    /**
//     * @param borderColor sets border color to the given one
//     *                    default: Color.BLACK
//     */
//    public void setBorderColor(int borderColor) {
//        this.borderColor = borderColor;
//    }
//
//    public int getDividerSize() {
//        return dividerSize;
//    }
//
//    public int getRippleColor() {
//        return rippleColor;
//    }
//
//    public int getSelectorColor() {
//        return selectorColor;
//    }
//
//    public int getSelectorAnimation() {
//        return animateSelector;
//    }
//
//    public int getSelectorAnimationDuration() {
//        return animateSelectorDuration;
//    }
//
//    public int getPosition() {
//        return position;
//    }
//
//    public int getBackgroundColor() {
//        return backgroundColor;
//    }
//
//    public int getDividerColor() {
//        return dividerColor;
//    }
//
//    public float getRadius() {
//        return radius;
//    }
//
//    public int getDividerPadding() {
//        return dividerPadding;
//    }
//
//    public float getDividerRadius() {
//        return dividerRadius;
//    }
//
//    public boolean isHasDivider() {
//        return hasDivider;
//    }
//
//    public boolean isHasRippleColor() {
//        return hasRippleColor;
//    }
//
//    public boolean isRipple() {
//        return ripple;
//    }
//
//    public Interpolator getInterpolatorSelector() {
//        return interpolatorSelector;
//    }

//    private void setRippleState(boolean state) {
//        for (View v : ripples) {
//            setRipple(v, state);
//        }
//    }
//
//    private void setEnabledAlpha(boolean enabled) {
//        float alpha = 1f;
//        if (!enabled) {
//            alpha = 0.5f;
//        }
//
//        setAlpha(alpha);
//    }
//
//
//    /**
//     * @param enabled set it to:
//     *                false, if you want buttons to be unclickable and add grayish looking which gives disabled look,
//     *                true, if you want buttons to be clickable and remove grayish looking
//     */
//    @Override
//    public void setEnabled(boolean enabled) {
//        this.enabled = enabled;
//        setRippleState(enabled);
//        setEnabledAlpha(enabled);
//    }
//
//    /**
//     * @param clickable set it to:
//     *                  false for unclickable buttons,
//     *                  true for clickable buttons
//     */
//    @Override
//    public void setClickable(boolean clickable) {
//        this.clickable = clickable;
//        setRippleState(clickable);
//    }
//
//    @Override
//    public Parcelable onSaveInstanceState() {
//        Bundle bundle = new Bundle();
//        bundle.putParcelable("state", super.onSaveInstanceState());
//        bundle.putInt("position", position);
//        return bundle;
//    }
//
//    @Override
//    public void onRestoreInstanceState(Parcelable state) {
//        if (state instanceof Bundle) {
//            Bundle bundle = (Bundle) state;
//            position = bundle.getInt("position");
//            state = bundle.getParcelable("state");
//
//            setPosition(position, false);
//        }
//        super.onRestoreInstanceState(state);
//    }

    // endregion

    // region Classes

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private class OutlineProvider extends ViewOutlineProvider {
        // This class is used to define an outline for this view
        // This is necessary because the view may have rounded corners
        // Primary benefit is that shadows will follow contours of the outline rather than rectangular bounds

        @Override
        public void getOutline(final View view, final Outline outline) {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    }

    // endregion
}
