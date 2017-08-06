package com.jwg.grunert.ajgsensor;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Werner-Jens Grunert on 4/21/2017.
 */
public class LockableViewPager extends android.support.v4.view.ViewPager {
    private boolean swipeable;

    public LockableViewPager(Context context) {
        super(context);
    }

    public LockableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.swipeable = true;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.swipeable || getCurrentItem() < 3) {
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.swipeable || getCurrentItem() < 3) {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }
}
