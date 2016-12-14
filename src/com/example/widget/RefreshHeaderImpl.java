package com.example.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.example.refreshlayout.R;

/**
 * Created by AItsuki on 2016/6/15.
 *
 */
public class RefreshHeaderImpl extends FrameLayout implements IRefreshHeader {


    private Animation rotate_up;
    private Animation rotate_down;
    private Animation rotate_infinite;
    private TextView textView;
    private View arrowIcon;
    private View successIcon;
    private View loadingIcon;

    public RefreshHeaderImpl(Context context) {
        this(context, null);
    }

    public RefreshHeaderImpl(Context context, AttributeSet attrs) {
        super(context, attrs);

        rotate_up = AnimationUtils.loadAnimation(context , R.anim.rotate_up);
        rotate_down = AnimationUtils.loadAnimation(context , R.anim.rotate_down);
        rotate_infinite = AnimationUtils.loadAnimation(context , R.anim.rotate_infinite);

        inflate(context, R.layout.header_qq, this);

        textView = (TextView) findViewById(R.id.text);
        arrowIcon = findViewById(R.id.arrowIcon);
        successIcon = findViewById(R.id.successIcon);
        loadingIcon = findViewById(R.id.loadingIcon);
    }

    @Override
    public void reset() {
        textView.setText(getResources().getText(R.string.qq_header_reset));
        successIcon.setVisibility(INVISIBLE);
        arrowIcon.setVisibility(VISIBLE);
        arrowIcon.clearAnimation();
        loadingIcon.setVisibility(INVISIBLE);
        loadingIcon.clearAnimation();
    }

    @Override
    public void pull() {

    }

    @Override
    public void refreshing() {
        arrowIcon.setVisibility(INVISIBLE);
        loadingIcon.setVisibility(VISIBLE);
        textView.setText(getResources().getText(R.string.qq_header_refreshing));
        arrowIcon.clearAnimation();
        loadingIcon.startAnimation(rotate_infinite);
    }

    @Override
    public void onPositionChange(float currentPos, float lastPos, float refreshPos, boolean isTouch, State state) {
        // 寰�涓婃媺
        if (currentPos < refreshPos && lastPos >= refreshPos) {
            if (isTouch && state == State.PULL) {
                textView.setText(getResources().getText(R.string.qq_header_pull));
                arrowIcon.clearAnimation();
                arrowIcon.startAnimation(rotate_down);
            }
            // 寰�涓嬫媺
        } else if (currentPos > refreshPos && lastPos <= refreshPos) {
            if (isTouch && state == State.PULL) {
                textView.setText(getResources().getText(R.string.qq_header_pull_over));
                arrowIcon.clearAnimation();
                arrowIcon.startAnimation(rotate_up);
            }
        }
    }

    @Override
    public void complete() {
        loadingIcon.setVisibility(INVISIBLE);
        loadingIcon.clearAnimation();
        successIcon.setVisibility(VISIBLE);
        textView.setText(getResources().getText(R.string.qq_header_completed));
    }
}
