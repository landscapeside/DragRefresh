package com.landscape.dragrefreshview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by 1 on 2016/9/7.
 */
public class DragRefreshLayout extends FrameLayout {
    ViewDragHelper dragHelper;
    private GestureDetectorCompat gestureDetector;

    View mTarget = null, emptyView = null;
    ImageView refreshView, loadView;
    private int emptyId = 0, contentId = 0;
    private RingDrawable mRefreshDrawable, mLoadDrawable;

    private static final int DRAG_MAX_DISTANCE = 64;
    static final int DRAG_MAX_RANGE = 150;
    int contentTop = 0;
    int initY = 0, mActivePointerId = -1;
    ScrollStatus status = ScrollStatus.IDLE;
    Direction direction = Direction.STATIC;
    Direction dragDirection = Direction.STATIC;

    public DragRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        gestureDetector = new GestureDetectorCompat(context, new XScrollDetector());
        dragHelper = ViewDragHelper.create(this, 0.3f, dragHelperCallback);
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
        if (mTarget == null) {
            return false;
        }
        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                initY = (int) MotionEventUtil.getMotionEventY(event, mActivePointerId);
                dragHelper.shouldInterceptTouchEvent(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == -1) {
                    return false;
                }
                direction = Direction.getDirection(
                        (int) (MotionEventUtil.getMotionEventY(event, mActivePointerId) - initY));
                if (direction == Direction.DOWN) {
                    if (!ScrollStatus.isDragging(status) && !ScrollStatus.isRefreshing(status) && ScrollViewCompat.canScrollDown(mTarget)) {
                        if (ScrollStatus.isLoading(status)) {
                            return handleMotionEvent(event);
                        } else {
                            return false;
                        }
                    } else {
                        return handleMotionEvent(event);
                    }
                } else if (direction == Direction.UP) {
                    if (!ScrollStatus.isDragging(status) && !ScrollStatus.isLoading(status) && ScrollViewCompat.canScrollUp(mTarget)) {
                        if (ScrollStatus.isRefreshing(status)) {
                            return handleMotionEvent(event);
                        } else {
                            return false;
                        }
                    } else {
                        return handleMotionEvent(event);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                mActivePointerId = -1;
                dragDirection = Direction.STATIC;
                return false;
            case MotionEvent.ACTION_UP:
                mActivePointerId = -1;
                dragDirection = Direction.STATIC;
                if (ScrollStatus.isDragging(status)) {
                    return handleMotionEvent(event);
                } else {
                    return false;
                }

        }
        return ScrollStatus.isDragging(status);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == -1) {
                    return true;
                }
                direction = Direction.getDirection(
                        (int) (MotionEventUtil.getMotionEventY(event, mActivePointerId) - initY));
                if (dragDirection == Direction.STATIC) {
                    dragDirection = direction;
                }
                if (direction == Direction.DOWN) {
                    if (dragDirection != direction) {
                        if (contentTop < 0) {
//                                Log.i("dragLayout", "继续处理");
                            dragHelper.processTouchEvent(event);
                            break;
                        } else {
//                                Log.i("dragLayout", "防止折回");
                            setLoading(false);
                            event.setAction(MotionEvent.ACTION_CANCEL);
                            super.dispatchTouchEvent(event);
                            return true;
                        }
                    } else {
//                            Log.i("dragLayout", "DOWN同方向");
                        dragHelper.processTouchEvent(event);
                        break;
                    }
                } else if (direction == Direction.UP) {
                    if (dragDirection != direction) {
                        if (contentTop > 0) {
//                                Log.i("dragLayout", "继续处理");
                            dragHelper.processTouchEvent(event);
                            break;
                        } else {
//                                Log.i("dragLayout", "防止折回");
                            setRefreshing(false);
                            event.setAction(MotionEvent.ACTION_CANCEL);
                            super.dispatchTouchEvent(event);
                            return true;
                        }
                    } else {
//                            Log.i("dragLayout", "UP同方向");
                        dragHelper.processTouchEvent(event);
                        break;
                    }
                }
                break;
            default:
                dragHelper.processTouchEvent(event);
                break;
        }
        initY = (int) MotionEventUtil.getMotionEventY(event, mActivePointerId);
        return true;
    }

    private boolean handleMotionEvent(MotionEvent event) {
        if (!ScrollStatus.isDragging(status)) {
            MotionEvent cancelEvent = MotionEvent.obtain(event);
            cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
            mTarget.dispatchTouchEvent(cancelEvent);
        }
        return dragHelper.shouldInterceptTouchEvent(event) && gestureDetector.onTouchEvent(event);
    }

    class XScrollDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return Math.abs(dx) <= Math.abs(dy);
        }
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
            emptyView.layout(paddingLeft, paddingTop + contentTop, width - paddingRight, contentTop + height - paddingBottom);
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
            if (child == mTarget) {
                return true;
            }
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
            if (contentTop > DRAG_MAX_DISTANCE) {
                setRefreshing(true);
            } else if (contentTop < -DRAG_MAX_DISTANCE) {
                setLoading(true);
            } else if (contentTop > 0) {
                setRefreshing(false);
            } else {
                setLoading(false);
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (changedView == mTarget) {
                refreshView.offsetTopAndBottom(dy);
                loadView.offsetTopAndBottom(dy);
                contentTop = top;
                invalidate();
                if (contentTop == 0) {
                    status = ScrollStatus.IDLE;
                }
            }
        }
    };

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    @Override
    public void computeScroll() {
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
            mRefreshDrawable.invalidateSelf();
            mLoadDrawable.invalidateSelf();
        } else {
            if (ScrollStatus.isRefreshing(status)) {
                mRefreshDrawable.start();
            } else if (ScrollStatus.isLoading(status)) {
                mLoadDrawable.start();
            } else if (ScrollStatus.isIdle(status)) {
                mRefreshDrawable.stop();
                mLoadDrawable.stop();
            }
        }
    }

    public void setRefreshing(boolean refreshing) {
        setRefreshing(refreshing, true);
    }

    public void setRefreshing(boolean refreshing, boolean animation) {
        status = ScrollStatus.REFRESHING;
        if (animation) {
            if (refreshing) {
                if (dragHelper.smoothSlideViewTo(mTarget, 0, DRAG_MAX_DISTANCE)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            } else {
                if (dragHelper.smoothSlideViewTo(mTarget, 0, 0)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
        } else {
            if (refreshing) {
                contentTop = DRAG_MAX_DISTANCE;
                layoutViews();
            } else {
                contentTop = 0;
                layoutViews();
            }
        }
    }

    public void setLoading(boolean loading) {
        setLoading(loading, true);
    }

    public void setLoading(boolean loading, boolean animation) {
        status = ScrollStatus.LOADING;
        if (animation) {
            if (loading) {
                if (dragHelper.smoothSlideViewTo(mTarget, 0, -DRAG_MAX_DISTANCE)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            } else {
                if (dragHelper.smoothSlideViewTo(mTarget, 0, 0)) {
                    ViewCompat.postInvalidateOnAnimation(this);
                }
            }
        } else {
            if (loading) {
                contentTop = -DRAG_MAX_DISTANCE;
                layoutViews();
            } else {
                contentTop = 0;
                layoutViews();
            }
        }
    }
}
