package com.landscape.dragrefreshview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
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
    ScrollStatus status = ScrollStatus.IDLE, scrollStatus = ScrollStatus.IDLE;
    DragDelegate dragDelegate = null;
    DragRefreshListener refreshListener = null;
    DragLoadListener loadListener = null;
    Direction smoothToDirection = Direction.STATIC;
    boolean shouldCancel = false;

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
    public void setDrawPercent(float drawPercent) {
        if (mRefreshDrawable.isRunning()) {
            lastAnimState = false;
            mRefreshDrawable.stop();
        }
        if (mLoadDrawable.isRunning()) {
            lastAnimState = false;
            mLoadDrawable.stop();
        }
        mRefreshDrawable.setPercent(drawPercent);
        mLoadDrawable.setPercent(drawPercent);
        mRefreshDrawable.invalidateSelf();
        mLoadDrawable.invalidateSelf();
    }

    @Override
    public boolean isRefreshAble() {
        return refreshListener != null;
    }

    @Override
    public boolean isLoadAble() {
        return loadListener != null;
    }

    @Override
    public void beforeMove() {
        shouldCancel = ScrollStatus.isRefreshing(status) || ScrollStatus.isLoading(status);
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
        refreshView.setPadding(0, dp2px(DRAW_PADDING), 0, dp2px(DRAW_PADDING));
        loadView.setPadding(0, dp2px(DRAW_PADDING), 0, dp2px(DRAW_PADDING));

        initDrawable();
        refreshView.setImageDrawable(mRefreshDrawable);
        loadView.setImageDrawable(mLoadDrawable);
        refreshView.setBackgroundColor(Color.BLUE);
        loadView.setBackgroundColor(Color.BLACK);

        addView(refreshView);
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
            emptyView.setClickable(true);
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
            emptyView.layout(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);
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
            return child == mTarget
                    || (child == emptyView && emptyView.isShown())
                    || child == refreshView
                    || child == loadView;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (child == mTarget || (child == emptyView && emptyView.isShown())) {
                status = ScrollStatus.DRAGGING;
                if (contentTop + dy > DRAG_MAX_RANGE) {
                    return DRAG_MAX_RANGE;
                } else if (contentTop + dy < -DRAG_MAX_RANGE) {
                    return -DRAG_MAX_RANGE;
                } else {
                    return top;
                }
            } else {
                status = ScrollStatus.DRAGGING;
                if (contentTop + dy > DRAG_MAX_RANGE) {
                    return DRAG_MAX_RANGE - refreshView.getMeasuredHeight();
                } else if (contentTop + dy < -DRAG_MAX_RANGE) {
                    return getMeasuredHeight() - getPaddingBottom() - DRAG_MAX_RANGE;
                } else {
                    return top;
                }
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
                shouldCancel = false;
                setRefreshing(true);
            } else if (contentTop < -dp2px(DRAG_MAX_DISTANCE)) {
                shouldCancel = false;
                setLoading(true);
            } else if (contentTop > 0) {
                setRefreshing(false);
            } else if (contentTop == 0) {
                if (!ScrollViewCompat.canSmoothDown(mTarget)) {
                    setRefreshing(false);
                } else if (!ScrollViewCompat.canSmoothUp(mTarget)) {
                    setLoading(false);
                }
            } else {
                setLoading(false);
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (changedView == mTarget) {
                if (!ScrollViewCompat.canSmoothDown(mTarget)
                        && top < 0) {
                    contentTop = 0;
                    layoutViews();
                } else if (ScrollViewCompat.canSmoothDown(mTarget)
                        && !ScrollViewCompat.canSmoothUp(mTarget)
                        && top > 0) {
                    contentTop = 0;
                    layoutViews();
                } else {
                    refreshView.offsetTopAndBottom(dy);
                    loadView.offsetTopAndBottom(dy);
                    contentTop = top;
                    layoutViews();
                    invalidate();
                }
            } else if (changedView == emptyView && emptyView.isShown()) {
                refreshView.offsetTopAndBottom(dy);
                loadView.offsetTopAndBottom(dy);
                contentTop = top;
                invalidate();
            } else {
                if (!ScrollViewCompat.canSmoothDown(mTarget)
                        && (top + refreshView.getMeasuredHeight() - getPaddingTop()) < 0) {
                    contentTop = 0;
                    layoutViews();
                } else if (ScrollViewCompat.canSmoothDown(mTarget)
                        && !ScrollViewCompat.canSmoothUp(mTarget)
                        && (top - getMeasuredHeight() + getPaddingBottom()) > 0) {
                    contentTop = 0;
                    layoutViews();
                } else {
                    contentTop += dy;
                    layoutViews();
                    invalidate();
                }
            }
        }
    };

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    boolean lastAnimState = true, animContinue = true;

    @Override
    public void computeScroll() {
        animContinue = dragHelper.continueSettling(true);
        if (animContinue && lastAnimState == animContinue) {
            ViewCompat.postInvalidateOnAnimation(this);
            mRefreshDrawable.invalidateSelf();
            mLoadDrawable.invalidateSelf();
        } else if (!animContinue && lastAnimState != animContinue) {
            if (ScrollStatus.isRefreshing(scrollStatus)) {
                mRefreshDrawable.start();
                if (isRefreshAble()) {
                    refreshListener.onRefresh();
                }
            } else if (ScrollStatus.isLoading(scrollStatus)) {
                mLoadDrawable.start();
                if (isLoadAble()) {
                    loadListener.onLoad();
                }
            } else if (ScrollStatus.isIdle(scrollStatus)) {
                mRefreshDrawable.stop();
                mLoadDrawable.stop();
                if (smoothToDirection == Direction.UP && isRefreshAble() && shouldCancel) {
                    refreshListener.refreshCancel();
                }
                if (smoothToDirection == Direction.DOWN && isLoadAble() && shouldCancel) {
                    loadListener.loadCancel();
                }
                shouldCancel = false;
            }
            status = scrollStatus;
            lastAnimState = animContinue;
            smoothToDirection = Direction.STATIC;
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
                    scrollStatus = ScrollStatus.REFRESHING;
                } else {
                    status = ScrollStatus.REFRESHING;
                    scrollStatus = status;
                    if (isRefreshAble()) {
                        refreshListener.onRefresh();
                    }
                }
            } else {
                lastAnimState = true;
                if (dragHelper.smoothSlideViewTo(mTarget, 0, 0)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                    scrollStatus = ScrollStatus.IDLE;
                    smoothToDirection = Direction.UP;
                } else {
                    if (ScrollStatus.isRefreshing(scrollStatus) && isRefreshAble()) {
                        refreshListener.refreshCancel();
                    }
                    status = ScrollStatus.IDLE;
                    scrollStatus = status;
                }
            }
        } else {
            if (refreshing) {
                contentTop = dp2px(DRAG_MAX_DISTANCE);
                layoutViews();
                status = ScrollStatus.REFRESHING;
                if (isRefreshAble()) {
                    refreshListener.onRefresh();
                }
            } else {
                contentTop = 0;
                layoutViews();
                if (!ScrollStatus.isIdle(status) && isRefreshAble()) {
                    refreshListener.refreshCancel();
                }
                status = ScrollStatus.IDLE;
            }
            scrollStatus = status;
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
                    scrollStatus = ScrollStatus.LOADING;
                } else {
                    status = ScrollStatus.LOADING;
                    scrollStatus = status;
                    if (isLoadAble()) {
                        loadListener.onLoad();
                    }
                }
            } else {
                lastAnimState = true;
                if (dragHelper.smoothSlideViewTo(mTarget, 0, 0)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                    scrollStatus = ScrollStatus.IDLE;
                    smoothToDirection = Direction.DOWN;
                } else {
                    if (ScrollStatus.isLoading(scrollStatus) && isLoadAble()) {
                        loadListener.loadCancel();
                    }
                    status = ScrollStatus.IDLE;
                    scrollStatus = status;
                }
            }

        } else {
            if (loading) {
                contentTop = -dp2px(DRAG_MAX_DISTANCE);
                layoutViews();
                status = ScrollStatus.LOADING;
                if (isLoadAble()) {
                    loadListener.onLoad();
                }
            } else {
                contentTop = 0;
                layoutViews();
                if (!ScrollStatus.isIdle(status) && isLoadAble()) {
                    loadListener.loadCancel();
                }
                status = ScrollStatus.IDLE;
            }
            scrollStatus = status;
        }
    }

    public void setRefreshListener(DragRefreshListener refreshListener) {
        this.refreshListener = refreshListener;
    }

    public void setLoadListener(DragLoadListener loadListener) {
        this.loadListener = loadListener;
    }
}
