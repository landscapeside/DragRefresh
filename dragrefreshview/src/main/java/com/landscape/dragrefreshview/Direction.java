package com.landscape.dragrefreshview;

/**
 * Created by 1 on 2016/9/9.
 */
public enum Direction {
    STATIC,
    UP,
    DOWN;

    public static Direction getDirection(int diffY) {
        if (diffY > 0 ) {
            return DOWN;
        } else if (diffY < 0) {
            return UP;
        } else {
            return STATIC;
        }
    }
}
