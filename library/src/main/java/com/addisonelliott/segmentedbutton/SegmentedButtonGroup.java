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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
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
import java.util.List;

public class SegmentedButtonGroup extends LinearLayout
{
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
    @IntDef({
        ANIM_INTERPOLATOR_NONE, ANIM_INTERPOLATOR_FAST_OUT_SLOW_IN, ANIM_INTERPOLATOR_BOUNCE,
        ANIM_INTERPOLATOR_LINEAR, ANIM_INTERPOLATOR_DECELERATE, ANIM_INTERPOLATOR_CYCLE,
        ANIM_INTERPOLATOR_ANTICIPATE, ANIM_INTERPOLATOR_ACCELERATE_DECELERATE, ANIM_INTERPOLATOR_ACCELERATE,
        ANIM_INTERPOLATOR_ANTICIPATE_OVERSHOOT, ANIM_INTERPOLATOR_FAST_OUT_LINEAR_IN,
        ANIM_INTERPOLATOR_LINEAR_OUT_SLOW_IN, ANIM_INTERPOLATOR_OVERSHOOT
    })
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
    private EmptyView borderView;

    // Array containing the buttons
    private ArrayList<SegmentedButton> buttons;

    // Drawable for the background, this will be a ColorDrawable in case a solid color is given
    private Drawable backgroundDrawable;
    // Drawable for the background when selected, this will be a ColorDrawable in case a solid color is given
    private Drawable selectedBackgroundDrawable;

    // Width of the border in pixels (default value is 0px for no border)
    private int borderWidth;
    // Color of the border (default color is black)
    private int borderColor;
    // Width of the dash for border, in pixels. Value of 0px means solid line (default is 0px)
    private int borderDashWidth;
    // Width of the gap for border, in pixels. Only relevant when dashWidth is greater than 0px
    private int borderDashGap;

    // This is the border information for the selected button with the same defaults as above for the button group
    // border
    private int selectedBorderWidth;
    private int selectedBorderColor;
    private int selectedBorderDashWidth;
    private int selectedBorderDashGap;

    // Radius for rounding edges of the button group, in pixels (default value is 0)
    private int radius;
    // Radius for rounding edges of the selected button, in pixels (default value is 0)
    private int selectedButtonRadius;

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

    // Whether or not ripple is enabled for animating when any button is pressed (default is true)
    private boolean ripple;
    // Whether or not a ripple color was specified
    private boolean hasRippleColor;
    // Color of the ripple to display over the buttons (default value is gray)
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

    // Listener to notify when position is changed
    // Note: Position change will be notified after animations complete because that is when the position actually
    // changes
    private OnPositionChangedListener onPositionChangedListener;

    // endregion

    // region Constructor

    public SegmentedButtonGroup(Context context)
    {
        super(context);

        init(context, null);
    }

    public SegmentedButtonGroup(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        init(context, attrs);
    }

    public SegmentedButtonGroup(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SegmentedButtonGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super(context, attrs, defStyleAttr, defStyleRes);

        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs)
    {
        // Create and set outline provider for the segmented button group
        // This is used to provide an outline for the layout because it may have rounded corners
        // The primary benefit to using this is that shadows will follow the contour of the outline rather than the
        // rectangular bounds
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
            setOutlineProvider(new OutlineProvider());

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
        borderView = new EmptyView(context);
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

    private void getAttributes(Context context, @Nullable AttributeSet attrs)
    {
        // According to docs for obtainStyledAttributes, attrs can be null and I assume that each value will be set
        // to the default
        final TypedArray ta = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SegmentedButtonGroup, 0, 0);

        // Load background if available, this can be a drawable or a color
        // Note: Not well documented but getDrawable will return a ColorDrawable if a color is specified
        if (ta.hasValue(R.styleable.SegmentedButtonGroup_android_background))
            backgroundDrawable = ta.getDrawable(R.styleable.SegmentedButtonGroup_android_background);

        // Load background on selection if available, can be drawable or color
        if (ta.hasValue(R.styleable.SegmentedButtonGroup_sbg_selectedBackground))
            selectedBackgroundDrawable = ta.getDrawable(R.styleable.SegmentedButtonGroup_sbg_selectedBackground);

        // Note: Must read radius before setBorder call in order to round the border corners!
        radius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_radius, 0);
        selectedButtonRadius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_selectedButtonRadius, 0);

        // Setup border for button group
        // Width is the thickness of the border, color is the color of the border
        // Dash width and gap, if the dash width is not zero will make the border dashed with a ratio between dash
        // width and gap
        borderWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_borderWidth, 0);
        borderColor = ta.getColor(R.styleable.SegmentedButtonGroup_sbg_borderColor, Color.BLACK);
        borderDashWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_borderDashWidth, 0);
        borderDashGap = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_borderDashGap, 0);

        // Set the border to the read values, this will set the border values to itself but will create a
        // GradientDrawable containing the border
        setBorder(borderWidth, borderColor, borderDashWidth, borderDashGap);

        // Get border information for the selected button
        // Same defaults as the border above, however this border information will be passed to each button so that
        // the correct border can be rendered around the selected button
        selectedBorderWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_selectedBorderWidth, 0);
        selectedBorderColor = ta.getColor(R.styleable.SegmentedButtonGroup_sbg_selectedBorderColor, Color.BLACK);
        selectedBorderDashWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_selectedBorderDashWidth, 0);
        selectedBorderDashGap = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_selectedBorderDashGap, 0);

        position = ta.getInt(R.styleable.SegmentedButtonGroup_sbg_position, 0);
        draggable = ta.getBoolean(R.styleable.SegmentedButtonGroup_sbg_draggable, false);

        // Update clickable property
        // Not updating this property sets the clickable value to false by default but this sets the default to true
        // while keeping the clickable value if specified in the layouot XML
        setClickable(ta.getBoolean(R.styleable.SegmentedButtonGroup_android_clickable, true));

        ripple = ta.getBoolean(R.styleable.SegmentedButtonGroup_sbg_ripple, true);
        hasRippleColor = ta.hasValue(R.styleable.SegmentedButtonGroup_sbg_rippleColor);
        rippleColor = ta.getColor(R.styleable.SegmentedButtonGroup_sbg_rippleColor, Color.GRAY);

        final int dividerWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_dividerWidth, 1);
        final int dividerRadius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_dividerRadius, 0);
        final int dividerPadding = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_sbg_dividerPadding, 0);

        // Load divider value if available, the divider can be either a drawable resource or a color
        // Load the TypedValue first and check the type to determine if color or drawable
        final TypedValue value = new TypedValue();
        if (ta.getValue(R.styleable.SegmentedButtonGroup_sbg_divider, value))
        {
            if (value.type == TypedValue.TYPE_REFERENCE || value.type == TypedValue.TYPE_STRING)
            {
                // Note: Odd case where Android Studio layout preview editor will fail to display a
                // SegmentedButtonGroup with a divider drawable because value.resourceId returns 0 and thus
                // ContextCompat.getDrawable will return NullPointerException
                // Loading drawable TypedArray.getDrawable or doing TypedArray.getResourceId fixes the problem
                if (isInEditMode())
                {
                    setDivider(ta.getDrawable(R.styleable.SegmentedButtonGroup_sbg_divider), dividerWidth, dividerRadius,
                        dividerPadding);
                }
                else
                {
                    setDivider(ContextCompat.getDrawable(context, value.resourceId), dividerWidth, dividerRadius,
                        dividerPadding);
                }
            }
            else if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT)
            {
                // Divider is a color, value.data is the color value
                setDivider(value.data, dividerWidth, dividerRadius, dividerPadding);
            }
            else
            {
                // Invalid type for the divider, throw an exception
                throw new IllegalArgumentException("Invalid type for SegmentedButtonGroup divider in layout XML "
                    + "resource. Must be a color or drawable");
            }
        }

        int selectionAnimationInterpolator = ta.getInt(R.styleable.SegmentedButtonGroup_sbg_selectionAnimationInterpolator,
            ANIM_INTERPOLATOR_FAST_OUT_SLOW_IN);
        setSelectionAnimationInterpolator(selectionAnimationInterpolator);
        selectionAnimationDuration = ta.getInt(R.styleable.SegmentedButtonGroup_sbg_selectionAnimationDuration, 500);

        // Recycle the typed array, required once done using it
        ta.recycle();
    }

    // endregion

    // region Layout & Measure

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params)
    {
        if (child instanceof SegmentedButton)
        {
            final SegmentedButton button = (SegmentedButton)child;

            // New position of the button will be the size of the buttons before the button is added
            // For example, if there are 5 buttons, then the indices are 0, 1, 2, 3, 4, so the next index is 5!
            final int position = buttons.size();

            // Give radius, selected button radius, default background and default selected background to the button
            // The default backgrounds will only update the background of the button if there is not a background set
            // on that button explicitly
            button.setBackgroundRadius(radius);
            button.setSelectedButtonRadius(selectedButtonRadius);
            button.setDefaultBackground(backgroundDrawable);
            button.setDefaultSelectedBackground(selectedBackgroundDrawable);

            // Setup listener that detects changes in visibility for the buttons
            button._setOnVisibilityChangedListener((button1, visibility) -> {
                // Mimic visibility for the corresponding divider (i.e. make visible if parent is visible, invisible
                // otherwise)
                final int index1 = SegmentedButtonGroup.this.buttonLayout.indexOfChild(button1);
                SegmentedButtonGroup.this.dividerLayout.getChildAt(index1).setVisibility(visibility);

                // Find the first visible button to the left of this button (or null if none)
                SegmentedButton leftButton = null;
                for (int i = index1 - 1; i >= 0; --i)
                {
                    final SegmentedButton button_ = buttons.get(i);
                    if (button_.getVisibility() != GONE)
                    {
                        leftButton = button_;
                        break;
                    }
                }

                // Find the first visible button to the right of this button (or null if none)
                SegmentedButton rightButton = null;
                for (int i = index1 + 1; i < buttons.size(); ++i)
                {
                    final SegmentedButton button_ = buttons.get(i);
                    if (button_.getVisibility() != GONE)
                    {
                        rightButton = button_;
                        break;
                    }
                }

                // Below, we update the buttons leftButton and rightButton properties
                // Think of the buttons in the group like a chain, each button knows about the button to the left and
                // right of itself.
                if (visibility == GONE)
                {
                    // This button is being hidden, we leave the left/right button properties alone because they dont
                    // matter
                    //
                    // Update the "chain" of buttons so that the first visible left button is linked to the first
                    // visible right button
                    if (leftButton != null)
                    {
                        leftButton.setRightButton(rightButton);
                        leftButton.setupBackgroundClipPath();
                    }

                    // Update the "chain" of buttons so that the first visible right button is linked to the first
                    // visible left button
                    if (rightButton != null)
                    {
                        rightButton.setLeftButton(leftButton);
                        rightButton.setupBackgroundClipPath();
                    }
                }
                else
                {
                    // This button is being shown again, we update the left/right button to be the first visible ones
                    button1.setLeftButton(leftButton);
                    button1.setRightButton(rightButton);
                    button1.setupBackgroundClipPath();

                    // Update the "chain" of buttons so that the left button points to this button now
                    if (leftButton != null)
                    {
                        leftButton.setRightButton(button1);
                        leftButton.setupBackgroundClipPath();
                    }

                    // Update the "chain" of buttons so that the right button points to this button now
                    if (rightButton != null)
                    {
                        rightButton.setLeftButton(button1);
                        rightButton.setupBackgroundClipPath();
                    }
                }
            });

            // Setup button with ripple if enabled and a color is given
            // Otherwise disable ripple on the button if ripple is disabled
            // The ripple color is only passed to the buttons if a color is specified, otherwise the default color is
            // used from the button itself
            if (ripple && hasRippleColor)
            {
                // Set button ripple color only if a value was given globally
                button.setRipple(rippleColor);
            }
            else if (!ripple)
            {
                // Disable the ripple on the button
                button.setRipple(false);
            }

            // If this is NOT the first item in the group, then update the previous button and this button with its
            // respective right button and left button.
            if (position != 0)
            {
                // Find the first visible button to the left of this button (or null if none)
                SegmentedButton leftButton = null;
                for (int i = buttons.size() - 1; i >= 0; --i)
                {
                    final SegmentedButton button_ = buttons.get(i);
                    if (button_.getVisibility() != GONE)
                    {
                        leftButton = button_;
                        break;
                    }
                }

                // If there is a visible button to the left, then set it to point to this button if its visible or
                // otherwise null to treat it as an end button
                if (leftButton != null)
                {
                    leftButton.setRightButton(button.getVisibility() != GONE ? button : null);
                    // Update background clip path for that button since it may need to add/remove round edges
                    leftButton.setupBackgroundClipPath();
                }

                // Always set this button to point to the leftmost button
                // In the case this button is not visible, then it does not matter since it wont be drawn
                button.setLeftButton(leftButton);
            }

            // Sets up the background clip path, selected button clip path, and selected button border
            button.setupBackgroundClipPath();
            button.setupSelectedButtonClipPath();
            button.setSelectedButtonBorder(selectedBorderWidth, selectedBorderColor, selectedBorderDashWidth,
                selectedBorderDashGap);

            // Add the button to the main group instead and store the button in our buttons list
            buttonLayout.addView(button, params);
            buttons.add(button);

            // If the given position to start at is this button, select it
            if (this.position == position)
                updateSelectedPosition(position);

            // Add a divider view to the divider layout that mimics the size of the button
            // This view is used as essentially a spacer for the dividers in the divider layout
            // The divider view needs to know the divider width in order to offset the width correctly
            ButtonActor dividerView = new ButtonActor(getContext());
            dividerView.setButton(button);
            dividerView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            dividerView.setVisibility(button.getVisibility());
            Drawable dividerDrawable = dividerLayout.getDividerDrawable();
            if (dividerDrawable != null)
                dividerView.setDividerWidth(dividerDrawable.getIntrinsicWidth());
            dividerLayout.addView(dividerView);
        }
        else
        {
            // Not allowed to have children of any type besides SegmentedButton
            throw new IllegalArgumentException("Invalid child view for SegmentedButtonGroup. Only SegmentedButton's "
                + "are valid children of the group");
        }
    }

    // endregion

    // region Events

    /**
     * Return button position for corresponding button that contains the X coordinate within its bounds
     *
     * This returns a whole-number indicating the index of the button that corresponds to the given X coordinate.
     *
     * @param x X screen coordinate
     * @return int representing index of the corresponding button
     * @throws IllegalStateException if no button contains the coordinate within its bounds
     */
    int getButtonPositionFromX(float x)
    {
        // Loop through each button
        int i = 0;
        for (; i < buttons.size(); ++i)
        {
            final SegmentedButton button = buttons.get(i);

            // If x value is less than the right-hand side of the button, this is the selected button
            // Note: No need to check the left side of button because we assume each button is directly connected
            // from left to right
            if (button.getVisibility() != GONE && x <= button.getRight())
                break;
        }

        // Return last button if x value is out of bounds
        return Math.min(i, buttons.size() - 1);
    }

    /**
     * Return fractional button position for corresponding button that contains the X coordinate within its bounds
     *
     * The whole part of the floating number returned indicates the index of the button that contains the coordinate.
     * The fractional part of the returned floating number represents the position of the coordinate within the buttons
     * bounds relative to its width.
     *
     * For example, a returned button position of 2.25 would indicate that the coordinate was within the 3rd button
     * and 1/4 (25%) of the width of the button.
     *
     * @param x X screen coordinate
     * @return float representing the fractional position of the corresponding button
     * @throws IllegalStateException if no button contains the coordinate within its bounds
     */
    float getButtonPositionFromXF(float x)
    {
        // Loop through each button
        int i = 0;
        for (; i < buttons.size(); ++i)
        {
            final SegmentedButton button = buttons.get(i);

            // If x value is less than the right-hand side of the button, this is the selected button
            // Note: No need to check the left side of button because we assume each button is directly connected
            // from left to right
            if (button.getVisibility() != GONE && x < button.getRight())
                return i + (x - button.getLeft()) / button.getWidth();
        }

        // Return last button if x value is out of bounds
        return (float)i;
    }

    /**
     * Handles touch events by the user in order to handle tapping a new button or dragging the selected button
     *
     * Note: dispatchTouchEvent is used here rather than onTouchEvent to allow for touch events to be handled by the
     * SegmentedButtonGroup AND SegmentedButton. SegmentedButton needs onTouch events in order to correctly
     * displaying ripple animations on tap.
     *
     * This cannot be done with onTouchEvent because one View must intercept and consume the events. This method
     * handles the touch event and then passes to the SegmentedButton to consume the touch event.
     *
     * @hide
     */
    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev)
    {
        // Do not handle touch events if the view is disabled or not clickable
        // Oddly enough, the enabled and clickable states don't do anything unless specifically programmed into the
        // custom views
        if (!isEnabled() || !isClickable())
            return false;

        switch (ev.getAction())
        {
            case MotionEvent.ACTION_UP:
            {
                // Selected button position
                final int position = getButtonPositionFromX(ev.getX());

                // Go to the selected button on touch up and animate too
                //
                // In the case that the user has been dragging the button, this will have the effect of "snapping" to
                // the nearest button
                setPosition(position, true);

                // Enable scroll touch event interception again now that we're done dragging
                requestDisallowInterceptTouchEvent(false);
            }
            break;

            case MotionEvent.ACTION_DOWN:
            {
                // Selected button position
                final int position = getButtonPositionFromX(ev.getX());

                // If button cannot be dragged, user is NOT pressing the currently selected button or the button is
                // being animated, then just set drag offset to NaN meaning drag is not activated
                if (!draggable || this.position != position || (buttonAnimator != null && buttonAnimator.isRunning()))
                {
                    dragOffsetX = Float.NaN;
                    break;
                }

                // Since the user is now officially dragging the button, we want to disable scrolling interception
                // of touch events
                requestDisallowInterceptTouchEvent(true);

                // Set drag offset in case user moves finger to initiate drag
                // Drag offset is the difference between finger current X location and the location of the selected
                // button
                //
                // Used in touch move for calculating the location of the button. Think of it as a delta. If the user
                // moves their finger 50px, then the button should move 50px. Thus, this delta will be subtracted
                // from the user's X value to get the location of where the button position should be.
                dragOffsetX = ev.getX() - buttons.get(position).getLeft();

                // Return here so that the touch event is not sent to the buttons
                // This prevents the ripple effect from showing up when dragging
                return true;
            }

            case MotionEvent.ACTION_MOVE:
            {
                // Only drag if drag has been activated and hence is allowed
                if (Float.isNaN(dragOffsetX))
                    break;

                // Get X coordinate of where the selected button should be by taking user's X location and subtract
                // the offset
                float xCoord = ev.getX() - dragOffsetX;

                // Convert X coordinate to a position containing the relative offset within the button as well
                // Clip the position to be between 0.0f and buttons size minus 1 so that the user doesn't drag out of
                // bounds
                final float newPosition = Math.min(Math.max(getButtonPositionFromXF(xCoord), 0.0f), buttons.size() - 1);

                // Update the selected button to the new position
                moveSelectedButton(newPosition);
            }
            break;

            case MotionEvent.ACTION_CANCEL:
            {
                // Cancel action is called when user leaves the view with their finger and another view captures the
                // actions (e.g. scroll views for example)
                // In this case, stop dragging and "snap" to nearest position
                if (!Float.isNaN(dragOffsetX))
                {
                    setPosition(Math.round(currentPosition), true);

                    // Enable scroll touch event interception again now that we're done dragging
                    requestDisallowInterceptTouchEvent(false);
                }
            }
            break;
        }

        // Pretend like we never handled the touch event and pass to children (SegmentedButton)
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Move the selected button to a new position, used for animating and dragging the selected button
     *
     * The position parameter is a relative index with the whole part of the number indicating the button index and the
     * fractional part of the number indicating the relative position of the starting point of the selected button.
     *
     * For example, a position of 2.25 would indicate that the selected button should START, i.e. the left side of
     * the selected button, at 1/4 of the width of the 3rd button.
     */
    private void moveSelectedButton(final float position)
    {
        // Update current position to be the animated value
        // This is a float value indicating where the left-side of the button is located
        // For example, a currentPosition of 1.0 would mean all of button 1 was selected
        // But a currentPosition of 1.5 would mean half of button 1 was selected and half of button 2 (its in
        // between animating the value)
        currentPosition = position;

        // Get the current button position and extract the offset. For example, a currentPosition of 2.25 would
        // result in a currentButtonPosition of 2 and the currentOffset to 0.25.
        final int currentButtonPosition = (int)currentPosition;
        final float currentOffset = currentPosition - currentButtonPosition;

        // Get the current button end position, which will start at the current button plus 1 because the width of the
        // selected button is 1. Check each button to the right for the first one that is not GONE
        int currentEndButtonPosition = currentButtonPosition + 1;
        while (currentEndButtonPosition < buttons.size()
            && buttons.get(currentEndButtonPosition).getVisibility() == GONE)
        {
            ++currentEndButtonPosition;
        }

        // Grab the current button from the position and clip the right side of the button to show the appropriate
        // offset
        //
        // For example, if the currentOffset was 0.25, this means that the left-side of the selected button should
        // start at 0.25. Clipping at 0.25 means the right 75% of the button would be showing which is as expected. The
        // offset given to the current button is 0.0f -> 1.0f and represents the relative X position to clip the
        // button at (going all the way to the right side of the button, i.e. 0.25 all the way to 1.0)
        final SegmentedButton currentButton = buttons.get(currentButtonPosition);
        currentButton.clipRight(currentOffset);

        // For the end button, we want to clip the left side of the button to match up with the right side of the
        // previous button. However, there is a slight chance the end button position might be out of range so we
        // check if it is first (do nothing if out of range, nothing to clip on left side of)
        if (currentEndButtonPosition >= 0 && currentEndButtonPosition < buttons.size())
        {
            // Grab the button directly to the right of the current button and clip the left
            final SegmentedButton currentEndButton = buttons.get(currentEndButtonPosition);
            currentEndButton.clipLeft(currentOffset);
        }

        // lastPosition is the last button position (whole integer) that the selected button began at in the last
        // animation frame.
        // When the selected button moves from one button to the next, we need to hide the old button somehow.
        // This will do that by checking if the last button position is not equal to the current button position or
        // the end button position (where the selected button ends = currentButtonPosition + 1). If not equal, then
        // that means we are done showing the selected view on this button so we clip the entire view to just show
        // the normal button
        if (lastPosition != currentButtonPosition && lastPosition != currentEndButtonPosition)
        {
            buttons.get(lastPosition).clipRight(1.0f);
        }

        // Repeat same process above but check with where the last button position ended. Note, this last end position
        // is the next VISIBLE button, so we start at 1 plus the last position because the width of the selected button
        // is 1
        int lastEndPosition = lastPosition + 1;
        while (lastEndPosition < buttons.size() && buttons.get(lastEndPosition).getVisibility() == GONE)
        {
            ++lastEndPosition;
        }

        // Clip any views like explained above
        if (lastEndPosition != currentEndButtonPosition && lastEndPosition != currentButtonPosition
            && lastEndPosition < buttons.size())
            buttons.get(lastEndPosition).clipRight(1.0f);

        // Update the lastPosition for the next animation frame
        lastPosition = currentButtonPosition;

        // Notify to redraw buttons
        invalidate();
    }

    /**
     * Update the currently selected position
     *
     * This will "refresh" the buttons, so to speak, such that they accurately show the correct selected button.
     * During animation and dragging operations, there is the chance that intermediate buttons are showing parts of
     * the selected view. This shouldn't happen, but if it does this will refresh it to how it should look.
     *
     * In addition, the onPositionChangedListener will be called with the updated position.
     */
    private void updateSelectedPosition(final int position)
    {
        // Update position, current position and last position to the desired value
        this.position = position;
        this.currentPosition = position;
        this.lastPosition = position;

        // Loop through each button and reset it to the appropriate value
        for (int i = 0; i < buttons.size(); ++i)
        {
            final SegmentedButton button = buttons.get(i);

            if (i == position)
            {
                // Show entire selected view
                button.clipRight(0.0f);
            }
            else
            {
                // Hide entire selected view
                button.clipRight(1.0f);
            }
        }

        // Notify listener of position change
        if (onPositionChangedListener != null)
            onPositionChangedListener.onPositionChanged(position);
    }

    // endregion

    // region Save & Restore State

    @Override
    protected Parcelable onSaveInstanceState()
    {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());

        // Save position of selected button
        bundle.putInt("position", position);

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state)
    {
        // On off chance that the state is not a Bundle, just pass it to the parent class
        // Not sure why this would ever happen, but prevents a casting exception
        if (!(state instanceof Bundle))
        {
            super.onRestoreInstanceState(state);
            return;
        }

        final Bundle bundle = (Bundle)state;

        // Restore position of selected button
        final int position = bundle.getInt("position");
        setPosition(position, false);

        super.onRestoreInstanceState(bundle.getParcelable("superState"));
    }

    // endregion

    // region Getters & Setters

    /**
     * List of segmented buttons that are attached to this button group
     */
    public ArrayList<SegmentedButton> getButtons()
    {
        return buttons;
    }

    /**
     * Get segmented button at specified index
     *
     * Segmented buttons are indexed according to their order of being added to this group
     */
    public SegmentedButton getButton(int index)
    {
        return buttons.get(index);
    }

    /**
     * Returns the background drawable that is the 'global' value for each of the buttons. This is the background
     * that is shown when the button is not selected
     *
     * In the case a solid color background is used, this will be a ColorDrawable
     *
     * Note: This value is just passed down to each individual SegmentedButton when added to the view. This value can
     * be overridden on a per-button basis.
     *
     * @return the current background drawable when the button is not selected
     */
    public Drawable getBackground()
    {
        return backgroundDrawable;
    }

    /**
     * Set the background displayed when a button is not selected to a given drawable for each button
     *
     * Note: This is a convenience function that sets the background for each individual button.
     *
     * @param drawable drawable to set the background to
     */
    @Override
    public void setBackground(final Drawable drawable)
    {
        backgroundDrawable = drawable;

        // Check for non-null buttons because parent class calls setBackground
        if (buttons != null)
        {
            for (SegmentedButton button : buttons)
                button.setBackground(drawable);
        }
    }

    /**
     * Set the background to be a solid color when a button is not selected for each button
     *
     * This will create a ColorDrawable or modify the current background if it is a ColorDrawable.
     *
     * Note: This is a convenience function that sets the background for each individual button.
     *
     * @param color color to set the background to
     */
    public void setBackground(@ColorInt int color)
    {
        if (backgroundDrawable instanceof ColorDrawable)
        {
            ((ColorDrawable)backgroundDrawable.mutate()).setColor(color);

            // Required to update background for the buttons
            setBackground(backgroundDrawable);
        }
        else
        {
            setBackground(new ColorDrawable(color));
        }
    }

    /**
     * Convenience function for setting the background color
     *
     * This function already exists in the base View class so it is overridden to prevent confusion as to why
     * setBackground works but not setBackgroundColor.
     *
     * @param color color to set the background to
     * @see #setBackground(int)
     */
    @Override
    public void setBackgroundColor(@ColorInt int color)
    {
        setBackground(color);
    }

    /**
     * Returns the background drawable that is the 'global' value for each of the buttons. This is the background
     * that is shown when the button is selected
     *
     * In the case a solid color background is used, this will be a ColorDrawable
     *
     * Note: This value is just passed down to each individual SegmentedButton when added to the view. This value can
     * be overridden on a per-button basis.
     *
     * @return the current background drawable when the button is selected
     */
    public Drawable getSelectedBackground()
    {
        return selectedBackgroundDrawable;
    }

    /**
     * Set the background displayed when a button is selected to a given drawable for each button
     *
     * Note: This is a convenience function that sets the background for each individual button.
     *
     * @param drawable drawable to set the background to
     */
    public void setSelectedBackground(final Drawable drawable)
    {
        selectedBackgroundDrawable = drawable;

        for (SegmentedButton button : buttons)
            button.setSelectedBackground(drawable);
    }

    /**
     * Set the background to be a solid color when a button is selected for each button
     *
     * This will create a ColorDrawable or modify the current background if it is a ColorDrawable.
     *
     * Note: This is a convenience function that sets the background for each individual button.
     *
     * @param color color to set the background to
     */
    public void setSelectedBackground(@ColorInt int color)
    {
        if (selectedBackgroundDrawable instanceof ColorDrawable)
        {
            ((ColorDrawable)selectedBackgroundDrawable.mutate()).setColor(color);

            // Required to update background for the buttons
            setSelectedBackground(selectedBackgroundDrawable);
        }
        else
        {
            setSelectedBackground(new ColorDrawable(color));
        }
    }

    /**
     * Convenience function for setting the background color
     *
     * @param color color to set the background to
     * @see #setSelectedBackground(int)
     */
    public void setSelectedBackgroundColor(@ColorInt int color)
    {
        setSelectedBackground(color);
    }

    /**
     * Return the width of the border, in pixels
     *
     * 0px value indicates no border is present
     */
    public int getBorderWidth()
    {
        return borderWidth;
    }

    /**
     * Return color of the border
     */
    public int getBorderColor()
    {
        return borderColor;
    }

    /**
     * Return the border dash width, in pixels
     *
     * 0px value indicates the border is solid
     */
    public int getBorderDashWidth()
    {
        return borderDashWidth;
    }

    /**
     * Return the border gap width, in pixels
     *
     * Only relevant if border dash width is greater than 0px
     */
    public int getBorderDashGap()
    {
        return borderDashGap;
    }

    /**
     * Set the border for the perimeter of the button group.
     *
     * @param width     Width of the border in pixels (default value is 0px or no border)
     * @param color     Color of the border (default color is black)
     * @param dashWidth Width of the dash for border, in pixels. Value of 0px means solid line (default is 0px)
     * @param dashGap   Width of the gap for border, in pixels.
     */
    public void setBorder(int width, @ColorInt int color, int dashWidth, int dashGap)
    {
        borderWidth = width;
        borderColor = color;
        borderDashWidth = dashWidth;
        borderDashGap = dashGap;

        // Border width of 0 indicates to hide borders
        if (width > 0)
        {
            GradientDrawable borderDrawable = new GradientDrawable();
            // Set background color to be transparent so that buttons and everything underneath the border view is
            // still visible. This was an issue on API 16 Android where it would default to a black background
            borderDrawable.setColor(Color.TRANSPARENT);
            // Corner radius is the radius minus half of the border width because the drawable will draw the stroke
            // from the center, so the actual corner radius is reduced
            // If the half border width is left out, the border radius does not follow the curve of the background
            borderDrawable.setCornerRadius(radius - width / 2.0f);
            borderDrawable.setStroke(width, color, dashWidth, dashGap);

            borderView.setBackground(borderDrawable);
        }
        else
        {
            borderView.setBackground(null);
        }
    }

    /**
     * Return the width of the border for the selected button, in pixels
     *
     * 0px value indicates no border is present
     */
    public int getSelectedBorderWidth()
    {
        return selectedBorderWidth;
    }

    /**
     * Return color of the border for the selected button
     */
    public int getSelectedBorderColor()
    {
        return selectedBorderColor;
    }

    /**
     * Return the border dash width for the selected button, in pixels
     *
     * 0px value indicates the border is solid
     */
    public int getSelectedBorderDashWidth()
    {
        return selectedBorderDashWidth;
    }

    /**
     * Return the border gap width for the selected button, in pixels
     *
     * Only relevant if border dash width is greater than 0px
     */
    public int getSelectedBorderDashGap()
    {
        return selectedBorderDashGap;
    }

    /**
     * Set the border for the selected button.
     *
     * @param width     Width of the border in pixels (default value is 0px or no border)
     * @param color     Color of the border (default color is black)
     * @param dashWidth Width of the dash for border, in pixels. Value of 0px means solid line (default is 0px)
     * @param dashGap   Width of the gap for border, in pixels.
     */
    public void setSelectedBorder(int width, @ColorInt int color, int dashWidth, int dashGap)
    {
        selectedBorderWidth = width;
        selectedBorderColor = color;
        selectedBorderDashWidth = dashWidth;
        selectedBorderDashGap = dashGap;

        // Loop through each button and set the selected button border
        for (SegmentedButton button : buttons)
            button.setSelectedButtonBorder(width, color, dashWidth, dashGap);
    }

    /**
     * Returns the current corner radius for this button group, in pixels
     *
     * A value of 0px indicates the view is rectangular and has no rounded corners
     */
    public int getRadius()
    {
        return radius;
    }

    /**
     * Set the radius of this button group
     *
     * @param radius value of new corner radius, in pixels
     */
    public void setRadius(final int radius)
    {
        this.radius = radius;

        // Update radius for each button
        for (SegmentedButton button : buttons)
        {
            button.setBackgroundRadius(radius);
            button.setupBackgroundClipPath();

            button.invalidate();
        }

        // Update border for new radius
        GradientDrawable borderDrawable = (GradientDrawable)borderView.getBackground();
        if (borderDrawable != null)
            borderDrawable.setCornerRadius(radius - borderWidth / 2.0f);

        // Invalidate shadow outline so that it will be updated to follow the new radius
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
            invalidateOutline();
    }

    /**
     * Returns the current corner radius for the selected button, in pixels
     *
     * A value of 0px indicates the selected button will be rectangular and has no rounded corners
     */
    public int getSelectedButtonRadius()
    {
        return selectedButtonRadius;
    }

    /**
     * Sets the corner radius for the selected button
     *
     * @param selectedButtonRadius value of the new selected button corner radius, in pixels
     */
    public void setSelectedButtonRadius(int selectedButtonRadius)
    {
        this.selectedButtonRadius = selectedButtonRadius;

        // Update the selected button radius for each button
        for (SegmentedButton button : buttons)
        {
            button.setSelectedButtonRadius(selectedButtonRadius);
            button.setupSelectedButtonClipPath();
        }
    }

    /**
     * Return the currently selected button index
     *
     * If the button is currently being animated, then the position will be the old button position until the
     * animation is complete
     */
    public int getPosition()
    {
        return position;
    }

    /**
     * Selects the button at the given position and animates the movement if desired
     *
     * If the given position is out of bounds, i.e. less than 0 or greater than or equal to the number of buttons,
     * then the function will return after doing nothing. In addition, if the button selected is the current button
     * and the user is not dragging or the button is not currently being animated, then the function also returns
     * after doing nothing.
     *
     * If animate is true, then the button will be animated to the new position, using an animation interpolator and
     * animation duration stored in the button group. Otherwise, if animate is false, the button will be moved to the
     * new position immediately.
     *
     * If an existing animation is already taking place, the animation should automatically be cancelled and the new
     * animation will begin from the current location.
     *
     * @param position index of new button to select
     * @param animate  whether or not to animate moving to the button
     */
    public void setPosition(final int position, final boolean animate)
    {
        // Return and do nothing in two cases
        // First, if the position is out of bounds.
        // Second, if the desired position is equal to the current position do nothing. But, only do this under two
        // additional requirements. If the selected button is not being animated, since the position is not updated
        // until AFTER animation, it basically means the user cannot select the old button until the animation is done.
        // Also if the user is not dragging the button. If the user lets go from dragging and the button is still on
        // the same position but slightly offset, then we want to snap back to normal.
        if (position < 0 || position >= buttons.size() || (position == this.position && (buttonAnimator != null
            && !buttonAnimator.isRunning()) && Float.isNaN(dragOffsetX)))
            return;

        // If not animating or if the animation interpolator is null, then just update the selected position
        if (!animate || selectionAnimationInterpolator == null)
        {
            updateSelectedPosition(position);
            return;
        }

        // Loop through the buttons between the start and stop position to find any buttons with visibility equal to
        // GONE. Add to a list for later
        final List<Integer> buttonGoneIndices = new ArrayList<>();
        final boolean movingRight = currentPosition < position;
        if (movingRight)
        {
            for (int i = (int)Math.ceil(currentPosition); i < position; ++i)
            {
                if (buttons.get(i).getVisibility() == GONE)
                    buttonGoneIndices.add(i);
            }
        }
        else
        {
            for (int i = (int)Math.floor(currentPosition); i > position; --i)
            {
                if (buttons.get(i).getVisibility() == GONE)
                    buttonGoneIndices.add(i + 1);
            }
        }

        // Animate value from current position to the new position
        // Fraction positions such as 1.25 means we are 75% in button 1 and 25% in button 2.
        // The position indicates the position of the left side of the selected button
        //
        // The new position is adjusted to remove GONE buttons
        buttonAnimator = ValueAnimator.ofFloat(currentPosition,
            movingRight ? position - buttonGoneIndices.size() : position + buttonGoneIndices.size());

        // For each update to the animation value, move the button
        buttonAnimator.addUpdateListener(animation -> {
            float value = (float)animation.getAnimatedValue();

            // Account for GONE buttons in between the indices
            // Depending on if we're moving left/right, we add/subtract one when a button is missing
            // This will skip the GONE button
            for (int index : buttonGoneIndices)
            {
                if (movingRight && value >= index)
                    value += 1;
                else if (!movingRight && value <= index)
                    value -= 1;
            }

            // Move to the new position
            moveSelectedButton(value);
        });

        // Set the parameters for the button animation
        buttonAnimator.setDuration(selectionAnimationDuration);
        buttonAnimator.setInterpolator(selectionAnimationInterpolator);
        buttonAnimator.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(final Animator animation)
            {
                // Update the position of the button at the end of the animation
                // Also resets all buttons to their appropriate state in case the animation went wrong in any way
                updateSelectedPosition(position);

                // Note: Odd bug where this specific onAnimationEnd has to be overridden in order for the animation
                // end to be called. Originally the onAnimationEnd(animation, isReserve) operator was used but this
                // was not called on pre-lollipop devices.
            }
        });

        // Start the animation
        buttonAnimator.start();
    }

    /**
     * Returns whether or not the currently selected button can be moved via dragging
     */
    public boolean isDraggable()
    {
        return draggable;
    }

    /**
     * Sets whether or not the currently selected button can be moved via dragging
     *
     * If true, the user can drag their finger starting from the selected button to a different button and the
     * selected button will follow the users finger. When the user lets go, the selected button will snap to the
     * nearest button
     */
    public void setDraggable(final boolean draggable)
    {
        this.draggable = draggable;
    }

    /**
     * Whether or not the ripple effect is enabled on button tap
     *
     * If false, then no animation will be shown if the user taps a button. Otherwise, if true a ripple effect will
     * be shown on button tap.
     */
    public boolean hasRipple()
    {
        return ripple;
    }

    /**
     * Returns the ripple color used for displaying the ripple effect on button press
     *
     * The ripple color is a tint color applied on top of the button when it is pressed
     */
    public int getRippleColor()
    {
        return rippleColor;
    }

    /**
     * Set ripple effect to be either enabled or disabled on button press
     *
     * If enabled, then the ripple color used will be the last ripple color set for the buttons or the default value
     * of gray
     *
     * @param enabled whether or not to enable the ripple effect for all buttons in the group
     */
    public void setRipple(final boolean enabled)
    {
        ripple = enabled;

        // Loop through and set the ripple for each button
        for (SegmentedButton button : buttons)
            button.setRipple(enabled);
    }

    /**
     * Set ripple color used for ripple effect on button press
     *
     * This will automatically enable the ripple effect for all buttons if it is already disabled.
     *
     * @param color color to set for the ripple effect for all buttons in the group
     */
    public void setRipple(final @ColorInt int color)
    {
        ripple = true;
        rippleColor = color;

        // Loop through and set the ripple color for each button
        for (SegmentedButton button : buttons)
            button.setRipple(color);
    }

    /**
     * Returns divider drawable that is placed between each button in the group, value of null indicates no drawable
     */
    public Drawable getDivider()
    {
        return dividerLayout.getDividerDrawable();
    }

    /**
     * Set drawable as divider between buttons with a specified width, corner radius and padding
     *
     * If the drawable is null, then the divider will be removed and hidden
     *
     * @param drawable divider drawable that will be displayed between buttons
     * @param width    width of the divider drawable, in pixels
     * @param radius   corner radius of the divider drawable to round the corners, in pixels
     * @param padding  space above and below the divider drawable within the button group, in pixels
     */
    public void setDivider(@Nullable Drawable drawable, int width, int radius, int padding)
    {
        // Drawable of null indicates that we want to hide dividers
        if (drawable == null)
        {
            dividerLayout.setDividerDrawable(null);
            dividerLayout.setShowDividers(SHOW_DIVIDER_NONE);
            return;
        }

        // Set the corner radius and size if the drawable is a GradientDrawable
        // Otherwise just set the divider drawable like normal because we cant set the parameters
        if (drawable instanceof GradientDrawable)
        {
            GradientDrawable gradient = (GradientDrawable)drawable;
            gradient.setSize(width, 0);
            gradient.setCornerRadius(radius);

            dividerLayout.setDividerDrawable(gradient);
        }
        else
        {
            dividerLayout.setDividerDrawable(drawable);
        }

        dividerLayout.setDividerPadding(padding);
        dividerLayout.setShowDividers(SHOW_DIVIDER_MIDDLE);

        // Loop through and update the divider width for each of the dummy divider views
        for (int i = 0; i < dividerLayout.getChildCount(); ++i)
        {
            final ButtonActor view = (ButtonActor)dividerLayout.getChildAt(i);
            view.setDividerWidth(width);
        }
        dividerLayout.requestLayout();
    }

    /**
     * Set divider between buttons with a specified solid color, width, radius and padding
     *
     * @param color   color of the divider
     * @param width   width of the divider drawable, in pixels
     * @param radius  corner radius of the divider drawable to round the corners, in pixels
     * @param padding space above and below the divider drawable within the button group, in pixels
     */
    public void setDivider(@ColorInt int color, int width, int radius, int padding)
    {
        // Create GradientDrawable of the specified color
        // This is used to specify the corner radius, unlike ColorDrawable that does not have that feature
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
            new int[] {color, color});

        drawable.setCornerRadius(radius);
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setSize(width, 0);

        dividerLayout.setDividerDrawable(drawable);
        dividerLayout.setDividerPadding(padding);
        dividerLayout.setShowDividers(SHOW_DIVIDER_MIDDLE);

        // Loop through and update the divider width for each of the dummy divider views
        for (int i = 0; i < dividerLayout.getChildCount(); ++i)
        {
            final ButtonActor view = (ButtonActor)dividerLayout.getChildAt(i);
            view.setDividerWidth(width);
        }
        dividerLayout.requestLayout();
    }

    /**
     * Return the current animation interpolator used when animating button movement
     *
     * This will return null if no animation is being used
     */
    public Interpolator getSelectionAnimationInterpolator()
    {
        return selectionAnimationInterpolator;
    }

    /**
     * Set the current animation interpolator used when animating button movement
     *
     * If interpolator is null, no animation will be used
     */
    public void setSelectionAnimationInterpolator(@Nullable Interpolator interpolator)
    {
        selectionAnimationInterpolator = interpolator;
    }

    /**
     * Set the current animation interpolator used when animating button movement
     *
     * The interpolator given must be one of the predefined Android interpolators
     *
     * @param interpolator int value indicating which predefined Android interpolator to use
     */
    public void setSelectionAnimationInterpolator(@AnimationInterpolator int interpolator)
    {
        switch (interpolator)
        {
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

    /**
     * Return the duration, in milliseconds, it takes to complete the animation to change selected button
     */
    public int getSelectionAnimationDuration()
    {
        return selectionAnimationDuration;
    }

    /**
     * Set the duration for animating changing the selected button
     *
     * @param selectionAnimationDuration duration in milliseconds for animation to complete
     */
    public void setSelectionAnimationDuration(final int selectionAnimationDuration)
    {
        this.selectionAnimationDuration = selectionAnimationDuration;
    }

    /**
     * Returns the listener used for notifying position changes
     */
    public OnPositionChangedListener getOnPositionChangedListener()
    {
        return onPositionChangedListener;
    }

    /**
     * Sets the listeners used for notifying position changes
     */
    public void setOnPositionChangedListener(final OnPositionChangedListener onPositionChangedListener)
    {
        this.onPositionChangedListener = onPositionChangedListener;
    }

    // endregion

    // region Listeners

    /**
     * Interface definition for a callback that will be invoked when the position of the selection button changes
     *
     * This callback will be called AFTER the animation is complete since the position does not change until the
     * completion of the animation.
     */
    public interface OnPositionChangedListener
    {
        void onPositionChanged(int position);
    }

    // endregion

    // region Classes

    /**
     * Outline that creates a rounded rectangle with the radius set to the specified corner radius from layout
     *
     * Primary benefit of this class is that shadows will follow contours of the outline rather than the rectangular
     * bounds. Since shadows are only available from Lollipop and beyond (21+), outlines are only available from the
     * same API.
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private class OutlineProvider extends ViewOutlineProvider
    {
        @Override
        public void getOutline(final View view, final Outline outline)
        {
            outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
        }
    }

    // endregion
}
