package com.ioiometer;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * MyLineChartView instance that displays the data of multiple pins.
 * The header consists of the pin numbers, the plot shows the respective voltage time series of the pins.
 *
 * Created by Johannes Rieke on 23.03.14.
 */
public class MultiPinPlotView extends LinearLayout implements PinView {

    private final static String D = "MyDebug@MultiPinPlotView";

    protected Pin[] pins = null;

    protected MyLineChartView chart;
    protected LinearLayout layoutPinNumbers;
    protected TextView textViewAll;
    protected TextView[] textViewsPinNumbers;
    protected boolean allPinsEnabled;
    protected int pinNumbersHorizontalPadding = (int)Helper.dpToPx(6.7f);
    protected int pinNumbersVerticalPadding = (int)Helper.dpToPx(6.7f);

    private OnTimeRangeChangedListener onTimeRangeChangedListener = null;

    public MultiPinPlotView(Context context) {
        super(context);
        initChild();
    }

    public MultiPinPlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initChild();
    }

    private void initChild(){
        inflate(getContext(), R.layout.multi_pin_plot_view, this);

        layoutPinNumbers = (LinearLayout)findViewById(R.id.pin_numbers_layout);
        chart = (MyLineChartView)findViewById(R.id.chart);

        chart.setOnPlotRangeChangedListener(new MyLineChartView.OnPlotRangeChangedListener() {
            @Override
            public void onPlotRangeChanged(double xMin, double xMax, double yMin, double yMax) {

            }
        });
    }

    protected void togglePinVisible(int index) {
        pins[index].visible = !pins[index].visible;
        onPinMetaDataChanged();
    }

    protected void toggleAllPinsVisible() {
        allPinsEnabled = !allPinsEnabled;
        for (Pin p : pins)
            p.visible = allPinsEnabled;
        onPinMetaDataChanged();
    }

    /**
     * Set the pins to display and initialize the view accordingly.
     */
    @Override
    public void showPins(Pin[] pins) {
        chart.removeAllSeries();
        layoutPinNumbers.removeAllViews();

        this.pins = pins;

        textViewAll = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.pin_number, layoutPinNumbers, false);
        textViewAll.setText("All");
        textViewAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAllPinsVisible();
            }
        });
        textViewAll.setPadding(0, pinNumbersVerticalPadding, pinNumbersHorizontalPadding, pinNumbersVerticalPadding);
        layoutPinNumbers.addView(textViewAll);

        textViewsPinNumbers = new TextView[pins.length];
        for (int i = 0; i < pins.length; i++) {
            chart.addSeries(pins[i].series, pins[i].color);

            // Add TextView with pin number to header.
            TextView textViewPinNumber = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.pin_number, layoutPinNumbers, false);
            textViewPinNumber.setText(String.valueOf(pins[i].number));

            final int n = i;
            textViewPinNumber.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    togglePinVisible(n);
                }
            });
            if (i == pins.length-1)
                textViewPinNumber.setPadding(pinNumbersHorizontalPadding, pinNumbersVerticalPadding, 0, pinNumbersVerticalPadding);
            else
                textViewPinNumber.setPadding(pinNumbersHorizontalPadding, pinNumbersVerticalPadding, pinNumbersHorizontalPadding, pinNumbersVerticalPadding);
            layoutPinNumbers.addView(textViewPinNumber);
            textViewsPinNumbers[i] = textViewPinNumber;
        }
        onPinMetaDataChanged();
    }

    public Pin[] getPins() {
        return pins;
    }

    @Override
    public void onPinMetaDataChanged() {
        allPinsEnabled = true;
        chart.removeAllSeries();

        for (int i = 0; i < pins.length; i++) {
            if (pins[i].visible) {
                textViewsPinNumbers[i].setTextColor(pins[i].color);
                chart.addSeries(pins[i].series, pins[i].color);
            } else {
                textViewsPinNumbers[i].setTextColor(Color.LTGRAY);
                allPinsEnabled = false;
            }
        }

        textViewAll.setTextColor( (allPinsEnabled) ? Color.BLACK : Color.LTGRAY);
        chart.repaintPlot();
    }

    @Override
    public void onPinSeriesDataChanged() {
        chart.repaintPlot();
    }

    @Override
    public void onTimeRangeChanged(double min, double max) {
        chart.setXTo(min, max);
        chart.repaintPlot();
    }

    /**
     * Trigger a registered OnTimeRangeChangedListener.
     */
    private void notifyTimeRangeChanged() {
        if (onTimeRangeChangedListener != null)
            onTimeRangeChangedListener.onTimeRangeChanged(this, chart.getXMin(), chart.getXMax());
    }

    @Override
    public void setOnTimeRangeChangedListener(OnTimeRangeChangedListener listener) {
        this.onTimeRangeChangedListener = listener;
    }

    @Override
    public void removeOnTimeRangeChangedListener() {
        this.onTimeRangeChangedListener = null;
    }
}
