package com.landscape.dragrefreshview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import static com.landscape.dragrefreshview.Range.DRAG_MAX_DISTANCE;
import static com.landscape.dragrefreshview.Range.DRAG_MAX_RANGE;
import static com.landscape.dragrefreshview.Range.DRAW_PADDING;

/**
 * Created by 1 on 2016/9/7.
 */
public class DragRefreshLayout extends FrameLayout implements DragDelegate.DragActionBridge {
    ViewDragHelper dragHelper;
    View mTarget = null, emptyView = null;
    ImageView refreshView, loadView;
    private int emptyId = 0, contentId = 0;
    private RingDrawable mRefreshDrawable, mLoadDrawable;


    int contentTop = 0;
    ScrollStatus status = ScrollStatus.IDLE, tempStatus = ScrollStatus.IDLE;
    DragDelegate dragDelegate = null;

    public DragRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        dragHelper = ViewDragHelper.create(this, 0.3f, dragHelperCallback);
        dragDelegate = new DragDelegate(this);

        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.refresh_DragRefreshLayout);
        emptyId = a.getResourceId(R.styleable.refresh_DragRefreshLayout_refresh_empty, 0);
        contentId = a.getResourceId(R.styleable.refresh_DragRefreshLayout_refresh_content, 0);
        a.recycle();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return dragDelegate.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return dragDelegate.onTouchEvent(event);
    }

    @Override
    public ScrollStatus scrollStatus() {
        return status;
    }

    @Override
    public int contentTop() {
        return contentTop;
    }

    @Override
    public ViewDragHelper dragHelper() {
        return dragHelper;
    }

    @Override
    public View target() {
        if (mTarget.isShown()) {
            return mTarget;
        }
        if (emptyView != null && emptyView.isShown()) {
            return emptyView;
        }
        return mTarget;
    }

    @Override
    public void resetRefresh() {
        setRefreshing(false,false);
    }

    @Override
    public void resetLoading() {
        setLoading(false,false);
    }

    @Override
    public void setDrawPercent(float drawPercent) {
        mRefreshDrawable.setPercent(drawPercent);
        mLoadDrawable.setPercent(drawPercent);
        mRefreshDrawable.invalidateSelf();
        mLoadDrawable.invalidateSelf();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        addRefreshViews();
        ensureTarget();
    }

    private void addRefreshViews() {
        refreshView = new ImageView(getContext());
        loadView = new ImageView(getContext());
        refreshView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(DRAG_MAX_DISTANCE)));
        loadView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp2px(DRAG_MAX_DISTANCE)));
        refreshView.setPadding(0,dp2px(DRAW_PADDING),0,dp2px(DRAW_PADDING));
        loadView.setPadding(0,dp2px(DRAW_PADDING),0,dp2px(DRAW_PADDING));

        initDrawable();
        refreshView.setImageDrawable(mRefreshDrawable);
        loadView.setImageDrawable(mLoadDrawable);
        refreshView.setBackgroundColor(Color.BLUE);
        loadView.setBackgroundColor(Color.BLACK);

        addView(refreshView, 0);
        addView(loadView);
    }

    private void initDrawable() {
        mRefreshDrawable = new RingDrawable(this);
        mLoadDrawable = new RingDrawable(this);
    }

    private void ensureTarget() {
        if (contentId == 0) {
            if (getChildCount() > 0) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    if (child != refreshView && child != loadView) {
                        mTarget = child;
                        mTarget.setClickable(true);
                        return;
                    }
                }
            }
        } else {
            mTarget = findViewById(contentId);
            mTarget.setClickable(true);
        }
        if (emptyId != 0) {
            emptyView = findViewById(emptyId);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutViews();
    }

    private void layoutViews() {
        if (refreshView == null || loadView == null) {
            return;
        }
        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        if (mTarget != null) {
            mTarget.layout(paddingLeft, paddingTop + contentTop, width - paddingRight, contentTop + height - paddingBottom);
        }
        if (emptyView != null) {
            emptyView.layout(paddingLeft, paddingTop , width - paddingRight, height - paddingBottom);
        }
        refreshView.layout(
                paddingLeft,
                contentTop - refreshView.getMeasuredHeight() + paddingTop,
                width - paddingRight,
                contentTop + paddingTop);
        loadView.layout(
                paddingLeft,
                contentTop + height - paddingBottom,
                width - paddingRight,
                contentTop + height + loadView.getMeasuredHeight() - paddingBottom);
    }

    ViewDragHelper.Callback dragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            status = ScrollStatus.DRAGGING;
            if (contentTop + dy > DRAG_MAX_RANGE) {
                return DRAG_MAX_RANGE;
            } else if (contentTop + dy < -DRAG_MAX_RANGE) {
                return -DRAG_MAX_RANGE;
            } else {
                return top;
            }
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return DRAG_MAX_RANGE;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (contentTop > dp2px(DRAG_MAX_DISTANCE)) {
                setRefreshing(true);
                setDrawPercent(1f);
            } else if (contentTop < -dp2px(DRAG_MAX_DISTANCE)) {
                setLoading(true);
                setDrawPercent(1f);
            } else if (contentTop > 0) {
                setRefreshing(false);
            } else {
                setLoading(false);
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (changedView == mTarget) {
                Log.i("dragRefresh", "top:" + top);
                if (!ScrollViewCompat.canScrollDown(mTarget)
                        && top < 0) {
//                    setRefreshing(false,false);
                    contentTop = 0;
                    layoutViews();
                } else if (ScrollViewCompat.canScrollDown(mTarget)
                        && !ScrollViewCompat.canScrollUp(mTarget)
                        && top > 0) {
//                    setLoading(false,false);
                    contentTop = 0;
                    layoutViews();
                } else {
                    refreshView.offsetTopAndBottom(dy);
                    loadView.offsetTopAndBottom(dy);
                    contentTop = top;
                    invalidate();
                }
            } else if (changedView == emptyView) {
//                Log.i("dragRefresh", "top:" + top);
                refreshView.offsetTopAndBottom(dy);
                loadView.offsetTopAndBottom(dy);
                contentTop = top;
                invalidate();
            }
        }
    };

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    boolean lastAnimState = true,animContinue = true;

    @Override
    public void computeScroll() {
        animContinue = dragHelper.continueSettling(true);
        if (animContinue && lastAnimState == animContinue) {
            ViewCompat.postInvalidateOnAnimation(this);
            mRefreshDrawable.invalidateSelf();
            mLoadDrawable.invalidateSelf();
        } else if (lastAnimState != animContinue){
            if (ScrollStatus.isRefreshing(tempStatus)) {
                mRefreshDrawable.start();
            } else if (ScrollStatus.isLoading(tempStatus)) {
                mLoadDrawable.start();
            } else if (ScrollStatus.isIdle(tempStatus)) {
                mRefreshDrawable.stop();
                mLoadDrawable.stop();
            }
            status = tempStatus;
            lastAnimState = animContinue;
        }
    }

    public void setRefreshing(boolean refreshing) {
        setRefreshing(refreshing, true);
    }

    public void setRefreshing(boolean refreshing, boolean animation) {
        if (animation) {
            if (refreshing) {
                lastAnimState = true;
                if (dragHelper.smoothSlideViewTo(mTarget, 0, dp2px(DRAG_MAX_DISTANCE))) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            } else {
                lastAnimState = true;
                if (dragHelper.smoothSlideViewTo(mTarget, 0, 0)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
            if (refreshing) {
                tempStatus = ScrollStatus.REFRESHING;
            } else {
                tempStatus = ScrollStatus.IDLE;
            }
        } else {
            if (refreshing) {
                contentTop = dp2px(DRAG_MAX_DISTANCE);
                layoutViews();
            } else {
                contentTop = 0;
                layoutViews();
            }
            if (refreshing) {
                status = ScrollStatus.REFRESHING;
            } else {
                status = ScrollStatus.IDLE;
            }
            tempStatus = status;
        }
    }

    public void setLoading(boolean loading) {
        setLoading(loading, true);
    }

    public void setLoading(boolean loading, boolean animation) {
        if (animation) {
            if (loading) {
                lastAnimState = true;
                if (dragHelper.smoothSlideViewTo(mTarget, 0, -dp2px(DRAG_MAX_DISTANCE))) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            } else {
                lastAnimState = true;
                if (dragHelper.smoothSlideViewTo(mTarget, 0, 0)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
            if (loading) {
                tempStatus = ScrollStatus.LOADING;
            } else {
                tempStatus = ScrollStatus.IDLE;
            }
        } else {
            if (loading) {
                contentTop = -dp2px(DRAG_MAX_DISTANCE);
                layoutViews();
            } else {
                contentTop = 0;
                layoutViews();
            }
            if (loading) {
                status = ScrollStatus.LOADING;
            } else {
                status = ScrollStatus.IDLE;
            }
            tempStatus = status;
        }
    }
}
