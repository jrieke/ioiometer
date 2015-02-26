package com.ioiometer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

/**
 *
 *
 * @author Johannes Rieke
 */
public class MyLineChartView extends FrameLayout {

    private final static String D = "MyDebug@MyLineChartView";

    protected GraphicalView plot;
    protected XYMultipleSeriesRenderer plotRenderer;
    protected XYMultipleSeriesDataset dataset;

    protected OnPlotRangeChangedListener onPlotRangeChangedListener = null;

    public MyLineChartView(Context context) {
        super(context);
        init();
    }

    public MyLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dataset = new XYMultipleSeriesDataset();

        // Default plot style.
        plotRenderer = new XYMultipleSeriesRenderer();
        plotRenderer.setAxesColor(Color.BLACK);
        plotRenderer.setMargins(new int[] {(int)Helper.dpToPx(6.7f), (int)Helper.dpToPx(16.7f), (int)Helper.dpToPx(-5f), (int)Helper.dpToPx(10)});  // top, left, bottom, right
        plotRenderer.setMarginsColor(Color.argb(0, 255, 255, 255));
        plotRenderer.setLabelsTextSize(Helper.dpToPx(13.3f));
        plotRenderer.setLabelsColor(Color.BLACK);
        plotRenderer.setXLabelsColor(Color.BLACK);
        plotRenderer.setYLabelsColor(0, Color.BLACK);
        plotRenderer.setYLabelsAlign(Paint.Align.RIGHT);
        plotRenderer.setYLabelsPadding(Helper.dpToPx(7f));
        plotRenderer.setYLabelsVerticalPadding(Helper.dpToPx(-4.8f));

        plotRenderer.setYAxisMin(-0.1);
        plotRenderer.setYAxisMax(3.5);
        plotRenderer.setXAxisMin(0);
        plotRenderer.setXAxisMax(10);

        plotRenderer.setPanEnabled(true, false);
        plotRenderer.setZoomEnabled(true, false);
        plotRenderer.setShowLegend(false);

        plot = ChartFactory.getLineChartView(getContext(), dataset, plotRenderer);
        addView(plot);

        // Wihtout a series the axes and labels do not show up.
        addDummySeries();

        plot.repaint();

        plot.addPanListener(new PanListener() {
            @Override
            public void panApplied() {
                notifyPlotRangeChanged();
            }
        });
        plot.addZoomListener(new ZoomListener() {
            @Override
            public void zoomApplied(ZoomEvent zoomEvent) {
                notifyPlotRangeChanged();
            }

            @Override
            public void zoomReset() {
                notifyPlotRangeChanged();
            }
        }, true, true);
    }

    /**
     * Add a graph for the data in 'series' with 'color' to the plot.
     * @param series The x and y values to add
     * @param color Used for plotting
     */
    public void addSeries(XYSeries series, int color) {
        dataset.addSeries(series);
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        renderer.setColor(color);
        plotRenderer.addSeriesRenderer(renderer);
    }

    /**
     * Equal to addSeries(series, Color.RED)
     */
    public void addSeries(XYSeries series) {
        addSeries(series, Color.RED);
    }

    /**
     * Add a dummy series with a single, transparent point in the origin to the plot.
     */
    protected void addDummySeries() {
        XYSeries series = new XYSeries("dummy");
        series.add(0, 0);
        dataset.addSeries(series);
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        renderer.setColor(Color.TRANSPARENT);
        plotRenderer.addSeriesRenderer(renderer);
    }

    /**
     * Remove 'series' and its graph from the plot.
     * @param series The series to remove
     */
    public void removeSeries(XYSeries series) {
        int index = 0;
        int count = dataset.getSeriesCount();
        while (index < count && dataset.getSeriesAt(index) != series)
            index++;

        if (index < count) {
            dataset.removeSeries(index);
            plotRenderer.removeSeriesRenderer(plotRenderer.getSeriesRendererAt(index));
        }
    }

    /**
     * Remove all series and their graphs from the plot.
     */
    public void removeAllSeries() {
        dataset.clear();
        plotRenderer.removeAllRenderers();
        addDummySeries();
    }

    // TODO: Sort out which methods are actually needed/useful.
    public void setXTo(double xMin, double xMax) {
        plotRenderer.setXAxisMin(xMin);
        plotRenderer.setXAxisMax(xMax);
    }

    public void setXTo(double xMin) {
        setXTo(xMin, xMin + getXRange());
    }

    protected void centerXOn(double x) {
        setXTo(x - getXRange() / 2);
    }

    public double getXMin() {
        return plotRenderer.getXAxisMin();
    }
    public double getXMax() {
        return plotRenderer.getXAxisMax();
    }
    public double getYMin() {
        return plotRenderer.getYAxisMin();
    }
    public double getYMax() {
        return plotRenderer.getYAxisMax();
    }


    public void setXRange(double xRange) {
        plotRenderer.setXAxisMax(plotRenderer.getXAxisMin() + xRange);
    }

    public double getXRange() {
        return plotRenderer.getXAxisMax() - plotRenderer.getXAxisMin();
    }

    public void repaintPlot() {
        plot.repaint();
    }



    public void setOnPlotRangeChangedListener(OnPlotRangeChangedListener listener) {
        onPlotRangeChangedListener = listener;
    }

    protected void notifyPlotRangeChanged() {
        if (onPlotRangeChangedListener != null)
            onPlotRangeChangedListener.onPlotRangeChanged(getXMin(), getXMax(), getYMin(), getYMax());
    }

    interface OnPlotRangeChangedListener {
        public void onPlotRangeChanged(double xMin, double xMax, double yMin, double yMax);
    }
}
