package com.addisonelliott.segmentedbutton.sample;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.content.res.ResourcesCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.addisonelliott.segmentedbutton.SegmentedButton;
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import com.addisonelliott.segmentedbutton.sample.drawable.BadgeDrawable;

public class MainActivity extends AppCompatActivity implements OnItemSelectedListener {

    private static final String TAG = "SegmentedButtonSample";

//    @BindView(R.id.buttonGroup_lotr)
//    SegmentedButtonGroup lotrButtonGroup;
//    @BindView(R.id.button_aragorn)
//    SegmentedButton aragornButton;
//    @BindView(R.id.spinner)
//    Spinner spinner;
//    private Button button;
//    private SegmentedButtonGroup group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        // Initialize all variables annotated with @BindView and other variants
//        ButterKnife.bind(this);
//
//        String[] items = {
//                "",
//                "Change ripple color to red",
//                "Change ripple color to green",
//                "Change ripple color to blue",
//                "Query ripple color",
//                "Manually add a new view",
//                "Update text for button"
//        };
//        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
//        spinner.setOnItemSelectedListener(this);

//        group = (SegmentedButtonGroup) findViewById(R.id.segmentedButtonGroup);
//        button = (Button) findViewById(R.id.button);
//
//        updateButton(group.getPosition());
//
//        group.setOnClickedButtonListener(new SegmentedButtonGroup.OnClickedButtonListener() {
//            @Override
//            public void onClickedButton(int position) {
//                updateButton(position);
//            }
//        });
//
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                int position = group.getPosition();
//                position = ++position % 3;
//                updateButton(position);
//                group.setPosition(position, true);
//            }
//        });
//
//
//        group.setEnabled(false);
//
//        Handler handler = new Handler();
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                group.setEnabled(true);
//            }
//        };
//        handler.postDelayed(runnable, 5000);
//
//        setupDynamicDrawables();
    }

    @Override
    public void onItemSelected(final AdapterView<?> parent, final View view, final int position, final long id) {
//        switch (position) {
//            // Change ripple color to red
//            case 1:
//                lotrButtonGroup.setRipple(Color.RED);
//                break;
//
//            // Change ripple color to green
//            case 2:
//                lotrButtonGroup.setRipple(Color.GREEN);
//                break;
//
//            // Change ripple color to blue
//            case 3:
//                lotrButtonGroup.setRipple(Color.BLUE);
//                break;
//
//            // Query ripple color
//            case 4:
//                Log.v(TAG, "Ripple color is: " + Boolean.toString(lotrButtonGroup.getRippleEnabled()) + " " +
//                        Integer.toHexString(lotrButtonGroup.getRippleColor()));
//                break;
//
//            // Manually add a new view
//            case 5:
//                SegmentedButton newButton = new SegmentedButton(getBaseContext());
//                newButton.setId(20202);
//
//                lotrButtonGroup.addView(newButton, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f));
//                break;
//
//            // Update text for button
//            case 6:
//                aragornButton.setText("Test");
//                break;
//
//            default:
//                break;
//        }
//
//        spinner.setSelection(0);
    }

    @Override
    public void onNothingSelected(final AdapterView<?> parent) {

    }

//    private void setupDynamicDrawables() {
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
//    }
//
//    private void updateButton(int position) {
//        button.setText("Position: " + position);
//    }
}
