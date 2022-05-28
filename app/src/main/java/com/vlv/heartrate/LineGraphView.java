package com.vlv.heartrate;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.graphics.Point;

import androidx.annotation.NonNull;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

/**
 * This class uses external library AChartEngine to show dynamic real time line graph for HR values
 */
class LineGraphView {
    //TimeSeries will hold the data in x,y format for single chart
    private TimeSeries series = new TimeSeries("Heart Rate");
    //XYMultipleSeriesDataset will contain all the TimeSeries
    private XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
    //XYMultipleSeriesRenderer will contain all XYSeriesRenderer and it can be used to set the properties of whole Graph
    private XYMultipleSeriesRenderer multiRenderer = new XYMultipleSeriesRenderer();
    private static LineGraphView instance = null;

    /**
     * Singleton implementation of LineGraphView class
     */
    @NonNull
    static synchronized LineGraphView getLineGraphView() {
        if (instance == null) {
            instance = new LineGraphView();
        }
        return instance;
    }

    /**
     * This constructor will set some properties of single chart and some properties of whole graph
     */
    private LineGraphView() {
        //add single line chart series
        dataSet.addSeries(series);

        //XYSeriesRenderer is used to set the properties like chart color, style of each point, etc. of single chart
        final XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
        //set line chart color to Black
        seriesRenderer.setColor(Color.BLACK);
        //set line chart style to square points
        seriesRenderer.setPointStyle(PointStyle.SQUARE);
        seriesRenderer.setFillPoints(true);

        final XYMultipleSeriesRenderer renderer = multiRenderer;
        //set whole graph background color to transparent color
        renderer.setBackgroundColor(Color.TRANSPARENT);
        renderer.setMargins(new int[] { 50, 65, 40, 5 }); // top, left, bottom, right
        renderer.setMarginsColor(Color.argb(0x00, 0x01, 0x01, 0x01));
        renderer.setAxesColor(Color.BLACK);
        renderer.setAxisTitleTextSize(24);
        renderer.setShowGrid(true);
        renderer.setGridColor(Color.LTGRAY);
        renderer.setLabelsColor(Color.BLACK);
        renderer.setYLabelsColor(0, Color.DKGRAY);
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setYLabelsPadding(4.0f);
        renderer.setXLabelsColor(Color.DKGRAY);
        renderer.setLabelsTextSize(20);
        renderer.setLegendTextSize(20);
        //Disable zoom
        renderer.setPanEnabled(false, false);
        renderer.setZoomEnabled(false, false);
        //set title to x-axis and y-axis
        renderer.setXTitle("    Time (seconds)");
        renderer.setYTitle("               BPM");
        renderer.addSeriesRenderer(seriesRenderer);
    }

    /**
     * return graph view to activity
     */
    GraphicalView getView(@NonNull final Context context) {
        return ChartFactory.getLineChartView(context, dataSet, multiRenderer);
    }

    /**
     * add new x,y value to chart
     */
    void addValue(@NonNull final Point p) {
        series.add(p.x, p.y);
    }

    /**
     * clear all previous values of chart
     */
    void clearGraph() {
        series.clear();
    }

}
