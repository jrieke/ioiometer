package com.ioiometer;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import org.achartengine.model.FixedLengthXYSeries;

/**
 * Created by Johannes Rieke on 22.03.14.
 */
public class Pin implements Parcelable {

    private static final String D = "MyDebug@Pin";

    public int number;
    public boolean visible;
    public String description;
    public int color;
    public FixedLengthXYSeries series;

    public Pin(int number, boolean visible, String description, int color) {
        this.number = number;
        this.visible = visible;
        this.description = description;
        this.color = color;
        series = new FixedLengthXYSeries(String.valueOf(number));
    }

    public Pin(int number, int color) {
        this(number, true, "", color);
    }

    public Pin(int number) {
        this(number, Color.RED);
    }

    public Pin(Parcel in) {
        number = in.readInt();
        visible = (in.readInt() == 1);
        description = in.readString();
        color = in.readInt();
        series = (FixedLengthXYSeries)in.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(number);
        out.writeInt((visible) ? 1 : 0);
        out.writeString(description);
        out.writeInt(color);
        out.writeSerializable(series);
    }

    public void addPointToSeries(double time, double voltage) {
        series.add(time, voltage);
    }

    public void clearSeries() {
        series.clear();
    }

    public boolean equals(Pin pin) {
        return pin.number == number;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Pin createFromParcel(Parcel in) {
            return new Pin(in);
        }

        @Override
        public Pin[] newArray(int size) {
            return new Pin[size];
        }
    };

    public int getNumSteps() {
       return series.getItemCount();
    }

    public double getTime(int step) {
        return series.getX(step);
    }

    public double getVoltage(int step) {
        return series.getY(step);
    }

    public void setMaxDatapoints(int numDatapoints) {
        this.series.setFixedLength(numDatapoints);
    }
}
