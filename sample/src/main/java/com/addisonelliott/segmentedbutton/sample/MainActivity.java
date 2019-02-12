package com.addisonelliott.segmentedbutton.sample;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import java.util.ArrayList;

enum Action {
    None,
    ChangeBorder1,
    ChangeBorder2,
    ChangeBackgroundColor,
    ChangeBackgroundDrawable,
    ChangeRadius,
    ChangePositionAnimated,
    ChangePosition,
    ToggleDraggable;

    public String getDisplayText() {
        if (this == None) {
            return "";
        } else if (this == ChangeBorder1) {
            return "Change border 1";
        } else if (this == ChangeBorder2) {
            return "Change border 2";
        } else if (this == ChangeBackgroundColor) {
            return "Change background and selected background to color";
        } else if (this == ChangeBackgroundDrawable) {
            return "Change background and selected background to drawable";
        } else if (this == ChangeRadius) {
            return "Change radius";
        } else if (this == ChangePositionAnimated) {
            return "Change position and animate movement";
        } else if (this == ChangePosition) {
            return "Change position without animating";
        } else if (this == ToggleDraggable) {
            return "Toggle draggable boolean";
        } else {
            return "";
        }
    }
}

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {

    private static final String TAG = "SegmentedButtonSample";

    @BindView(R.id.spinner)
    Spinner spinner;
    @BindView(R.id.button_changePosition)
    Button changePositionButton;
    @BindView(R.id.buttonGroup_gradient)
    SegmentedButtonGroup gradientButtonGroup;
    @BindView(R.id.buttonGroup_lordOfTheRings)
    SegmentedButtonGroup lordOfTheRingsButtonGroup;
    @BindView(R.id.buttonGroup_DCSuperheros)
    SegmentedButtonGroup DCSuperHerosButtonGroup;
    @BindView(R.id.buttonGroup_marvelSuperheros)
    SegmentedButtonGroup marvelSuperherosButtonGroup;
    @BindView(R.id.buttonGroup_guys)
    SegmentedButtonGroup guysButtonGroup;
    @BindView(R.id.buttonGroup_starWars)
    SegmentedButtonGroup starWarsButtonGroup;
    @BindView(R.id.buttonGroup_darthVader)
    SegmentedButtonGroup darthVaderButtonGroup;
    @BindView(R.id.buttonGroup_draggable)
    SegmentedButtonGroup draggableButtonGroup;
    @BindView(R.id.buttonGroup_dynamic)
    SegmentedButtonGroup dynamicButtonGroup;
    @BindView(R.id.buttonGroup_pickupDropoffBoth)
    SegmentedButtonGroup pickupDropoffButtonGroup;
    @BindView(R.id.buttonGroup_starWarsAlt)
    SegmentedButtonGroup starWarsAltButtonGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize all variables annotated with @BindView and other variants
        ButterKnife.bind(this);

        ArrayList<String> spinnerItems = new ArrayList<>();

        for (Action action : Action.values()) {
            spinnerItems.add(action.getDisplayText());
        }

        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        updateButton(gradientButtonGroup.getPosition());
        gradientButtonGroup.setOnPositionChangedListener(position -> updateButton(position));

        // TODO Get this working
        setupDynamicDrawables();

        // Basic checks
        if (starWarsButtonGroup.getButtons().size() != 3) {
            throw new AssertionError("Buttons size incorrect");
        }

        if (!lordOfTheRingsButtonGroup.getButton(1).getText().equals("Gimli")) {
            throw new AssertionError("Button name is incorrect");
        }
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
        Action action = Action.values()[position];

        switch (action) {
            case None:
                break;

            case ChangeBorder1:
                lordOfTheRingsButtonGroup.setBorder(5, Color.RED, 25, 8);
                marvelSuperherosButtonGroup.setBorder(5, Color.BLACK, 30, 10);
                break;

            case ChangeBorder2:
                lordOfTheRingsButtonGroup.setBorder(2, Color.RED, 25, 8);
                marvelSuperherosButtonGroup.setBorder(2, Color.BLACK, 30, 10);
                break;

            case ChangeBackgroundColor:
                lordOfTheRingsButtonGroup.setBackground(ContextCompat.getColor(getApplicationContext(),
                        R.color.brown_600));
                lordOfTheRingsButtonGroup.setSelectedBackground(ContextCompat.getColor(getApplicationContext(),
                        R.color.blue_800));
                break;

            case ChangeBackgroundDrawable:
                lordOfTheRingsButtonGroup.setBackground(ContextCompat.getDrawable(getApplicationContext(),
                        R.drawable.gradient_drawable));
                lordOfTheRingsButtonGroup.setSelectedBackground(ContextCompat.getDrawable(getApplicationContext(),
                        R.drawable.gradient_drawable_selector));
                break;

            case ChangeRadius:
                marvelSuperherosButtonGroup.setRadius(5);
                DCSuperHerosButtonGroup.setRadius(50);
                break;

            case ChangePositionAnimated:
                lordOfTheRingsButtonGroup.setPosition(((lordOfTheRingsButtonGroup.getPosition() + 1) % 3), true);
                break;

            case ChangePosition:
                lordOfTheRingsButtonGroup.setPosition(((lordOfTheRingsButtonGroup.getPosition() + 1) % 3), false);
                break;

            case ToggleDraggable:
                darthVaderButtonGroup.setDraggable(!darthVaderButtonGroup.isDraggable());
                break;

            default:
                break;
        }

        // Reset back to the normal value
        spinner.setSelection(0);
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {

    }

    @OnClick(R.id.button_changePosition)
    public void changePositionButton_onClick(View view) {
        int position = gradientButtonGroup.getPosition();
        position = ++position % 3;
        updateButton(position);

        gradientButtonGroup.setPosition(position, true);
    }

    private void setupDynamicDrawables() {
//        final BadgeDrawable drawable = new BadgeDrawable(Color.RED, 80, 50, 3, 3);
//        final SegmentedButton leftButton = (SegmentedButton) findViewById(R.id.left_button);
//        leftButton.setDrawable(drawable);
//
//        SegmentedButtonGroup group = (SegmentedButtonGroup)findViewById(R.id.dynamic_drawable_group);
//        group.setOnClickedButtonListener(new SegmentedButtonGroup.OnClickedButtonListener() {
//            @Override
//            public void onClickedButton(int position) {
//                if(position == 0){
//                    drawable.setCount(drawable.getCount() + 1);
//                    leftButton.requestLayout();
//                }
//            }
//        });
//
//        final SegmentedButton rightButton = (SegmentedButton) findViewById(R.id.right_button);
//        rightButton.setDrawable(R.drawable.ic_b1);
    }

    private void updateButton(int position) {
        changePositionButton.setText("Position: " + position);
    }
}
