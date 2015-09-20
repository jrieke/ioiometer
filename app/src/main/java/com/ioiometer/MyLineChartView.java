package com.ioiometer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.AbstractChart;
import org.achartengine.chart.XYChart;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.tools.PanListener;
import org.achartengine.tools.ZoomEvent;
import org.achartengine.tools.ZoomListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

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

        // This is a little custom extension to make X-axis labels more user friendly
        //
        AbstractChart chart=plot.getChart();
        if(chart instanceof XYChart) {
            ((XYChart)chart).setXLabelRenderer(new XYChart.LabelRenderer() {
                private double range=0;

                @Override
                public void prepare(List<Double> xLabels) {
                    range = plotRenderer.getXAxisMax() - plotRenderer.getXAxisMin();
                }

                @Override
                public String getLabel(NumberFormat format, double value) {
                    String text="";

                    if(value==0)
                        return "0";

                    if(value<0) {
                        value=Math.abs(value);
                        text="-";
                    }

                    // This commented out block works fine, but the times it shows
                    // are not really suitable for this task. The user most likely
                    // wants to see how much time is between two markers on the chart
                    // and it is hard to see with labels like "5:33" vs "11:06".
                    //
                    /// int s=(int)(value % 60);
                    /// int m=(int)((value / 60) % 60);
                    /// int h=(int)((value / 3600) % 24);
                    /// int d=(int)(value / 86400);
                    ///
                    /// NumberFormat f0=new DecimalFormat("00");
                    /// NumberFormat f1=new DecimalFormat("#");
                    ///
                    /// if (d > 0)
                    ///     text+=d + "d+";
                    ///
                    /// if (h > 0 || d > 0)
                    ///     text+=f1.format(h)+":";
                    ///
                    /// if (m > 0 || h > 0 || d > 0)
                    ///    text+=(h>0 || d>0 ? f0 : f1).format(m);
                    ///
                    /// if (range < 600)
                    ///     text+=":"+(h>0 || d>0 || m>0 ? f0 : f1).format(s);

                    // For easier calculation of interval showing the labels as
                    // decimal values of one unit -- sec, min, or hour.
                    //
                    double divider;
                    String suffix;
                    if(range<60*10) {
                        divider=1;
                        suffix="s";
                    }
                    else if(range<3600*10) {
                        divider=60;
                        suffix="m";
                    }
                    else {
                        divider=3600;
                        suffix="h";
                    }

                    text+=(new DecimalFormat("#").format(value/divider))+suffix;

                    return text;
                }
            });
        }
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
