package com.ioiometer;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

/**
 * Enhanced ScrollView that allows to
 *     - register a listener for scroll events
 *     - save and restore scroll position (e. g. at activity restart).
 * Unlike normal ScrollView, it does NOT scroll to the focused View at startup.
 *
 * Created by Johannes Rieke on 24.03.14.
 */
public class MyScrollView extends ScrollView implements View.OnTouchListener {

    private final static String D = "MyDebug@MyScrollView";
    OnScrollListener onScrollListener = null;

    public MyScrollView(Context context) {
        super(context);
        init();
    }

    public MyScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // Prevent from scrolling to focused View, see http://stackoverflow.com/questions/8100831/stop-scrollview-from-setting-focus-on-edittext
        setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.requestFocusFromTouch();
        return false;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        View child = getChildAt(0);
        if (child != null) {
//            Log.d(D, "store x: " + getScrollX() + " width: " + child.getWidth() + " y: " + getScrollY() + " height: " + child.getHeight());
            bundle.putDoubleArray("scrollPosition", new double[]{1. * getScrollX() / child.getMeasuredWidth(), 1. * getScrollY() / child.getMeasuredHeight()});
        }
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle)state;
            final double[] restoredScrollPosition = bundle.getDoubleArray("scrollPosition");
            if (restoredScrollPosition != null) {
                // Scroll to the restored scroll position after layout etc.
                post(new Runnable() {
                    @Override
                    public void run() {
                        View child = getChildAt(0);
//                        Log.d(D, "scrollview layout");
                        if (child != null) {
//                            Log.d(D, "restore x rel: " + restoredScrollPosition[0] + " width: " + child.getWidth() + " y rel:" + restoredScrollPosition[1] + " height: " + child.getHeight());
//                            Log.d(D, "scrolling to: " + (int) (restoredScrollPosition[0] * child.getWidth()) + ", " + (int) (restoredScrollPosition[1] * child.getHeight()));
                            scrollTo((int) (restoredScrollPosition[0] * child.getMeasuredWidth()), (int) (restoredScrollPosition[1] * child.getMeasuredHeight()));
                        }
                    }
                });
            }
            state = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(state);
    }


    public void setOnScrollListener(OnScrollListener listener) {
        onScrollListener = listener;
    }

    public void removeOnScrollListener() {
        onScrollListener = null;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (onScrollListener != null)
            onScrollListener.onScrollStateChanged(this);
    }

    public static interface OnScrollListener {
        // TODO: Add scrollState o. e. if needed.
        public void onScrollStateChanged(ScrollView view);
    }
}
