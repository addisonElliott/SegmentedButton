[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-SegmentedButton-green.svg?style=true)](https://android-arsenal.com/details/1/4445) [![](https://jitpack.io/v/addisonelliott/segmentedbutton.svg)](https://jitpack.io/#addisonelliott/segmentedbutton)

# SegmentedButton

Android view that mimics iOS's [SegmentedControl](https://developer.apple.com/design/human-interface-guidelines/ios/controls/segmented-controls/)

## Features
* Customizable text color, size and font
* Custom button drawables
* Customizable button dividers
* Solid and dashed border support
* Ripple effect on button tap
* Draggable buttons
* Animations

## Old Library

This project is originally forked from [ceryle/SegmentedButton](https://github.com/ceryle/SegmentedButton) but has been revamped and given some TLC. The parent repository has been stagnant since **October 17th, 2017**.

## Preview
![1](https://cloud.githubusercontent.com/assets/20969019/21565956/9fec9300-cea6-11e6-981f-c5c2a70a2e57.gif)

![2](https://cloud.githubusercontent.com/assets/20969019/21565963/bdda9aba-cea6-11e6-91ba-6e6b0230a512.gif)

![3](https://cloud.githubusercontent.com/assets/20969019/21565967/c3dd51d2-cea6-11e6-91c4-1d3fa0ee6884.gif)

![6](https://cloud.githubusercontent.com/assets/20969019/21565976/df8a7fc2-cea6-11e6-8740-debb45d1aff7.gif)

![4](https://cloud.githubusercontent.com/assets/20969019/21565969/ce06efa6-cea6-11e6-8878-6260054bb3be.gif)

![5](https://cloud.githubusercontent.com/assets/20969019/21565972/d6df69d2-cea6-11e6-8391-27b1d45b945b.gif)

![7](https://cloud.githubusercontent.com/assets/20969019/21565978/ec2fb698-cea6-11e6-8ae9-54326e3ebdf4.gif)

![8](https://cloud.githubusercontent.com/assets/20969019/24909871/6b0a8b10-1ece-11e7-8686-df8276f1ae15.gif)


## Installation

#### Gradle

Add it to your build.gradle with:
```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```
and:

```gradle
dependencies {
    compile 'com.github.addisonelliott:SegmentedButton:3.0.0'
}
```

## Usage

### Layout XML
```xml
<com.addisonelliott.segmentedbutton.SegmentedButtonGroup
    android:id="@+id/buttonGroup_lordOfTheRings"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="center_horizontal"
    android:layout_margin="4dp"
    android:elevation="2dp"
    app:background="@color/white"
    app:borderColor="@color/orange_700"
    app:borderWidth="1dp"
    app:divider="@color/orange_700"
    app:dividerPadding="10dp"
    app:dividerWidth="1dp"
    app:position="0"
    app:radius="30dp"
    app:ripple="true"
    app:rippleColor="@color/green_800"
    app:selectedBackground="@color/green_900">

    <com.addisonelliott.segmentedbutton.SegmentedButton
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:fontFamily="@font/aniron"
        android:padding="10dp"
        app:drawable="@drawable/ic_aragorn"
        app:drawableGravity="top"
        app:selectedTextColor="@color/orange_700"
        app:text="Aragorn"
        app:textColor="@color/black" />

    <com.addisonelliott.segmentedbutton.SegmentedButton
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:fontFamily="@font/aniron"
        android:padding="10dp"
        app:drawable="@drawable/ic_gimli"
        app:drawableGravity="top"
        app:selectedTextColor="@color/grey_600"
        app:text="Gimli"
        app:textColor="@color/black" />

    <com.addisonelliott.segmentedbutton.SegmentedButton
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_weight="1"
        android:fontFamily="@font/aniron"
        android:padding="10dp"
        app:drawable="@drawable/ic_legolas"
        app:drawableGravity="top"
        app:selectedTextColor="@color/yellow_200"
        app:text="Legolas"
        app:textColor="@color/black" />
</com.addisonelliott.segmentedbutton.SegmentedButtonGroup>
```

### Java
```java
segmentedButtonGroup.setOnPositionChangedListener(new OnPositionChangedListener() {
    @Override
    public void onPositionChanged(final int position) {
        // Handle stuff here
    }
});

// Get current position
segmentedButtonGroup.getPosition();
```

## Attributes

### SegmentedButtonGroup

| Attribute                          | Format            | Description                                                                |
| ---------------------------------- | ----------------- | -------------------------------------------------------------------------- |
| app:background                     | `drawable\|color` | Set background for every button when unselected                            |
| app:selectedBackground             | `drawable\|color` | Set background for every button when selected                              |
| app:borderWidth                    | `dimension`       | Width of border around button group                                        |
| app:borderColor                    | `color`           | Color of border                                                            |
| app:borderDashWidth                | `dimension`       | Width of dashes, 0 indicates solid line                                    |
| app:borderDashGap                  | `dimension`       | Width of gaps in dashes                                                    |
| app:radius                         | `dimension`       | Radius of corners for button group                                         |
| app:position                       | `integer`         | Default button that is selected                                            |
| app:draggable                      | `boolean`         | Whether or not buttons can be dragged to change selected state             |
| app:ripple                         | `boolean`         | Whether or not ripple effect is enabled for all buttons                    |
| app:rippleColor                    | `color`           | Ripple effect tint color for each button                                   |
| app:divider                        | `drawable\|color` | Drawable or color to display for divider between buttons                   |
| app:dividerWidth                   | `dimension`       | Width of the divider between buttons, 0 indicates no dividers              |
| app:dividerRadius                  | `dimension`       | Corner radius for divider to round edges                                   |
| app:dividerPadding                 | `dimension`       | Divider padding on top and bottom of divider                               |
| app:selectionAnimationDuration     | `integer`         | Duration in ms for change button selection animation                       |
| app:selectionAnimationInterpolator | `enum`            | Type of animation used for changing button. Valid options are listed below |

### SegmentedButton

| Option Name                     | Format            | Description                                                                  |
| ------------------------------- | ----------------- | ---------------------------------------------------------------------------- |
| app:background                  | `drawable\|color` | Set background for button when unselected                                    |
| app:selectedBackground          | `drawable\|color` | Set background for button when selected                                      |
| app:rippleColor                 | `color`           | Ripple effect tint color when user taps on button                            |
| app:drawable                    | `drawable`        | Drawable to display                                                          |
| app:drawablePadding             | `dimension`       | Padding between drawable and text                                            |
| app:drawableTint                | `color`           | Tint color for drawable when unselected                                      |
| app:selectedDrawableTint        | `color`           | Tint color for drawable when selected                                        |
| app:drawableWidth               | `dimension`       | Width of drawable (default uses intrinsic)                                   |
| app:drawableHeight              | `dimension`       | Height of drawable (default uses intrinsic)                                  |
| app:drawableGravity             | `enum`            | Determines where drawable should be placed in relation to the text. Valid options are `Gravity.LEFT`, `Gravity.TOP`, `Gravity.RIGHT`, and `Gravity.BOTTOM`                                                     |
| app:text                        | `string`          | Text to display on button                                                    |
| app:textColor                   | `color`           | Color of text when button is unselected                                      |
| app:selectedTextColor           | `color`           | Color of text when button is selected                                        |
| app:textSize                    | `dimension`       | Font size of text                                                            |
| android:fontFamily              | `font`            | Font for displaying text                                                     |
| app:textStyle                   | `flag`            | Text style, can be `Typeface.NORMAL`, `Typeface.BOLD`, and `Typeface.ITALIC` |

**All layout attributes have a corresponding function in Java that can be called to change programatically. See Javadocs of source code for more information.**

### Available Animations

- fastOutSlowIn
- bounce
- linear
- decelerate
- cycle
- anticipate
- accelerateDecelerate
- accelerate
- anticipateOvershoot
- fastOutLinearIn
- linearOutSlowIn
- overshoot

These animations can be set using the attribute noted above like so: `app:selectionAnimationInterpolator="bounce"`.

## Support

Issues and pull requests are encouraged.

## License

This project is licensed under the Apache License Version 2.0 - see the [LICENSE](LICENSE) file for details

```
Copyright (C) 2016 ceryle
Copyright (C) 2019 Addison Elliott

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

