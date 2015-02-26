package com.ioiometer;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A View that shows the information of a single pin.
 * Consists of a header with the pin number and description and a chart that plots voltage over time.
 *
 * @author Johannes Rieke
 */
public class SinglePinPlotView extends LinearLayout implements PinView {

    private final static String D = "MyDebug@SinglePinPlotView";

    protected Pin pin = null;

    protected TextView textViewPinNumber;
    protected EditText editTextDescription;
    private MyLineChartView chart;

    private boolean collapseOnHide = true;
    protected boolean collapsed = false;
    private PinView.OnTimeRangeChangedListener onTimeRangeChangedListener = null;

    public SinglePinPlotView(Context context) {
        super(context);
        init();
    }

    public SinglePinPlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.single_pin_plot_view, this);

        textViewPinNumber = (TextView)findViewById(R.id.pin_number);
        editTextDescription = (EditText)findViewById(R.id.description);
        chart = (MyLineChartView)findViewById(R.id.chart);

        textViewPinNumber.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                pin.visible = !pin.visible;
                onPinMetaDataChanged();
            }
        });

        editTextDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (pin != null)
                    pin.description = String.valueOf(s);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        editTextDescription.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                // Hide keyboard when enter or done has been pressed.
                if (actionId == EditorInfo.IME_ACTION_DONE || (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });

        chart.setOnPlotRangeChangedListener(new MyLineChartView.OnPlotRangeChangedListener() {
            @Override
            public void onPlotRangeChanged(double xMin, double xMax, double yMin, double yMax) {
                // TODO: Shouldn't this take xMin, xMax?
                notifyTimeRangeChanged();
            }
        });
    }

    public boolean isCollapseOnHide() {
        return collapseOnHide;
    }

    public void setCollapseOnHide(boolean collapseOnHide) {
        this.collapseOnHide = collapseOnHide;
        onPinMetaDataChanged();
    }

    public Pin getPin() {
        return pin;
    }

    public MyLineChartView getChart() {
        return chart;
    }

    /**
     * Collapse or expand the plot.
     */
    public void setCollapsed(boolean collapsed) {
        if (collapsed) {
            chart.setVisibility(GONE);
            findViewById(R.id.separator).setVisibility(GONE);

        } else {
            chart.setVisibility(VISIBLE);
            findViewById(R.id.separator).setVisibility(VISIBLE);
            chart.repaintPlot();
        }
        invalidate();
        this.collapsed = collapsed;
    }

    @Override
    public void showPins(Pin[] pins) {
        if (pins.length > 0)
            this.pin = pins[0];
            textViewPinNumber.setText(String.valueOf(this.pin.number));
            onPinMetaDataChanged();
            onPinSeriesDataChanged();
    }

    @Override
    public void onPinMetaDataChanged() {
        editTextDescription.setText(pin.description);
        // Unique id in order to restore state.
        editTextDescription.setId(2000 + pin.number);

        if (pin.visible) {
            textViewPinNumber.setTextColor(pin.color);
            if (collapseOnHide)
                setCollapsed(false);
            chart.removeAllSeries();
            chart.addSeries(pin.series, pin.color);
            chart.repaintPlot();
        } else {
            textViewPinNumber.setTextColor(Color.LTGRAY);
            if (collapseOnHide)
                setCollapsed(true);
            chart.removeAllSeries();
        }
    }

    @Override
    public void onPinSeriesDataChanged() {
        repaintChart();
    }

    @Override
    public void onTimeRangeChanged(double min, double max) {
        setTimeRange(min, max);
        repaintChart();
    }

    public void setTimeRange(double min, double max) {
        chart.setXTo(min, max);
    }

    protected void repaintChart() {
        if (!collapsed)
            chart.repaintPlot();
    }

    public void setOnTimeRangeChangedListener(PinView.OnTimeRangeChangedListener listener) {
        onTimeRangeChangedListener = listener;
    }

    public void removeOnTimeRangeChangedListener() {
        onTimeRangeChangedListener = null;
    }

    private void notifyTimeRangeChanged() {
        if (onTimeRangeChangedListener != null)
            onTimeRangeChangedListener.onTimeRangeChanged(this, chart.getXMin(), chart.getXMax());
    }
}
