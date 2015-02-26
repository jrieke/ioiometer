package com.ioiometer;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;


/**
 * Main Activity that is invoked when the app starts.
 *
 * @author Johannes Rieke
 */
public class MainActivity extends IOIOActivity implements PinView.OnTimeRangeChangedListener {

    private final static String D = "MyDebug@MainActivity";

    private Handler handler;
    private SharedPreferences preferences;
    private int[] allColors;  // Loaded in onCreate from resource file.
    private int colorIterator = 0;

    private boolean isFirstRun;
    private boolean paused = true;
    private boolean pausedBefore = true;
    private boolean ioioConnected = false;

    public final static int FIRST_PIN_NUMBER = 31;
    public final static int LAST_PIN_NUMBER = 42;
    protected final int numPins = LAST_PIN_NUMBER - FIRST_PIN_NUMBER + 1;
    protected Pin[] pins;

    protected double time = 0;  // s
    private long lastMeasuredTime = -1;  // ns

    private double timeRangeMin;
    private double timeRangeMax;
    private final static double TIME_RANGE_DEFAULT = 5;

    private View settings;
    private boolean settingsVisible = true;

    private int measurementInterval;  // ms
    private final static int MIN_MEASUREMENT_INTERVAL = 5;  // ms
    private final static int MAX_MEASUREMENT_INTERVAL = 200;  // ms
    private final static int DEFAULT_MEASUREMENT_INTERVAL = 50;  // ms

    private int numDatapoints;
    private final static int MIN_NUM_DATAPOINTS = 1000;
    private final static int MAX_NUM_DATAPOINTS = 10000;
    private final static int DEFAULT_NUM_DATAPOINTS = 6000;

    private PinView currentView;
    private int viewMode = -1;
    public final static int VIEW_MODE_SINGLE = 1;
    public final static int VIEW_MODE_GRID = 2;

    private MenuItem menuItemStartPause;
    private MenuItem menuItemShowSettings;

    private ProgressBar mainProgressBar;

    // A separate thread which steadily updates the graphs in the current view if not paused.
    Thread uiThread = new Thread() {
        @Override
        public void run() {
            while (true) {

                // Loop here while paused.
                while (paused) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                currentView.onPinSeriesDataChanged();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getPreferences(MODE_PRIVATE);

        // Show overlay explaining axes quantities on first run.
        isFirstRun = preferences.getBoolean("is_first_run", true);
        if (isFirstRun)
            preferences.edit().putBoolean("is_first_run", false).commit();

        handler = new Handler();
        allColors = getResources().getIntArray(R.array.plot_colors);

        pins = new Pin[numPins];
        Parcelable[] restoredPins;
        if (savedInstanceState == null)
            restoredPins = null;
        else
            restoredPins = savedInstanceState.getParcelableArray("pins");

        for (int i = 0; i < numPins; i++) {
            if (restoredPins != null && restoredPins[i] != null) {  // Restore from savedInstanceState.
                pins[i] = (Pin) restoredPins[i];
            } else {
                int pinNumber = FIRST_PIN_NUMBER + i;
                String pinKey = String.valueOf(pinNumber);

                if (preferences.getBoolean(pinKey + "_saved", false)) {  // Restore from preferences.
                    pins[i] = new Pin(pinNumber, preferences.getBoolean(pinKey + "_enabled", true), preferences.getString(pinKey + "_description", ""), preferences.getInt(pinKey + "_color", nextColor()));
                } else {  // Create new.
                    pins[i] = new Pin(pinNumber, nextColor());
                }
            }
        }

        if (savedInstanceState != null) {
            timeRangeMin = savedInstanceState.getDouble("time_range_min", 0);
            time = savedInstanceState.getDouble("time", 0);
        }
        else {
            timeRangeMin = 0;
        }

        timeRangeMax = timeRangeMin + Double.longBitsToDouble(preferences.getLong("time_range", Double.doubleToLongBits(TIME_RANGE_DEFAULT)));
        settingsVisible = preferences.getBoolean("settings_visible", true);
        measurementInterval = preferences.getInt("measurement_interval", DEFAULT_MEASUREMENT_INTERVAL);
        numDatapoints = preferences.getInt("num_datapoints", DEFAULT_NUM_DATAPOINTS);
        setMaxDatapoints(numDatapoints);
        viewMode = preferences.getInt("view_mode", VIEW_MODE_SINGLE);

        initUi();
        uiThread.start();


        /**
         *  Create progress bar below the action bar.
         *  See http://stackoverflow.com/questions/13934010/progressbar-under-action-bar
         */
        mainProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        mainProgressBar.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, 24));
        mainProgressBar.setProgress(65);
        mainProgressBar.setVisibility(View.INVISIBLE);
        final FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(mainProgressBar);
        ViewTreeObserver observer = mainProgressBar.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Position the ProgressBar by looking at the start position of the content area.
                // Note: mainProgressBar.setY(136) will not work because of different screen densities and different sizes of the action bar.
                View contentView = decorView.findViewById(android.R.id.content);
                // TODO: Disable this for Android < 11
                mainProgressBar.setY(contentView.getY() - 10);

                ViewTreeObserver observer = mainProgressBar.getViewTreeObserver();
                observer.removeGlobalOnLayoutListener(this);
            }
        });
    }

    /**
     * Initialize the content of all UI elements according to the state of the activity.
     * This is called each time the configuration changes (for example if the device rotates).
     */
    private void initUi() {
        setContentView(R.layout.activity_main);

        if (isFirstRun)
            findViewById(R.id.demo_view).setVisibility(View.VISIBLE);

        settings = findViewById(R.id.settings);

        ((PinView) findViewById(R.id.single_view)).showPins(pins);
        ((PinView) findViewById(R.id.grid_view)).showPins(pins);
        ((PinView) findViewById(R.id.single_view)).setOnTimeRangeChangedListener(this);
        ((PinView) findViewById(R.id.grid_view)).setOnTimeRangeChangedListener(this);

        setSettingsVisible(settingsVisible);

        ((SeekBar)findViewById(R.id.seek_bar_measurement_interval)).setProgress((measurementInterval - MIN_MEASUREMENT_INTERVAL) * 100 / (MAX_MEASUREMENT_INTERVAL - MIN_MEASUREMENT_INTERVAL));
        ((SeekBar)findViewById(R.id.seek_bar_num_datapoints)).setProgress((numDatapoints - MIN_NUM_DATAPOINTS) * 100 / (MAX_NUM_DATAPOINTS - MIN_NUM_DATAPOINTS));

        ((SeekBar)findViewById(R.id.seek_bar_measurement_interval)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setMeasurementInterval((int) (MIN_MEASUREMENT_INTERVAL + progress / 100. * (MAX_MEASUREMENT_INTERVAL - MIN_MEASUREMENT_INTERVAL)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        ((SeekBar)findViewById(R.id.seek_bar_num_datapoints)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setPaused(true);
                setMaxDatapoints((int) (MIN_NUM_DATAPOINTS + seekBar.getProgress() / 100. * (MAX_NUM_DATAPOINTS - MIN_NUM_DATAPOINTS)));
                restorePaused();
            }
        });

        // Has to be called after layout because it affects view visibility.
        handler.post(new Runnable() {
            @Override
            public void run() {
                setViewMode(viewMode);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Store preferences.
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("view_mode", viewMode)
                .putInt("measurement_interval", measurementInterval)
                .putInt("num_datapoints", numDatapoints)
                .putLong("time_range", Double.doubleToRawLongBits(timeRangeMax - timeRangeMin))
                .putBoolean("settings_visible", settingsVisible);

        for (Pin pin : pins) {
            String pinKey = String.valueOf(pin.number);
            editor.putBoolean(pinKey + "_saved", true)
                    .putBoolean(pinKey + "_enabled", pin.visible)
                    .putString(pinKey + "_description", pin.description)
                    .putInt(pinKey + "_color", pin.color);
        }

        editor.commit();
    }

    private void setMaxDatapoints(int numDatapoints) {
        this.numDatapoints = numDatapoints;
        for (Pin pin : pins) {
            pin.setMaxDatapoints(numDatapoints);
        }
    }

    private void setMeasurementInterval(int measurementInterval) {
        this.measurementInterval = measurementInterval;
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Activity and thus data collection keeps running, only view is recreated.

        // Save state for selected views.
        ArrayList<Parcelable> viewStates = new ArrayList<Parcelable>();
        View gridViewWrapper = findViewById(R.id.grid_view_wrapper);
        if (gridViewWrapper instanceof MyScrollView) {
            viewStates.add(((MyScrollView)gridViewWrapper).onSaveInstanceState());
        }

        isFirstRun = false;

        initUi();

        // Restore state.
        gridViewWrapper = findViewById(R.id.grid_view_wrapper);
        if (gridViewWrapper instanceof MyScrollView) {
            ((MyScrollView)gridViewWrapper).onRestoreInstanceState(viewStates.remove(0));
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Hide demo view on touch.
        if (isFirstRun && findViewById(R.id.demo_view).getVisibility() == View.VISIBLE)
            findViewById(R.id.demo_view).setVisibility(View.GONE);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray("pins", pins);
        outState.putDouble("time_range_min", timeRangeMin);
        outState.putDouble("time", time);
    }


    /**
     * @param viewMode - one of VIEW_MODE_SINGLE or VIEW_MODE_GRID
     */
    public void setViewMode(int viewMode) {
        // TODO: Maybe switch icons.
        switch (viewMode) {
            case VIEW_MODE_SINGLE:
                findViewById(R.id.single_view).setVisibility(View.VISIBLE);
                findViewById(R.id.grid_view_wrapper).setVisibility(View.GONE);
                currentView = (PinView) findViewById(R.id.single_view);
                break;
            case VIEW_MODE_GRID:
                // TODO: Show item 'Collapse all'.
                findViewById(R.id.single_view).setVisibility(View.GONE);
                findViewById(R.id.grid_view_wrapper).setVisibility(View.VISIBLE);
                currentView = (PinView) findViewById(R.id.grid_view_wrapper).findViewById(R.id.grid_view);
                break;
            default:
                throw new IllegalArgumentException("View Mode " + viewMode + " is not supported");
        }

        currentView.onTimeRangeChanged(timeRangeMin, timeRangeMax);
        currentView.onPinMetaDataChanged();
        this.viewMode = viewMode;
        currentView.onPinSeriesDataChanged();
    }

    public void switchView() {
        switch (viewMode) {
            case VIEW_MODE_SINGLE:
                setViewMode(VIEW_MODE_GRID);
                break;
            case VIEW_MODE_GRID:
                setViewMode(VIEW_MODE_SINGLE);
                break;
        }
    }

    public void setPaused(boolean paused) {
        pausedBefore = this.paused;
        if (paused || ioioConnected) {  // Do not start if IOIO is not connected.
            this.paused = paused;
            if (paused) {
                if (menuItemStartPause != null) {
                    menuItemStartPause.setIcon(getResources().getDrawable(R.drawable.ic_menu_start));
                    menuItemStartPause.setTitle("Start");
                }
                lastMeasuredTime = -1;
            } else {
                if (menuItemStartPause != null) {
                    menuItemStartPause.setIcon(getResources().getDrawable(R.drawable.ic_menu_pause));
                    menuItemStartPause.setTitle("Pause");
                }
            }
            currentView.onPinSeriesDataChanged();
        } else {
            showToast("Not connected");
        }
    }

    public void restorePaused() {
        setPaused(pausedBefore);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menuItemStartPause = menu.findItem(R.id.start_pause);
        menuItemShowSettings = menu.findItem(R.id.show_settings);
        if (menuItemShowSettings != null)
            menuItemShowSettings.setChecked(settingsVisible);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.start_pause) {
            setPaused(!paused);
            return true;
        } else if (id == R.id.clear) {
            // TODO: Show undo button.
            clear();
            return true;
        } else if (id == R.id.switch_view) {
            switchView();
            return true;
        } else if (id == R.id.save) {
            boolean pausedBefore = paused;
            setPaused(true);

            Time now = new Time();
            now.setToNow();
            final String filename = now.format("%Y-%m-%d_%H-%M-%S") + ".csv";

            // Save in AsyncTask to free the UI.
            new SaveToCsvTask().execute(filename);

            setPaused(pausedBefore);
        } else if (id == R.id.show_settings) {
            setSettingsVisible(!settingsVisible);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new IOIOLooper() {

            private AnalogInput[] analogPins;
            private DigitalOutput led;  // Negative logic.

            @Override
            public void setup(IOIO ioio) throws ConnectionLostException {
                led = ioio.openDigitalOutput(IOIO.LED_PIN, true);

                // Open the analog pins that will be measured.
                analogPins = new AnalogInput[numPins];
                for (int i = 0; i < numPins; i++)
                    analogPins[i] = ioio.openAnalogInput(FIRST_PIN_NUMBER + i);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onConnected();
                    }
                });
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                try {
                    if (paused) {
                        led.write(true);  // Turn led off during pause.
                    } else {
                        led.write(false);  // Turn led on during measurement.

                        // Measure time since the last measurement.
                        long measuredTime = System.nanoTime();
                        if (lastMeasuredTime != -1)
                            time += (measuredTime - lastMeasuredTime) / 1000000000.;
                        lastMeasuredTime = measuredTime;

                        // Measure voltage.
                        for (int i = 0; i < numPins; i++)
                            pins[i].addPointToSeries(time, analogPins[i].getVoltage());

                        onMeasurementFinished();
                    }
                    Thread.sleep(measurementInterval);
                } catch (ConnectionLostException e) {
                    // IOIOLib does not treat ConnectionLostException properly, so it's done here.
                    disconnected();
                }
            }

            @Override
            public void disconnected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onDisconnected();
                    }
                });
            }

            @Override
            public void incompatible() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onIncompatibleFirmware();
                    }
                });
            }
        };
    }

    private void onConnected() {
        ioioConnected = true;
        showToast("Connected");
    }

    private void onDisconnected() {
        setPaused(true);
        ioioConnected = false;
        showToast("Disconnected");
    }

    private void onIncompatibleFirmware() {
        setPaused(true);
        ioioConnected = false;
        showToast("Incompatible Firmware");
    }

    private void onMeasurementFinished() {
        // Move plots if the graphs do not fit on the screen any more.
        if (time >= timeRangeMax) {
            double offset = (timeRangeMax - timeRangeMin) / 2;
            timeRangeMin += offset;
            timeRangeMax += offset;
            currentView.onTimeRangeChanged(timeRangeMin, timeRangeMax);
        }
    }

    /**
     * Show a short notification to the user.
     * @param text The content of the notification
     */
    private void showToast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Clear all measured data points and reset the user interface.
     */
    public void clear() {
        setPaused(true);
        for (Pin pin : pins)
            pin.clearSeries();

        time = 0;
        lastMeasuredTime = -1;

        // Set the time in the plots to 0 but keep their time range.
        double timeRange = timeRangeMax - timeRangeMin;
        timeRangeMin = 0;
        timeRangeMax = timeRange;

        currentView.onTimeRangeChanged(timeRangeMin, timeRangeMax);
        currentView.onPinSeriesDataChanged();
        restorePaused();
    }

    /**
     * Iterate through a color spectrum of different shades of purple, blue, green, yellow.
     * @return The color integer value
     */
    protected int nextColor() {
        int color = allColors[colorIterator];
        colorIterator = (colorIterator + 1) % allColors.length;
        return color;
    }

    /**
     * Implementation of OnTimeRangeChangedListener. Triggered when plot is panned or zoomed.
     * @param min The new minimum time range
     * @param max The new maximum time range
     */
    @Override
    public void onTimeRangeChanged(PinView source, double min, double max) {
        timeRangeMin = min;
        timeRangeMax = max;
    }

    /**
     * Make the settings view visible or gone and change the corresponding menu item.
     * @param visible True is visible, false is gone
     */
    public void setSettingsVisible(boolean visible) {
        if (visible)
            settings.setVisibility(View.VISIBLE);
        else
            settings.setVisibility(View.GONE);

        if (menuItemShowSettings != null)
            menuItemShowSettings.setChecked(visible);
        settingsVisible = visible;
    }

    /**
     * Asynchronous background task that saves time and voltage data of all pins to a CSV file.
     */
    private class SaveToCsvTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // All pins have the same number of steps at the same time points.
                int numSteps = pins[0].getNumSteps();
                if (numSteps > 0) {

                    String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
                    File directory = new File(externalStorage + "/IOIO Meter/");
                    directory.mkdirs();  // Create directories if not present.

                    if (directory.isDirectory()) {
                        String filename = params[0];
                        File file = new File(externalStorage + "/IOIO Meter/", filename);
                        Log.d(D, file.getAbsolutePath());

                        StringBuilder builder = new StringBuilder();
                        builder.append("time/s");
                        for (Pin pin : pins) {
                            builder.append(",v").append(pin.number).append("/V");
                        }
                        builder.append("\n");

                        for (int i = 0; i < numSteps; i++) {
                            // TODO: Check that the time of all pins equals, throw error or show toast if not.
                            builder.append(pins[0].getTime(i));
                            for (Pin pin : pins) {
                                builder.append(",").append(pin.getVoltage(i));
                            }
                            builder.append("\n");

                            // Publish progress every few steps.
                            if (i % 10 == 0)
                                publishProgress((int)(i / (float)numSteps * 100));
                        }

                        // Remove trailing "\n".
                        builder.deleteCharAt(builder.length()-1);

                        try {
                            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file));
                            writer.write(builder.toString());
                            writer.close();
                            return "Saved in folder IOIO Meter";
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            return "Cannot create file";
                        } catch (IOException e) {
                            e.printStackTrace();
                            return "Cannot write file";
                        }
                    } else {
                        return "Cannot create directory";
                    }
                } else {
                    return "Nothing to save";
                }
            } else {
                return "Cannot access external storage";
            }
        }

        @Override
        protected void onPreExecute() {
            mainProgressBar.setVisibility(View.VISIBLE);
            mainProgressBar.setProgress(0);
        }

        @Override
        protected void onPostExecute(String s) {
            mainProgressBar.setVisibility(View.INVISIBLE);
            showToast(s);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mainProgressBar.setProgress(values[0]);
        }
    }
}