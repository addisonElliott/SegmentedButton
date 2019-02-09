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
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.interpolator.view.animation.FastOutLinearInInterpolator;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class SegmentedButtonGroup extends LinearLayout {

    // region Variables & Constants
    private static final String TAG = "SegmentedButtonGroup";

    // Animation interpolator styles for animating button movement
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

    // TODO Is this a vlaid number?
    private final static float DRAG_ANCHOR_NONE = Float.NaN;

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
    // 1. Button linear layout that contains the SegmentedButtons
    // 2. Divider linear layout that contains the dividers between buttons
    // 3. Border view that has the border for the group that is drawn over everything else
    private LinearLayout buttonLayout;

    // Purpose of divider LinearLayout is to ensure the button dividers are placed in between the buttons and that no
    // extra space is allocated for the divider. If dividers were placed on the buttonLayout, then space would be
    // allocated for the dividers and the biggest problem is that the background ends up being gray if there is
    // divider padding set.
    private LinearLayout dividerLayout;

    // View for placing border on top of the buttons
    // Background view for placing border on top of the buttons, background is transparent to see everything but border
    private BackgroundView borderView;

    // Array containing the buttons
    private ArrayList<SegmentedButton> buttons;

    // Whether or not the button can be dragged to a different position (default value is false)
    private boolean draggable;
    // When a user touches down on the currently selected button, this is set to be the difference between their
    // current X location of tapping and the currently selected button's left X coordinate
    // When user drags their finger across the button group, this value will be used to offset the current X location
    // with how much the button should have moved
    //
    // This value will be NaN when dragging is disabled
    private float dragOffsetX;

    // TODO Do I need this
    private ArrayList<BackgroundView> ripples = new ArrayList<>();

    // Position of the currently selected button, zero-indexed (default value is 0)
    // When animating, the position will be the previous value until after animation is finished
    private int position;

    // Radius for rounding edges of the button group, in pixels (default value is 0)
    private int radius;

    // Drawable for the border (default value is null)
    private GradientDrawable borderDrawable;
    private int borderWidth;
    private int borderColor;
    private int borderDashWidth, borderDashGap;

    private Drawable backgroundDrawable, selectedBackgroundDrawable;

    // Animation interpolator for animating button movement
    // Android has some standard interpolator, e.g. BounceInterpolator, but also easy to create own interpolator
    private Interpolator selectionAnimationInterpolator;

    // TODO Explain these
    ValueAnimator buttonAnimator;
    private int lastPosition, lastEndPosition;
    // Note this is different from the position field because that is set to basically what it WANTS to be
    // This is the actual position of the selector
    private float currentPosition;

    private int selectionAnimationDuration;

    private Drawable dividerBackgroundDrawable;
    private int dividerSize;
    private int dividerPadding;
    private int dividerRadius;

    // TODO UNUSED VARIABLES, FIX UP LATER
    private int selectorColor, animateSelector, animateSelectorDuration, dividerColor, rippleColor;
    private boolean clickable, enabled, ripple, hasRippleColor, hasDivider;

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
        // Retrieve custom attributes
        getAttributes(context, attrs);

        // TODO Analyze these parts below to see what is necessary
        // Create and set outline provider for the segmented button group
        // This is used to provide an outline for the layout because it may have rounded corners
        // The primary benefit to using this is that shadows will follow the contour of the outline rather than the
        // rectangular bounds
        if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new OutlineProvider());
        }

        // Initial setup
        // TODO Describe me
        currentPosition = position;
        lastPosition = position;
        lastEndPosition = position;

        buttons = new ArrayList<>();

        // TODO Explain why we need this and why the parent class has to be a LinearLayout
        FrameLayout container = new FrameLayout(getContext());
        container.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(container);

        buttonLayout = new LinearLayout(getContext());
        buttonLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        container.addView(buttonLayout);

        // Create border view
        // This is essentially a dummy view that is drawn on top of the buttonLayout (contains the buttons) so that the
        // border appears on top of them
        borderView = new BackgroundView(context);
        borderView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(borderView);

        // Only create border drawable if border is present
        if (borderWidth > 0) {
            borderDrawable = new GradientDrawable();
            borderDrawable.setCornerRadius(radius - borderWidth / 2.0f);
            borderDrawable.setStroke(borderWidth, borderColor, borderDashWidth, borderDashGap);

            borderView.setBackground(borderDrawable);
        }

        // TODO Struggling to see the purpose of this container
        // Oh, it probably is so that the divider appears over the ripple container
        dividerLayout = new LinearLayout(getContext());
        dividerLayout.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        dividerLayout.setOrientation(LinearLayout.HORIZONTAL);
        dividerLayout.setClickable(false);
        dividerLayout.setFocusable(false);
        container.addView(dividerLayout);

        // TODO Need to find way to have dividers shown on top of the buttons
        dividerLayout.setDividerPadding(25);
//        dividerLayout.setBackgroundColor(Color.YELLOW);

        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, new int[]{Color.RED,
                Color.RED});
        drawable.setCornerRadius(2);
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(Color.RED);
        drawable.setSize(10, 75);
        dividerLayout.setDividerDrawable(drawable);
        dividerLayout.setShowDividers(SHOW_DIVIDER_MIDDLE);

//        initInterpolations();
//        setDividerAttrs();
    }

    private void getAttributes(Context context, @Nullable AttributeSet attrs) {
        // According to docs for obtainStyledAttributes, attrs can be null and I assume that each value will be set
        // to the default
        TypedArray ta = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.SegmentedButtonGroup, 0, 0);

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

        // Setup border for button group
        // Width is the thickness of the border, color is the color of the border
        // Dash width and gap, if the dash width is not zero will make the border dashed with a ratio between dash
        // width and gap
        borderWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_borderWidth, 0);
        borderColor = ta.getColor(R.styleable.SegmentedButtonGroup_borderColor, Color.BLACK);
        borderDashWidth = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_borderDashWidth, 0);
        borderDashGap = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_borderDashGap, 0);

        radius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_radius, 0);
        position = ta.getInt(R.styleable.SegmentedButtonGroup_position, 0);

        hasRippleColor = ta.hasValue(R.styleable.SegmentedButtonGroup_rippleColor);
        ripple = ta.getBoolean(R.styleable.SegmentedButtonGroup_ripple, false);
        rippleColor = ta.getColor(R.styleable.SegmentedButtonGroup_rippleColor, Color.GRAY);

//        final TypedValue value = new TypedValue();
//        if (ta.getValue(R.styleable.SegmentedButtonGroup_divider, value)) {
//            if (value.type == TypedValue.TYPE_REFERENCE || value.type == TypedValue.TYPE_STRING) {
//                // Resource =                value.resourceId
//            } else if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
//                // Color = value.data
//            } else {
////                throw new Exception("whaaa TODO fix me");
//            }
//        }

//        hasDivider = ta.hasValue(R.styleable.SegmentedButtonGroup_dividerSize);
//        dividerBackgroundDrawable = ta
//                .getDrawable(R.styleable.SegmentedButtonGroup_dividerBackgroundDrawable);
//        dividerSize = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerSize, 0);
//        dividerColor = ta.getColor(R.styleable.SegmentedButtonGroup_dividerColor, Color.WHITE);
//        dividerPadding = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerPadding, 0);
//        dividerRadius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerRadius, 0);

        int selectionAnimationInterpolator = ta.getInt(R.styleable.SegmentedButtonGroup_selectionAnimationInterpolator,
                ANIM_INTERPOLATOR_FAST_OUT_SLOW_IN);
        setSelectionAnimationInterpolator(selectionAnimationInterpolator);
        selectionAnimationDuration = ta.getInt(R.styleable.SegmentedButtonGroup_selectionAnimationDuration, 500);

        enabled = ta.getBoolean(R.styleable.SegmentedButtonGroup_enabled, true);
        draggable = ta.getBoolean(R.styleable.SegmentedButtonGroup_draggable, false);

        // TODO Why is clickable needed and why is it in a try/catch?
        try {
            clickable = ta.getBoolean(R.styleable.SegmentedButtonGroup_android_clickable, true);
        } catch (Exception ex) {
            Log.d(TAG, ex.toString());
        }

        // Recycle the typed array, required once done using it
        ta.recycle();
    }

    // endregion

    // region Layout & Measure

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
//        LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams)params;
//        Log.v(TAG, "View added, params: " + Float.toString(params2.weight));

        if (child instanceof SegmentedButton) {
            SegmentedButton button = (SegmentedButton) child;
            final int position = buttons.size();

            // Give radius, default background and default selected background to the button
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

                // Update the background clip path for that button
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

//            // RIPPLE
//            BackgroundView rippleView = new BackgroundView(getContext());
//            if (!draggable) {
//                rippleView.setOnClickListener(new OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        if (clickable && enabled) {
//                            toggle(position, animateSelectorDuration, true);
//                        }
//                    }
//                });
//            }
//
//            setRipple(rippleView, enabled && clickable);
//            rippleContainer.addView(rippleView,
//                    new LinearLayout.LayoutParams(button.getButtonWidth(), ViewGroup.LayoutParams.MATCH_PARENT,
//                            button.getWeight()));
//            ripples.add(rippleView);
//
//            if (!hasDivider) {
//                return;
//            }
//
            BackgroundView dividerView = new BackgroundView(getContext());
//            dividerLayout.addView(dividerView, new LinearLayout.LayoutParams(button.getButtonWidth(),
//                    ViewGroup.LayoutParams.MATCH_PARENT, button.getWeight()));
            dividerLayout.addView(dividerView, params);

            // On update setLayoutParams
            // On update weightsum, well that won't happen, well sure it could I guess

        } else {
            // TODO Throw exception here, safe to require SegmentedButton
            super.addView(child, index, params);
        }
    }

    // endregion

    // region Events

    int getButtonPositionFromX(float x) {
        // TODO Comment me
        for (int i = 0; i < buttons.size(); ++i) {
            final SegmentedButton button = buttons.get(i);

            if (x <= button.getRight()) {
                return i;
            }
        }

        // TODO Throw an exception here maybe
        return -1;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        int position = getButtonPositionFromX(ev.getX());

        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                setPosition(position, true);
                break;

            case MotionEvent.ACTION_DOWN:
                if (this.position != position) {
                    dragOffsetX = -1.0f;
                    break;
                }

                dragOffsetX = ev.getX() - buttons.get(position).getLeft();
                Log.v(TAG, String.format("down: %f = %f", ev.getX(), dragOffsetX));
                break;

            case MotionEvent.ACTION_MOVE:
                // TODO Make this apart of the SegmentedButtonGroup class but need to save position variable
                // TODO Move invalidate into the moveSelectedButton function
                // Only can drag when starting on the selected button
                if (dragOffsetX == -1.0f) {
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
                invalidate();
                break;
        }

        return super.dispatchTouchEvent(ev);
    }

    // TODO Better name?
    public void setPosition(final int position, boolean animate) {
        // TODO Do something special here to update this.position
//        // Cancel current animation to start another one
//        if (buttonAnimator.isRunning()) {
//            buttonAnimator.cancel();
//        }

        // Do nothing and return if our current position is already the position given
        if (position == this.position) {
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

        if (lastEndPosition != currentEndButtonPosition && lastEndPosition != currentButtonPosition
                && lastEndPosition < buttons.size()) {
            buttons.get(lastEndPosition).clipRight(1.0f);
        }

        lastPosition = currentButtonPosition;
        lastEndPosition = currentEndButtonPosition;
    }

    private void updatePositions(final int position) {
        this.position = position;
        this.currentPosition = position;
        this.lastPosition = position;
        this.lastEndPosition = position + 1;

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
