package com.example.widget;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.provider.Telephony.Mms.Rate;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.Scroller;

import com.example.refreshlayout.R;

public class RefreshLayout extends ViewGroup {

	private static final String TAG = "RefreshLayout";
	private static final float DRAG_RATE = 0.5f;
	private static final int INVALID_POINTER = -1;

	// scroller duration
	private static final int SCROLL_TO_TOP_DURATION = 800;
	private static final int SCROLL_TO_REFRESH_DURATION = 250;
	private static final long SHOW_COMPLETED_TIME = 500;

	private View refreshHeader;
	private View refreshFooter;
	private View target;

	private int currentTargetOffsetTop; // target/header偏移距离
	private int lastTargetOffsetTop;
	private int currentTargetOffsetBottem; // target/footer偏移距离
	private int lastTargetOffsetBottem;

	private boolean hasMeasureHeader; // 是否已经计算头部高度
	private boolean hasMeasureFooter;
	private int touchSlop;

	private int headerHeight; // header高度
	private int footerHeight;
	private int totalDragDistance; // 霢�要下拉这个距离才进入松手刷新状��，默认和header高度丢��?

	private int maxDragDistance;
	private int activePointerId;
	private boolean isTouch;
	private boolean hasSendCancelEvent;
	private float lastMotionX;
	private float lastMotionY;
	private float initDownY;
	private float initDownX;
	private static final int START_POSITION = 0;
	private static int END_POSITION = 0;
	private MotionEvent lastEvent;
	private boolean mIsBeginDragged;
	private AutoScroll autoScroll;
	private State state = State.RESET;
	private OnRefreshListener refreshListener;
	private boolean isAutoRefresh;

	// 刷新成功，显�?00ms成功状��再滚动回顶�?
	private Runnable delayToScrollTopRunnable = new Runnable() {
		@Override
		public void run() {
			autoScroll.scrollTo(START_POSITION, SCROLL_TO_TOP_DURATION);
		}
	};

	private Runnable delayToScrollBootemRunnable = new Runnable() {
		@Override
		public void run() {
			autoScroll.scrollTo(target.getBottom(), END_POSITION,SCROLL_TO_TOP_DURATION);
		}
	};

	private Runnable autoRefreshRunnable = new Runnable() {
		@Override
		public void run() {
			isAutoRefresh = true;
			changeState(State.PULL);
			autoScroll.scrollTo(totalDragDistance, SCROLL_TO_REFRESH_DURATION);
		}
	};

	public RefreshLayout(Context context) {
		this(context, null);
	}

	public RefreshLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		autoScroll = new AutoScroll();
		setRefreshHeader(new RefreshHeaderImpl(context));
		setRefreshFooter(new RefreshFooter(context));
	}

	public void setRefreshHeader(View view) {
		if (view != null && view != refreshHeader) {
			removeView(refreshHeader);
			ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
			if (layoutParams == null) {
				layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
				view.setLayoutParams(layoutParams);
			}
			refreshHeader = view;
			addView(refreshHeader);
		}
	}

	public void setRefreshFooter(View view) {
		if (view != null && view != refreshFooter) {
			removeView(refreshFooter);
			ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
			if (layoutParams == null) {
				layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
				view.setLayoutParams(layoutParams);
			}
			refreshFooter = view;
			addView(refreshFooter);
		}
	}
	
	public void addContentView(View child) {
		// TODO Auto-generated method stub
		((ViewGroup) target).addView(child);
		target.invalidate();
		invalidate();
	}

	public void setRefreshListener(OnRefreshListener refreshListener) {
		this.refreshListener = refreshListener;
	}

	public void refreshComplete() {
		changeState(State.COMPLETE);
		// if refresh completed and the target at top, change state to reset.
		if (currentTargetOffsetTop == START_POSITION) {
			changeState(State.RESET);
		} else {
			// waiting for a time to show refreshView completed state.
			// at next touch event, remove this runnable
			if (!isTouch) {
				postDelayed(delayToScrollTopRunnable, SHOW_COMPLETED_TIME);
			}
		}
	}

	public void refreshAddMoreComplete() {
		((RefreshFooter) refreshFooter).setState(RefreshFooter.STATE_RESET);
		if (!isTouch) {
			//postDelayed(delayToScrollBootemRunnable, SHOW_COMPLETED_TIME);
		}
	}

	public void autoRefresh() {
		autoRefresh(500);
	}

	public void autoRefresh(long duration) {
		if (state != State.RESET) {
			return;
		}
		postDelayed(autoRefreshRunnable, duration);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (target == null) {
			ensureTarget();
		}

		if (target == null) {
			return;
		}

		// ----- measure target -----
		// target占满整屏
		target.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth()
				- getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(getMeasuredHeight()
						- getPaddingTop() - getPaddingBottom(),
						MeasureSpec.EXACTLY));

		// ----- measure refreshHeaderView-----
		measureChild(refreshHeader, widthMeasureSpec, heightMeasureSpec);
		if (!hasMeasureHeader) { // 防止header重复测量
			hasMeasureHeader = true;
			headerHeight = refreshHeader.getMeasuredHeight(); // header高度
			totalDragDistance = headerHeight; // 霢�要pull这个距离才进入松手刷新状�?
			if (maxDragDistance == 0) { // 默认朢�大下拉距离为控件高度的五分之�?
				maxDragDistance = totalDragDistance * 3;
			}
		}
		// ----- measure refreshFooterView-----
		measureChild(refreshFooter, widthMeasureSpec, heightMeasureSpec);
		if (!hasMeasureFooter) { // 防止header重复测量
			footerHeight = refreshFooter.getMeasuredHeight();
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		Log.e(TAG, "onLayout=======");
		final int width = getMeasuredWidth();
		final int height = getMeasuredHeight();
		if (getChildCount() == 0) {
			return;
		}

		if (target == null) {
			ensureTarget();
		}
		if (target == null) {
			return;
		}

		// target铺满屏幕
		final View child = target;
		final int childLeft = getPaddingLeft();
		final int childTop = getPaddingTop() + currentTargetOffsetTop;
		final int childWidth = width - getPaddingLeft() - getPaddingRight();
		final int childHeight = height - getPaddingTop() - getPaddingBottom();
		child.layout(childLeft, childTop, childLeft + childWidth, childTop
				+ childHeight + currentTargetOffsetBottem);

		// header放到target的上方，水平居中
		int refreshViewWidth = refreshHeader.getMeasuredWidth();
		refreshHeader.layout((width / 2 - refreshViewWidth / 2), -headerHeight
				+ currentTargetOffsetTop, (width / 2 + refreshViewWidth / 2),
				currentTargetOffsetTop);
		// footer放到target的上方，水平居中
		int refreshFooterViewWidth = refreshFooter.getMeasuredWidth();
		refreshFooter.layout((width / 2 - refreshFooterViewWidth / 2),
				target.getBottom() - currentTargetOffsetBottem,
				(width / 2 + refreshFooterViewWidth / 2), target.getBottom()
						+ footerHeight - currentTargetOffsetBottem);

		if (!hasMeasureFooter) {
			END_POSITION = refreshFooter.getTop();
			hasMeasureFooter = true;
		}
	}

	/**
	 * 将第丢�个Child作为target
	 */
	private void ensureTarget() {
		// Don't bother getting the parent height if the parent hasn't been laid
		// out yet.
		if (target == null) {
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				if (!child.equals(refreshHeader)
						&& !child.equals(refreshFooter)) {
					target = child;
					break;
				}
			}
		}
	}

	@SuppressLint("NewApi")
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		if (!isEnabled() || target == null) {
			return super.dispatchTouchEvent(ev);
		}

		final int actionMasked = ev.getActionMasked(); // support Multi-touch
		switch (actionMasked) {
		case MotionEvent.ACTION_DOWN:
			Log.e(TAG, "ACTION_DOWN");
			activePointerId = ev.getPointerId(0);
			isAutoRefresh = false;
			isTouch = true;
			hasSendCancelEvent = false;
			mIsBeginDragged = false;
			lastTargetOffsetTop = currentTargetOffsetTop;
			currentTargetOffsetTop = target.getTop();
			initDownX = lastMotionX = ev.getX(0);
			initDownY = lastMotionY = ev.getY(0);
			autoScroll.stop();
			removeCallbacks(delayToScrollTopRunnable);
			removeCallbacks(autoRefreshRunnable);
			super.dispatchTouchEvent(ev);
			return true;

		case MotionEvent.ACTION_MOVE:
			if (activePointerId == INVALID_POINTER) {
				Log.e(TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
				return super.dispatchTouchEvent(ev);
			}
			lastEvent = ev;
			float x = ev.getX(MotionEventCompat.findPointerIndex(ev,
					activePointerId));
			float y = ev.getY(MotionEventCompat.findPointerIndex(ev,
					activePointerId));
			float yDiff = y - lastMotionY;
			float offsetY = yDiff * DRAG_RATE;
			lastMotionX = x;
			lastMotionY = y;

			if (!mIsBeginDragged && Math.abs(y - initDownY) > touchSlop) {
				mIsBeginDragged = true;
			}

			if (mIsBeginDragged) {
				boolean moveDown = offsetY > 0;
				boolean canMoveDown = canChildScrollUp();
				boolean moveUp = !moveDown;
				boolean canMoveTopUp = currentTargetOffsetTop > START_POSITION;

				// refresh footer pop
				if ( canChildScrollDown()) {
					moveSpinnerDown(offsetY);
					return true;
				}

				// refresh header pop
				if ((moveDown && !canMoveDown) || (moveUp && canMoveTopUp)) {
					moveSpinner(offsetY);
					return true;
				}
			}
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			isTouch = false;
			if (target.getBottom() < END_POSITION
					&&((RefreshFooter) refreshFooter).getState() != RefreshFooter.STATE_LOADING) {
				autoScroll.scrollTo(target.getBottom(), END_POSITION,SCROLL_TO_TOP_DURATION);
				break;
			}
			if (currentTargetOffsetTop > START_POSITION) {
				finishSpinner();
			}
			activePointerId = INVALID_POINTER;
			break;

		case MotionEvent.ACTION_POINTER_DOWN:
			int pointerIndex = MotionEventCompat.getActionIndex(ev);
			if (pointerIndex < 0) {
				Log.e(TAG,"Got ACTION_POINTER_DOWN event but have an invalid action index.");
				return super.dispatchTouchEvent(ev);
			}
			lastMotionX = ev.getX(pointerIndex);
			lastMotionY = ev.getY(pointerIndex);
			lastEvent = ev;
			activePointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
			break;

		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			lastMotionY = ev.getY(ev.findPointerIndex(activePointerId));
			lastMotionX = ev.getX(ev.findPointerIndex(activePointerId));
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	private void moveSpinnerDown(float diff) {
		
		if(((RefreshFooter) refreshFooter).getState() == RefreshFooter.STATE_LOADING){
			return;
		}
		
		if (!hasSendCancelEvent && isTouch) {
			sendCancelEvent();
			hasSendCancelEvent = true;
		}
		
		if (END_POSITION - target.getBottom() >= footerHeight*0.7) {
			Log.i("index"," offset = "+(END_POSITION - target.getBottom())+"footerHeight="+footerHeight);
			((RefreshFooter) this.refreshFooter).setState(RefreshFooter.STATE_LOADING);
			autoScroll.scrollTo(refreshFooter.getTop(), refreshFooter.getTop()+footerHeight,3000);
			if (refreshListener != null) { refreshListener.onRefresh(TYPE_FOOTER);}
			return;
		}
	
		int offset = Math.round(diff);
		target.offsetTopAndBottom(offset);
		refreshHeader.offsetTopAndBottom(offset);
		refreshFooter.offsetTopAndBottom(offset);
		invalidate();
	}

	private void moveSpinner(float diff) {
		int offset = Math.round(diff);
		if (offset == 0) {
			return;
		}

		if (!hasSendCancelEvent && isTouch
				&& currentTargetOffsetTop > START_POSITION) {
			sendCancelEvent();
			hasSendCancelEvent = true;
		}

		int targetY = Math.max(0, currentTargetOffsetTop + offset);
		// y = x - (x/2)^2
		float extraOS = targetY - totalDragDistance;
		float slingshotDist = totalDragDistance;
		float tensionSlingshotPercent = Math.max(0,
				Math.min(extraOS, slingshotDist * 2) / slingshotDist);
		float tensionPercent = (float) (tensionSlingshotPercent - Math.pow(
				tensionSlingshotPercent / 2, 2));

		if (offset > 0) {
			offset = (int) (offset * (1f - tensionPercent));
			targetY = Math.max(0, currentTargetOffsetTop + offset);
		}

		// 1. 在RESET状��时，第丢�次下拉出现header的时候，设置状��变成PULL
		if (state == State.RESET && currentTargetOffsetTop == START_POSITION
				&& targetY > 0) {
			changeState(State.PULL);
		}

		// 2. 在PULL或��COMPLETE状��时，header回到顶部的时候，状��变回RESET
		if (currentTargetOffsetTop > START_POSITION
				&& targetY <= START_POSITION) {
			if (state == State.PULL || state == State.COMPLETE) {
				changeState(State.RESET);
			}
		}

		// 3. 如果是从底部回到顶部的过�?徢�上滚�?，并且手指是松开状��?
		// 并且当前是PULL状��，状��变成LOADING，这时��我们需要强制停止autoScroll
		if (state == State.PULL && !isTouch
				&& currentTargetOffsetTop > totalDragDistance
				&& targetY <= totalDragDistance) {
			autoScroll.stop();
			changeState(State.LOADING);
			if (refreshListener != null) {
				refreshListener.onRefresh(TYPE_HEADER);
			}
			// 因为判断条件targetY <=
			// totalDragDistance，会导致不能回到正确的刷新高度（有那么一丁点偏差），调整change
			int adjustOffset = totalDragDistance - targetY;
			offset += adjustOffset;
		}

		setTargetOffsetTopAndBottom(offset);

		// 别忘了回调header的位置改变方法��?
		if (refreshHeader instanceof IRefreshHeader) {
			((IRefreshHeader) refreshHeader).onPositionChange(
					currentTargetOffsetTop, lastTargetOffsetTop,
					totalDragDistance, isTouch, state);
		}

	}

	private void finishSpinner() {
		if (state == State.LOADING) {
			if (currentTargetOffsetTop > totalDragDistance) {
				autoScroll.scrollTo(totalDragDistance, SCROLL_TO_REFRESH_DURATION);
			}
		} else {
			autoScroll.scrollTo(START_POSITION, SCROLL_TO_TOP_DURATION);
		}
	}

	private void changeState(State state) {
		this.state = state;
		IRefreshHeader refreshHeader = this.refreshHeader instanceof IRefreshHeader ? ((IRefreshHeader) this.refreshHeader)
				: null;
		if (refreshHeader != null) {
			switch (state) {
			case RESET:
				refreshHeader.reset();
				break;
			case PULL:
				refreshHeader.pull();
				break;
			case LOADING:
				refreshHeader.refreshing();
				break;
			case COMPLETE:
				refreshHeader.complete();
				break;
			}
		}
	}

	private void setTargetOffsetTopAndBottom(int offset) {
		if (offset == 0) {
			return;
		}
		target.offsetTopAndBottom(offset);
		refreshHeader.offsetTopAndBottom(offset);
		refreshFooter.offsetTopAndBottom(offset);
		lastTargetOffsetTop = currentTargetOffsetTop;
		currentTargetOffsetTop = target.getTop();
		currentTargetOffsetBottem = 0;
		invalidate();
	}

	private void sendCancelEvent() {
		if (lastEvent == null) {
			return;
		}
		MotionEvent ev = MotionEvent.obtain(lastEvent);
		ev.setAction(MotionEvent.ACTION_CANCEL);
		super.dispatchTouchEvent(ev);
	}

	private void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = MotionEventCompat.getActionIndex(ev);
		final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == activePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			lastMotionY = ev.getY(newPointerIndex);
			lastMotionX = ev.getX(newPointerIndex);
			activePointerId = MotionEventCompat.getPointerId(ev,
					newPointerIndex);
		}
	}

	public boolean canChildScrollUp() {
		if (android.os.Build.VERSION.SDK_INT < 14) {
			if (target instanceof AbsListView) {
				final AbsListView absListView = (AbsListView) target;
				return absListView.getChildCount() > 0
						&& (absListView.getFirstVisiblePosition() > 0 || absListView
								.getChildAt(0).getTop() < absListView
								.getPaddingTop());
			} else {
				return ViewCompat.canScrollVertically(target, -1)
						|| target.getScrollY() > 0;
			}
		} else {
			return ViewCompat.canScrollVertically(target, -1);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public boolean canChildScrollDown() {
		if (target instanceof AbsListView) {
			AbsListView absListView = (AbsListView) target;
			return absListView.getChildCount() > 0
					&& absListView.getLastVisiblePosition() == absListView.getCount() - 1
					&& END_POSITION - target.getBottom() < footerHeight
					&& END_POSITION - target.getBottom() >= 0
					&& ((RefreshFooter) refreshFooter).getState() == RefreshFooter.STATE_RESET;
		} else {
			return ViewCompat.canScrollVertically(target, -1)
					&& END_POSITION - target.getBottom() < footerHeight
					&& END_POSITION - target.getBottom() >= 0
					&& ((RefreshFooter) refreshFooter).getState() == RefreshFooter.STATE_RESET;
		}
	}

	private class AutoScroll implements Runnable {
		private Scroller scroller;
		private int lastY;

		public AutoScroll() {
			scroller = new Scroller(getContext());
		}

		@Override
		public void run() {
			boolean finished = !scroller.computeScrollOffset()
					|| scroller.isFinished();
			if (!finished) {
				int currY = scroller.getCurrY();
				int offset = currY - lastY;
				lastY = currY;
				moveSpinner(offset);
				post(this);
				onScrollFinish(false);
			} else {
				stop();
				onScrollFinish(true);
			}
		}

		public void scrollTo(int to, int duration) {
			int from = currentTargetOffsetTop;
			int distance = to - from;
			stop();
			if (distance == 0) {
				return;
			}
			scroller.startScroll(0, 0, 0, distance, duration);
			post(this);
		}

		public void scrollTo(int from, int to, int duration) {
			int distance = to - from;
			stop();
			if (distance == 0) {
				return;
			}
			scroller.startScroll(0, 0, 0, distance, duration);
			post(this);
		}

		private void stop() {
			removeCallbacks(this);
			if (!scroller.isFinished()) {
				scroller.forceFinished(true);
			}
			lastY = 0;
		}
	}

	/**
	 * 在scroll结束的时候会回调这个方法
	 *
	 * @param isForceFinish
	 *            是否是强制结束的
	 */
	private void onScrollFinish(boolean isForceFinish) {
		if (isAutoRefresh && !isForceFinish) {
			isAutoRefresh = false;
			changeState(State.LOADING);
			if (refreshListener != null) {
				refreshListener.onRefresh(TYPE_HEADER);
			}
			finishSpinner();
		}
	}

	public static int TYPE_HEADER = 0;
	public static int TYPE_FOOTER = 1;

	public interface OnRefreshListener {
		void onRefresh(int type);
	}
}
