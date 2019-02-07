package com.addisonelliott.segmentedbutton;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
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
import android.view.ViewGroup.LayoutParams;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;

public class SegmentedButtonGroup extends LinearLayout {

    // region Variables & Constants
    private static final String TAG = "SegmentedButtonGroup";

    public final static int FastOutSlowInInterpolator = 0;
    public final static int BounceInterpolator = 1;
    public final static int LinearInterpolator = 2;
    public final static int DecelerateInterpolator = 3;
    public final static int CycleInterpolator = 4;
    public final static int AnticipateInterpolator = 5;
    public final static int AccelerateDecelerateInterpolator = 6;
    public final static int AccelerateInterpolator = 7;
    public final static int AnticipateOvershootInterpolator = 8;
    public final static int FastOutLinearInInterpolator = 9;
    public final static int LinearOutSlowInInterpolator = 10;
    public final static int OvershootInterpolator = 11;

    private LinearLayout mainGroup, rippleContainer, dividerContainer;

    // View for placing border on top of the buttons
    private BackgroundView borderView;
    private GradientDrawable borderDrawable;

    // General purpose rectangle to prevent allocation in onDraw
    private RectF rectF;

    private boolean draggable = false;
    private int numberOfButtons = 0;
    private ArrayList<SegmentedButton> buttons;
    private ArrayList<BackgroundView> ripples = new ArrayList<>();

    // Custom attributes
    private int selectorColor, animateSelector, animateSelectorDuration, position, dividerColor,
            radius, dividerSize, rippleColor, dividerPadding, dividerRadius, borderWidth, borderColor,
            borderDashWidth, borderDashGap;
    private boolean clickable, enabled, ripple, hasRippleColor, hasDivider;
    private Drawable backgroundDrawable, selectedBackgroundDrawable, dividerBackgroundDrawable;

    private Interpolator interpolatorSelector;

    private int toggledPosition = 0;
    private float toggledPositionOffset = 0;
    private int lastPosition = 0;
    private float lastPositionOffset = 0;

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

        // TODO Comment me
        setClickable(true);

        buttons = new ArrayList<>();

        FrameLayout container = new FrameLayout(getContext());
        container.setLayoutParams(
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(container);

        mainGroup = new LinearLayout(getContext());
        mainGroup.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mainGroup.setOrientation(LinearLayout.HORIZONTAL);
        container.addView(mainGroup);

        // Create border view
        // This is essentially a dummy view that is drawn on top of the mainGroup (contains the buttons) so that the
        // border appears on top of them
        borderView = new BackgroundView(context);
        borderView.setLayoutParams(
                new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        container.addView(borderView);

        // Only create border drawable if border is present
        if (borderWidth > 0) {
            borderDrawable = new GradientDrawable();
            borderDrawable.setCornerRadius(radius - borderWidth / 2.0f);
            borderDrawable.setStroke(borderWidth, borderColor, borderDashWidth, borderDashGap);

            borderView.setBackground(borderDrawable);
        }

//        rippleContainer = new LinearLayout(getContext());
//        rippleContainer
//                .setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//        rippleContainer.setOrientation(LinearLayout.HORIZONTAL);
//        rippleContainer.setClickable(false);
//        rippleContainer.setFocusable(false);
//        rippleContainer.setPadding(borderSize, borderSize, borderSize, borderSize);
//        container.addView(rippleContainer);

        // TODO Struggling to see the purpose of this container
        // Oh, it probably is so that the divider appears over the ripple container
//        dividerContainer = new LinearLayout(getContext());
//        dividerContainer
//                .setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//        dividerContainer.setOrientation(LinearLayout.HORIZONTAL);
//        dividerContainer.setClickable(false);
//        dividerContainer.setFocusable(false);
//        container.addView(dividerContainer);

//        initInterpolations();
//        setDividerAttrs();

        // General purpose float rectangle, used in layout or draw calls to prevent allocation of new objects
        rectF = new RectF();
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

        hasDivider = ta.hasValue(R.styleable.SegmentedButtonGroup_dividerSize);
        dividerBackgroundDrawable = ta
                .getDrawable(R.styleable.SegmentedButtonGroup_dividerBackgroundDrawable);
        dividerSize = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerSize, 0);
        dividerColor = ta.getColor(R.styleable.SegmentedButtonGroup_dividerColor, Color.WHITE);
        dividerPadding = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerPadding, 0);
        dividerRadius = ta.getDimensionPixelSize(R.styleable.SegmentedButtonGroup_dividerRadius, 0);

        animateSelector = ta.getInt(R.styleable.SegmentedButtonGroup_animateSelector, 0);
        animateSelectorDuration = ta.getInt(R.styleable.SegmentedButtonGroup_animateSelectorDuration, 500);

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

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {

        if (child instanceof SegmentedButton) {
            SegmentedButton button = (SegmentedButton) child;
            final int position = numberOfButtons++;

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
            mainGroup.addView(button, params);
            buttons.add(button);

            // If the given position to start at is this button, select it
            if (this.position == position) {
                // Disabled clipping for button to display entire selected view
                button.clipRight(0.0f);
            }

            // TODO Ensure that padding is set to 0 for this class, otherwise it messes stuff up

//            if (this.position == position) {
//                button.clipToRight(1);
//
//                lastPosition = toggledPosition = position;
//                lastPositionOffset = toggledPositionOffset = (float) position;
//            }
//
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
//            BackgroundView dividerView = new BackgroundView(getContext());
//            dividerContainer.addView(dividerView,
//                    new LinearLayout.LayoutParams(button.getButtonWidth(), ViewGroup.LayoutParams.MATCH_PARENT,
//                            button.getWeight()));
        } else {
            super.addView(child, index, params);
        }
    }

    // endregion

    // region Events

//    @Override
//    public boolean onInterceptTouchEvent(final MotionEvent ev) {
////        return super.onInterceptTouchEvent(ev);
//        return true;
//    }

    int getPositionFromX(float x) {
        for (int i = 0; i < buttons.size(); ++i) {
            final SegmentedButton button = buttons.get(i);

            if (x <= button.getRight()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float selectorWidth, offsetX, offsetX2;
        int position = 0;
        boolean isMovingLeft;
        float stop = 0.0f;

        // Get largest index where event.getX() < x[k]
        // That is your position = k
        // Get width = x[k] - x[k-1], if k = 0, width = x[k]
        // Relative clip = getX() - (x[k - 1]||0) / width
        //
        // Well, maybe I'll do it another way...
//        float[] x = { 100, 200, 400, 600, 700};
        final float x = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                // Position selected via touch
                position = getPositionFromX(event.getX());

                // Do nothing if we are already there
                if (this.position == position) {
                    return true;
                }

                // Whether or not we are going to move left or right
                isMovingLeft = (this.position > position);

                final int ttposition = position;

                ValueAnimator animator = ValueAnimator.ofFloat(this.position, position);
                animator.addUpdateListener(new AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(final ValueAnimator animation) {
                        final float value = (float) animator.getAnimatedValue();
                        final float endValue = value + (isMovingLeft ? 1.0f : 1.0f);

                        final int currentPosition = (int) value;
                        final float currentOffset = value - currentPosition;

                        final int currentEndPosition = (int) endValue;
                        final float currentEndOffset = endValue - currentEndPosition;

                        final SegmentedButton currentButton = buttons.get(currentPosition);
                        final SegmentedButton currentEndButton =
                                (currentEndPosition >= 0 && currentEndPosition < buttons.size()) ?
                                        buttons.get(currentEndPosition) : null;

                        if (isMovingLeft) {
                            if (currentEndButton != null) {
                                currentEndButton.clipLeft(currentEndOffset);
                            }

                            currentButton.clipRight(currentOffset);

                            if (currentEndPosition < buttons.size() - 1) {
                                buttons.get(currentEndPosition + 1).clipLeft(0.0f);
                            }
                        } else {
                            if (currentPosition > 0) {
                                buttons.get(currentPosition - 1).clipRight(1.0f);
                            }

                            currentButton.clipRight(currentOffset);

                            if (currentEndButton != null) {
                                currentEndButton.clipLeft(currentEndOffset);
                            }
                        }
                    }
                });
                animator.setDuration(500);
                animator.setInterpolator(new BounceInterpolator());
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(final Animator animation, final boolean isReverse) {
                        // TODO Here is where I can handle stuff?
                        SegmentedButtonGroup.this.position = ttposition;
                    }
                });
                animator.start();

//                buttons.get(this.position).getLeft()
//                buttons.get(position).getLeft()

                // Get selected button and the desired center of it

//                for (int i = 0; i < buttons.size(); ++i) {
//                    final SegmentedButton button = buttons.get(i);
//
//                    if (x >= button.getLeft() && x <= button.getRight()) {
//                        position = i;
//                        stop = button.getLeft();
//                        // button.getRight(); depending on which way we're moving
//                        break;
//                    }
//                }
//
//                ValueAnimator animator = ValueAnimator.ofFloat(0.0f, stop);
//                animator.addUpdateListener(new AnimatorUpdateListener() {
//                    @Override
//                    public void onAnimationUpdate(final ValueAnimator animation) {
//                        final float value = (float)animation.getAnimatedValue();
//                        SegmentedButton button;
//
//                        for (int i = 0; i < buttons.size(); ++i) {
//                            button = buttons.get(i);
////
////                            if (value >= button.getLeft() && x <= button.getRight()) {
////                                break;
////                            }
//                            button.updateClip(value);
//                        }
//                        // Get button from value
//                        //
//                    }
//                });
//
//                animator.setDuration(5000);
////                animator.setInterpolator(new BounceInterpolator());
////                animator.setInterpolator(TimeInterpolator.);
//                animator.start();
                //        animator.setInterpolator(interpolatorSelector);
//        animator.setDuration(duration);
//        animator.start();
                // setPosition(position, animated: true);
                // Get nearest position, loop through buttons, find one that is between getLeft() + getWidth()
                // Position = k
                // Get current position, if same position, do nothing
                // Otherwise, animate to that value
//                offsetX2 = (float) ((getX() / (float) getWidth()) * numberOfButtons - 0.5);

//                selectorWidth = (float) getWidth() / numberOfButtons / 2f;
//                offsetX = ((event.getX() - selectorWidth) * numberOfButtons) / getWidth();
//                position = (int) Math.floor(offsetX + 0.5);
//
//                Log.v(TAG, String.format("X: %f, Y: %f, offsetX: %f, selectorWidth: %f, position: %d, width: %d, %f",
//                        event.getX(), event.getY(), offsetX, selectorWidth, position, getWidth(), offsetX2));
                break;

            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_MOVE:
                break;
        }

        return true;

//        float selectorWidth, offsetX;
//        int position = 0;
//
//        switch (event.getAction()) {
//            case MotionEvent.ACTION_UP:
        // offsetX = (getX / width() * numberofButtons - 0.5
        // -0.5 -> 2.5 for 3 buttons
//                selectorWidth = (float) getWidth() / numberOfButtons / 2f;
//                offsetX = ((event.getX() - selectorWidth) * numberOfButtons) / getWidth();
//                position = (int) Math.floor(offsetX + 0.5);
//
//                toggledPositionOffset = lastPositionOffset = offsetX;
//
//                toggle(position, animateSelectorDuration, true);
//
//                break;
//            case MotionEvent.ACTION_DOWN:
//                break;
//            case MotionEvent.ACTION_MOVE:
//
//                if (!draggable) {
//                    break;
//                }
//
//                selectorWidth = (float) getWidth() / numberOfButtons / 2f;
//
//                offsetX = ((event.getX() - selectorWidth) * numberOfButtons) / (float) getWidth();
//                position = (int) Math.floor(offsetX);
//                offsetX -= position;
//
//                if (event.getRawX() - selectorWidth < getLeft()) {
//                    offsetX = 0;
//                    animateViews(position + 1, offsetX);
//                    break;
//                }
//                if (event.getRawX() + selectorWidth > getRight()) {
//                    offsetX = 1;
//                    animateViews(position - 1, offsetX);
//                    break;
//                }
//
//                animateViews(position, offsetX);
//
//                break;
//        }
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
//        dividerContainer.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
//        // Divider Views
//        RoundHelper.makeDividerRound(dividerContainer, dividerColor, dividerRadius, dividerSize,
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
//
//    private void initInterpolations() {
//        ArrayList<Class> interpolatorList = new ArrayList<Class>() {{
//            add(FastOutSlowInInterpolator.class);
//            add(BounceInterpolator.class);
//            add(LinearInterpolator.class);
//            add(DecelerateInterpolator.class);
//            add(CycleInterpolator.class);
//            add(AnticipateInterpolator.class);
//            add(AccelerateDecelerateInterpolator.class);
//            add(AccelerateInterpolator.class);
//            add(AnticipateOvershootInterpolator.class);
//            add(FastOutLinearInInterpolator.class);
//            add(LinearOutSlowInInterpolator.class);
//            add(OvershootInterpolator.class);
//        }};
//
//        try {
//            interpolatorSelector = (Interpolator) interpolatorList.get(animateSelector).newInstance();
//        } catch (Exception e) {
//            e.printStackTrace();
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
//        RoundHelper.makeDividerRound(dividerContainer, dividerColor, dividerRadius, dividerSize,
//                dividerBackgroundDrawable);
//    }
//
//    /**
//     * @param dividerSize sets thickness of divider
//     *                    default: 0
//     */
//    public void setDividerSize(int dividerSize) {
//        this.dividerSize = dividerSize;
//        RoundHelper.makeDividerRound(dividerContainer, dividerColor, dividerRadius, dividerSize,
//                dividerBackgroundDrawable);
//    }
//
//    /**
//     * @param dividerRadius determines how round divider should be
//     *                      default: 0
//     */
//    public void setDividerRadius(int dividerRadius) {
//        this.dividerRadius = dividerRadius;
//        RoundHelper.makeDividerRound(dividerContainer, dividerColor, dividerRadius, dividerSize,
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
//
//    private void toggle(int position, int duration, boolean isToggledByTouch) {
//        if (!draggable && toggledPosition == position) {
//            return;
//        }
//
//        toggledPosition = position;
//
//        ValueAnimator animator = ValueAnimator.ofFloat(toggledPositionOffset, position);
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                float animatedValue = toggledPositionOffset = (float) animation.getAnimatedValue();
//
//                int position = (int) animatedValue;
//                float positionOffset = animatedValue - position;
//
//                animateViews(position, positionOffset);
//
//                invalidate();
//            }
//        });
//        animator.setInterpolator(interpolatorSelector);
//        animator.setDuration(duration);
//        animator.start();
//
//        if (null != onClickedButtonListener && isToggledByTouch) {
//            onClickedButtonListener.onClickedButton(position);
//        }
//
//        if (null != onPositionChangedListener) {
//            onPositionChangedListener.onPositionChanged(position);
//        }
//
//        this.position = position;
//    }
//
//    private void animateViews(int position, float positionOffset) {
//        float realPosition = position + positionOffset;
//        float lastRealPosition = lastPosition + lastPositionOffset;
//
//        if (realPosition == lastRealPosition) {
//            return;
//        }
//
//        int nextPosition = position + 1;
//        if (positionOffset == 0.0f) {
//            if (lastRealPosition <= realPosition) {
//                nextPosition = position - 1;
//            }
//        }
//
//        if (lastPosition > position) {
//            if (lastPositionOffset > 0f) {
//                toNextPosition(nextPosition + 1, 1);
//            }
//        }
//
//        if (lastPosition < position) {
//            if (lastPositionOffset < 1.0f) {
//                toPosition(position - 1, 0);
//            }
//        }
//
//        toNextPosition(nextPosition, 1.0f - positionOffset);
//        toPosition(position, 1.0f - positionOffset);
//
//        lastPosition = position;
//        lastPositionOffset = positionOffset;
//    }
//
//    private void toPosition(int position, float clip) {
//        if (position >= 0 && position < numberOfButtons) {
//            buttons.get(position).clipToRight(clip);
//        }
//    }
//
//    private void toNextPosition(int position, float clip) {
//        if (position >= 0 && position < numberOfButtons) {
//            buttons.get(position).clipToLeft(clip);
//        }
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
