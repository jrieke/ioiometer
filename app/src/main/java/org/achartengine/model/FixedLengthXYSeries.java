package org.achartengine.model;

import java.io.Serializable;

/**
 * Created by Johannes on 15.04.14.
 */
public class FixedLengthXYSeries extends XYSeries {

    private int fixedLength = Integer.MAX_VALUE;

    public FixedLengthXYSeries(String title) {
        super(title);
    }

    public FixedLengthXYSeries(String title, int scaleNumber) {
        super(title, scaleNumber);
    }


    public int getFixedLength() {
        return fixedLength;
    }

    public void setFixedLength(int fixedLength) {
        if (fixedLength <= 0)
            this.fixedLength = Integer.MAX_VALUE;
        else
            this.fixedLength = fixedLength;

        while (mXY.size() > fixedLength)
            removeFirst();
    }


    /**
     * Removes the first value from the series.
     * Useful for sliding, realtime graphs where a standard remove takes up too much time.
     * It assumes data is sorted on the key value (X component).
     *
     * See http://stackoverflow.com/questions/14187716/is-achartengine-ready-for-realtime-graphing
     */
    private synchronized void removeFirst() {
        mXY.removeByIndex(0);
        mMinX = mXY.getXByIndex(0);
    }

    @Override
    public synchronized void add(int index, double x, double y) {
        super.add(index, x, y);
        if (mXY.size() > fixedLength)
            removeFirst();
    }

    @Override
    public synchronized void add(double x, double y) {
        super.add(x, y);
        if (mXY.size() > fixedLength)
            removeFirst();
    }

}
