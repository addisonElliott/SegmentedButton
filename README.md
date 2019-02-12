[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-SegmentedButton-green.svg?style=true)](https://android-arsenal.com/details/1/4445) [![](https://jitpack.io/v/addisonelliott/segmentedbutton.svg)](https://jitpack.io/#addisonelliott/segmentedbutton)

# SegmentedButton

Android view that mimics iOS's SegmentedControl

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
    compile 'com.github.addisonelliott:SegmentedButton:v3.0.0'
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

| Attribute                          | Format           | Description                              |
| ---------------------------------- | ---------------- | ---------------------------------------- |
| app:background                     | `drawable|color` | Set ripple color for every button        |
| app:selectedBackground             | `drawable|color` | Set ripple color for every button with custom color |
| app:borderWidth                    | `dimension`      | If selector on it, set tint onto image for every button |
| app:borderColor                    | `color`          | If selector on it, set text color for every button |
| app:borderDashWidth                | `dimension`      | Set selector color                       |
| app:borderDashGap                  | `dimension`      | Set divider size                         |
| app:radius                         | `dimension`      | Set divider padding for top and bottom   |
| app:position                       | `integer`        | Change divider color                     |
| app:draggable                      | `boolean`        | Round divider                            |
| app:ripple                         | `boolean`        | Shadow for container layout (api21+)     |
| app:rippleColor                    | `color`          | Shadow for container layout (api21+)     |
| app:divider                        | `drawable|color` | Set margin to make shadow visible (api21+) |
| app:dividerWidth                   | `dimension`      | Set selected button position             |
| app:dividerRadius                  | `dimension`      | Make layout rounder                      |
| app:dividerPadding                 | `dimension`      | Set background color of container (except transparent color) |
| app:selectionAnimationDuration     | `integer`        | Set how long selector travels to selected position |
| app:selectionAnimationInterpolator | `enum`           | Set selector animation (ex. bounce animation) |


### SegmentedButton

| Option Name                     | Format           | Description                              |
| ------------------------------- | ---------------- | ---------------------------------------- |
| app:background                  | `drawable|color` | Set ripple color for every button        |
| app:selectedBackground          | `drawable|color` | Set ripple color for every button with custom color |
| app:rippleColor                 | `color`          | If selector on it, set tint onto image for every button |
| app:drawable                    | `drawable`       | If selector on it, set text color for every button |
| app:drawablePadding             | `dimension`      | Set selector color                       |
| app:drawableTint                | `color`          | Set divider size                         |
| app:selectedDrawableTint        | `color`          | Set divider padding for top and bottom   |
| app:drawableWidth               | `dimension`      | Change divider color                     |
| app:drawableHeight              | `dimension`      | Round divider                            |
| app:drawableGravity             | `enum`           | Shadow for container layout (api21+)     |
| app:text                        | `string`         | Shadow for container layout (api21+)     |
| app:textColor                   | `color`          | Set margin to make shadow visible (api21+) |
| app:selectedTextColor           | `color`          | Set selected button position             |
| app:textSize                    | `dimension`      | Make layout rounder                      |
| android:fontFamily              | `font`           | Set background color of container (except transparent color) |
| app:textStyle                   | `flag`           | Set how long selector travels to selected position |






### Some Attributes

#### Segmented Button
| Option Name              | Format  | Description                              |
| ------------------------ | ------- | ---------------------------------------- |
| app:sb_imageTint         | `color` | Set tint onto button's image             |
| app:sb_imageScale        | `float` | Scale button's image                     |
| app:sb_selectedImageTint | `color` | Set tint onto button's image if selector on it |
| app:sb_selectedTextColor | `color` | Set color onto button's text if selector on it |
| app:sb_rippleColor       | `color` | Set ripple color of button               |

#### Segmented Button Group
| Option Name                     | Format      | Description                              |
| ------------------------------- | ----------- | ---------------------------------------- |
| app:sbg_ripple                  | `boolean`   | Set ripple color for every button        |
| app:sbg_rippleColor             | `color`     | Set ripple color for every button with custom color |
| app:sbg_selectorImageTint       | `color`     | If selector on it, set tint onto image for every button |
| app:sbg_selectorTextColor       | `color`     | If selector on it, set text color for every button |
| app:sbg_selectorColor           | `color`     | Set selector color                       |
| app:sbg_dividerSize             | `dimension` | Set divider size                         |
| app:sbg_dividerPadding          | `dimension` | Set divider padding for top and bottom   |
| app:sbg_dividerColor            | `color`     | Change divider color                     |
| app:sbg_dividerRadius           | `dimension` | Round divider                            |
| app:sbg_shadow                  | `boolean`   | Shadow for container layout (api21+)     |
| app:sbg_shadowElevation         | `dimension` | Shadow for container layout (api21+)     |
| app:sbg_shadowMargin            | `dimension` | Set margin to make shadow visible (api21+) |
| app:sbg_position                | `integer`   | Set selected button position             |
| app:sbg_radius                  | `dimension` | Make layout rounder                      |
| app:sbg_backgroundColor         | `color`     | Set background color of container (except transparent color) |
| app:sbg_animateSelectorDuration | `integer`   | Set how long selector travels to selected position |
| app:sbg_animateSelector         | `integer`   | Set selector animation (ex. bounce animation) |
| app:sbg_borderSize              | `dimension` | Add border by giving dimension           |
| app:sbg_borderColor             | `color`     | Change border color (Default: Grey)      |

### Animations Available

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

These animations can be set using the attribute noted above like so: `app:sbg_animateSelector="bounce"`. Also make sure to play with the `app:sbg_animateSelectorDuration` attribute to get the animation to look exactly how you want it.

## Todo List

This project is still under a major rehaul. The following is a list of items left to be done.

* [ ] Upload to JitPack
* [ ] Advertise branch via issues and PRs in upstream repo
* [ ] Update README to include better graphics and docs and real JitPack version
* [ ] Major rehaul to README
    * [ ] Include better previews with just simple segmented controls
    * [ ] Have a description for each preview option to see what this contains
    * [ ] Contain a note that the code for these previews can be found in the examples
* [ ] Consider switching to Maven from JitPack in future

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

