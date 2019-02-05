package com.addisonelliott.segmentedbutton.sample;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.content.res.ResourcesCompat;
import com.addisonelliott.segmentedbutton.SegmentedButton;
import com.addisonelliott.segmentedbutton.SegmentedButtonGroup;
import com.addisonelliott.segmentedbutton.sample.drawable.BadgeDrawable;

public class MainActivity extends AppCompatActivity {

//    private Button button;
//    private SegmentedButtonGroup group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView test;

        Typeface face = ResourcesCompat.getFont(getApplicationContext(), 0x7f070000);
        Log.v("SegmentedButton", "this went fine");

        // What main layout thinks it is 0x7f070001
        // R.font = -800001
//        R.font.shaka_pow
//        R.attr.actionBarDivider

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
