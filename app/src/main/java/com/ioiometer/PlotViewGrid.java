package com.ioiometer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created by Johannes Rieke on 21.03.14.
 */
public class PlotViewGrid extends MyGridLayout implements PinView {

    private static final String D = "MyDebug@PlotViewGrid";

    protected Pin[] pins;
    private boolean[] toUpdate;
    private boolean collapseOnHide = true;
    private OnTimeRangeChangedListener onTimeRangeChangedListener = null;

    public PlotViewGrid(Context context) {
        super(context);
        init();
    }

    public PlotViewGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        init();
    }

    public PlotViewGrid(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        init();
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PlotViewGrid);
        for (int i = 0; i < a.getIndexCount(); i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.PlotViewGrid_collapseOnHide:
                    collapseOnHide = a.getBoolean(attr, true);
                    break;
            }
        }
    }

    private void init() {
        pins = new Pin[0];
        toUpdate = new boolean[0];
        post(new Runnable() {
            @Override
            public void run() {
                // TODO: Maybe change to setInScroll method.
                if (getParent() instanceof MyScrollView) {
                    ((MyScrollView)getParent()).setOnScrollListener(new MyScrollView.OnScrollListener() {

                        /**
                         * Update plots that come on screen while scrolling if they were not updated during the last call to 'onPinMetaDataChanged'.
                         * @param view
                         */
                        @Override
                        public void onScrollStateChanged(ScrollView view) {
                            for (int i = 0; i < getChildCount(); i++) {
                                SinglePinPlotView plotView = (SinglePinPlotView) getChildAt(i);
                                if (toUpdate[i] && isPlotVisibleOnScreen(plotView)) {
                                    plotView.onPinSeriesDataChanged();
                                    toUpdate[i] = false;
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void showPins(Pin[] pins) {
        this.removeAllViews();
        this.pins = pins;
        toUpdate = new boolean[pins.length];
        for (int i = 0; i < pins.length; i++) {
            toUpdate[i] = true;
            final SinglePinPlotView plotView = new SinglePinPlotView(getContext());
            // Unique id in order to restore state.
            plotView.setId(3000 + pins[i].number);
            plotView.showPins(new Pin[] {pins[i]});
            plotView.setCollapseOnHide(collapseOnHide);

            plotView.setOnTimeRangeChangedListener(new OnTimeRangeChangedListener() {
                @Override
                public void onTimeRangeChanged(PinView source, double min, double max) {
                    synchronizeTimeRange((SinglePinPlotView)source, min, max);
                    notifyTimeRangeChanged(min, max);
                }
            });

            addView(plotView);
        }
        onPinMetaDataChanged();
    }

    @Override
    public void onPinMetaDataChanged() {
        for (int i = 0; i < getChildCount(); i++)
            ((SinglePinPlotView)getChildAt(i)).onPinMetaDataChanged();
    }

    @Override
    public void onPinSeriesDataChanged() {
        repaintPlots();
    }

    @Override
    public void onTimeRangeChanged(double min, double max) {
        setTimeRange(min, max);
        repaintPlots();
    }

    protected void setTimeRange(double min, double max) {
        for (int i = 0; i < getChildCount(); i++) {
            ((SinglePinPlotView)getChildAt(i)).setTimeRange(min, max);
        }
    }

    private void synchronizeTimeRange(SinglePinPlotView leadingChild, double min, double max) {
        for (int i = 0; i < getChildCount(); i++) {
            SinglePinPlotView plotView = (SinglePinPlotView) getChildAt(i);
            if (plotView != leadingChild) {
                plotView.setTimeRange(min, max);
            }
        }
        repaintPlots();
    }



    protected void repaintPlots() {
        // Only update plots that are currently visible.
        for (int i = 0; i < getChildCount(); i++) {
            SinglePinPlotView plotView = (SinglePinPlotView) getChildAt(i);
            if (isPlotVisibleOnScreen(plotView)) {
                plotView.onPinSeriesDataChanged();
                toUpdate[i] = false;
            }
            else {
                toUpdate[i] = true;
            }
        }
    }

    private boolean isPlotVisibleOnScreen(SinglePinPlotView child) {
        if (getParent() instanceof ScrollView) {
            Rect scrollBounds = new Rect();
            ((ScrollView)getParent()).getHitRect(scrollBounds);
            return child.getChart().getLocalVisibleRect(scrollBounds);
        }
        else
            return true;
    }

    private boolean isPlotVisibleOnScreen(int childIndex) {
        return isPlotVisibleOnScreen((SinglePinPlotView)getChildAt(childIndex));
    }


    private void notifyTimeRangeChanged(double min, double max) {
        if (onTimeRangeChangedListener != null)
            onTimeRangeChangedListener.onTimeRangeChanged(this, min, max);
    }

    @Override
    public void setOnTimeRangeChangedListener(OnTimeRangeChangedListener listener) {
        onTimeRangeChangedListener = listener;
    }

    @Override
    public void removeOnTimeRangeChangedListener() {
        onTimeRangeChangedListener = null;
    }
}
