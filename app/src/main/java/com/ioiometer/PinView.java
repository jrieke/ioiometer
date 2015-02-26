package com.ioiometer;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import org.achartengine.model.XYSeries;

/**
 * Interface for views that represent an array of pins.
 * @author Johannes Rieke
 */
public interface PinView {

    /**
     * Register an array of pins to be represented by this view.
     * @param pins The pins to show
     */
    public void showPins(Pin[] pins);

    /**
     * Inform the view that the metadata (description, color, visibility) of at least one pin has changed.
     */
    public void onPinMetaDataChanged();

    /**
     * Inform the view that the series of at least one pin has changed (e. g. points were added).
     */
    public void onPinSeriesDataChanged();

    /**
     * Inform the view that the visible time range has changed.
     * @param min The new minimum time
     * @param max The new maximum time
     */
    public void onTimeRangeChanged(double min, double max);

    /**
     * Register an OnTimeRangeChangedListener instance that needs to be triggered when this PinView instance initiates a time range change.
     * @param listener The listener to register
     */
    public void setOnTimeRangeChangedListener(OnTimeRangeChangedListener listener);

    /**
     * Remove a registered OnTimeRangeChangedListener instance.
     */
    public void removeOnTimeRangeChangedListener();


    interface OnTimeRangeChangedListener {

        /**
         * Called when the time range has changed.
         * @param source The PinView that initiated the time range change
         * @param min The new minimum time
         * @param max The new maximum time
         */
        public void onTimeRangeChanged(PinView source, double min, double max);
    }

}
