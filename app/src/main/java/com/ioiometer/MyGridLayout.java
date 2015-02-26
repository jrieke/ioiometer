package com.ioiometer;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

/**
 * Custom GridLayout that can stretch its rows and columns (with equal weights) as well as add horizontal and vertical spacing between its cells.
 *
 * Created by Johannes on 19.04.14.
 */
public class MyGridLayout extends android.support.v7.widget.GridLayout {

    private int lastMeasuredWidth = 0;
    private int lastMeasuredHeight = 0;

    private boolean stretchColumns = false;
    private boolean stretchRows = false;

    private int horizontalSpacing = 0;
    private int verticalSpacing = 0;

    private ViewTreeObserver.OnGlobalLayoutListener resizeListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {

            // If stretchColumns/stretchRows are true, adjust width/height of all children to fill up the entire available space.
            int measuredWidth = lastMeasuredWidth;
            int measuredHeight = lastMeasuredHeight;

            if (stretchColumns)
                measuredWidth = getMeasuredWidth();
            if (stretchRows)
                measuredHeight = getMeasuredHeight();

            if (measuredWidth != lastMeasuredWidth || measuredHeight != lastMeasuredHeight) {
                for (int i = 0; i < getChildCount(); i++) {
                    View child = getChildAt(i);
                    GridLayout.LayoutParams params = (GridLayout.LayoutParams) child.getLayoutParams();

                    // Apply spacing to child's margin if it does not represent an outer border of the GridLayout.
                    // Works only if each child spans one cell and its column and row numbers have not been set manually.
                    if (horizontalSpacing > 0) {
                        if (getOrientation() == HORIZONTAL) {
                            if (i % getColumnCount() != 0)  // Omit child on left border.
                                params.leftMargin = horizontalSpacing / 2;
                            if ((i + 1) % getColumnCount() != 0)  // Omit child on right border.
                                params.rightMargin = horizontalSpacing / 2;
                        } else {
                            if ((i / getRowCount()) % getColumnCount() != 0)  // Omit child on left border.
                                params.leftMargin = horizontalSpacing / 2;
                            if ((i / getRowCount() + 1) % getColumnCount() != 0)  // Omit child on right border.
                                params.rightMargin = horizontalSpacing / 2;
                        }
                    }
                    if (verticalSpacing > 0) {
                        if (getOrientation() == HORIZONTAL) {
                            if ((i / getColumnCount()) % getRowCount() != 0)  // Omit child on top border.
                                params.topMargin = verticalSpacing / 2;
                            if ((i / getColumnCount() + 1) % getRowCount() != 0)  // Omit child on bottom border.
                                params.bottomMargin = verticalSpacing / 2;
                        } else {
                            if (i % getRowCount() != 0)  // Omit child on top border.
                                params.topMargin = verticalSpacing / 2;
                            if ((i + 1) % getRowCount() != 0)  // Omit child on bottom border.
                                params.bottomMargin = verticalSpacing / 2;
                        }
                    }

                    if (stretchColumns)
                        params.width = (measuredWidth / getColumnCount()) - Math.max(0, params.rightMargin) - Math.max(0, params.leftMargin);
                    if (stretchRows)
                        params.height = (measuredHeight / getRowCount()) - Math.max(0, params.topMargin) - Math.max(0, params.bottomMargin);

                    child.setLayoutParams(params);
                }

                if (stretchColumns)
                    lastMeasuredWidth = measuredWidth;
                if (stretchRows)
                    lastMeasuredHeight = measuredHeight;
            }
        }
    };

    public MyGridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(attrs);
        attachResizeListener();
    }

    public MyGridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        attachResizeListener();
    }

    public MyGridLayout(Context context) {
        super(context);
        attachResizeListener();
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MyGridLayout);
        for (int i = 0; i < a.getIndexCount(); i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.MyGridLayout_strechColumns:
                    stretchColumns = a.getBoolean(attr, false);
                    break;
                case R.styleable.MyGridLayout_stretchRows:
                    stretchRows = a.getBoolean(attr, false);
                    break;
                case R.styleable.MyGridLayout_horizontalSpacing:
                    horizontalSpacing = a.getDimensionPixelSize(attr, 0);
                    break;
                case R.styleable.MyGridLayout_verticalSpacing:
                    verticalSpacing = a.getDimensionPixelSize(attr, 0);
                    break;
            }
        }
    }

    private void attachResizeListener() {
        // Apply resizing at layout time because the view dimensions have to be determined before.
        ViewTreeObserver viewTreeObserver = this.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(resizeListener);
        }
    }

    public int getVerticalSpacing() {
        return verticalSpacing;
    }

    public void setVerticalSpacing(int verticalSpacing) {
        this.verticalSpacing = verticalSpacing;
    }

    public boolean isStretchColumns() {
        return stretchColumns;
    }

    public void setStretchColumns(boolean stretchColumns) {
        this.stretchColumns = stretchColumns;
    }

    public boolean isStretchRows() {
        return stretchRows;
    }

    public void setStretchRows(boolean stretchRows) {
        this.stretchRows = stretchRows;
    }

    public int getHorizontalSpacing() {
        return horizontalSpacing;
    }

    public void setHorizontalSpacing(int horizontalSpacing) {
        this.horizontalSpacing = horizontalSpacing;
    }
}
