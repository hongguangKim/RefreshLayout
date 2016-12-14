package com.example.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.refreshlayout.R;

public class RefreshFooter extends FrameLayout implements IRefreshFooter {
	
	public final static int STATE_RESET = 0;
	public final static int STATE_LOADING = 1;
	public int state = STATE_RESET;
	
	private TextView mHintView;
	private ImageView loadingIcon;
	private Animation rotate_infinite;

	public RefreshFooter(Context context) {
		super(context);
		init(context);
	}

	public RefreshFooter(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public int getState() {
		return this.state;
	}

	public void setState(int state) {
		this.state = state;
		if (state == STATE_LOADING) {
			refreshing();
		} else if (state == STATE_RESET) {
			reset();
		}
	}

	@Override
	public void init(Context context) {
		rotate_infinite = AnimationUtils.loadAnimation(context,R.anim.rotate_infinite);
		inflate(context, R.layout.refresh_footer, this);
		mHintView = (TextView) findViewById(R.id.refresh_footer_hint_textview);
		loadingIcon = (ImageView)findViewById(R.id.refresh_footer_loadingIcon);
		setState(STATE_RESET);
	}

	@Override
	public void reset() {
		mHintView.setText(getResources().getText(R.string.footerr_reset));
		loadingIcon.setVisibility(INVISIBLE);
		loadingIcon.clearAnimation();
	}

	@Override
	public void refreshing() {
		mHintView.setText(getResources().getText(R.string.footer_refreshing));
		loadingIcon.setVisibility(VISIBLE);
		loadingIcon.startAnimation(rotate_infinite);
	}
}