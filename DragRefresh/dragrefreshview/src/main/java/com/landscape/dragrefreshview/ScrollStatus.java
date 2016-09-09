package com.landscape.dragrefreshview;

/**
 * Created by 1 on 2016/9/9.
 */
public enum  ScrollStatus {
    IDLE,
    DRAGGING,
    REFRESHING,
    LOADING;

    public static boolean isRefreshing(ScrollStatus status) {
        return status == REFRESHING;
    }

    public static boolean isLoading(ScrollStatus status) {
        return status == LOADING;
    }

    public static boolean isDragging(ScrollStatus status) {
        return status == DRAGGING;
    }

    public static boolean isIdle(ScrollStatus status) {
        return status == IDLE;
    }
}
