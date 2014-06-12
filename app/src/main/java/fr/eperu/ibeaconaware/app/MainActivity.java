package fr.eperu.ibeaconaware.app;

import fr.eperu.ibeaconaware.app.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

import java.util.Collection;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MainActivity extends Activity implements IBeaconConsumer {

    protected static final String TAG = "SearchRegionActivity";

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    /**
     * Gestionnaire D'IBeacon de la librairie radiusnetwork.
     */
    private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // On bind le manager avec notre activity, ce qui fera un appel à notre methode onIBeaconServiceConnect.
        this.iBeaconManager.bind(this);

        setContentView(R.layout.activity_main);

        final View controlsView = findViewById(R.id.fullscreen_content_controls);
        final View contentView = findViewById(R.id.fullscreen_content);

        // Set up an instance of SystemUiHider to control the system UI for
        // this activity.
        mSystemUiHider = SystemUiHider.getInstance(this, contentView, HIDER_FLAGS);
        mSystemUiHider.setup();
        mSystemUiHider
                .setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
                    // Cached values.
                    int mControlsHeight;
                    int mShortAnimTime;

                    @Override
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
                    public void onVisibilityChange(boolean visible) {
                        if (mControlsHeight == 0) {
                            mControlsHeight = controlsView.getHeight();
                        }
                        if (mShortAnimTime == 0) {
                            mShortAnimTime = getResources().getInteger(
                                    android.R.integer.config_shortAnimTime);
                        }
                        controlsView.animate()
                                .translationY(visible ? 0 : mControlsHeight)
                                .setDuration(mShortAnimTime);

                        if (visible && AUTO_HIDE) {
                            // Schedule a hide().
                            delayedHide(AUTO_HIDE_DELAY_MILLIS);
                        }
                    }
                });

        // Set up the user interaction to manually show or hide the system UI.
        contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TOGGLE_ON_CLICK) {
                    mSystemUiHider.toggle();
                } else {
                    mSystemUiHider.show();
                }
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.search_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(1000);
        this.iBeaconManager.setForegroundScanPeriod(10 * 1000);
        this.iBeaconManager.setForegroundBetweenScanPeriod(3 * 1000);
    }


    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    Handler mHideHandler = new Handler();
    Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            mSystemUiHider.hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /**
     * Lorsqu'on va stopper l'activity on va passer le baconmanager en mode background.
     */
    @Override
    protected void onStop() {
        super.onStop();
        this.iBeaconManager.setBackgroundBetweenScanPeriod(5 * 000);
        this.iBeaconManager.setBackgroundBetweenScanPeriod(30 * 000);
        this.iBeaconManager.setBackgroundMode(this, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.iBeaconManager.unBind(this);
    }

    @Override
    public void onIBeaconServiceConnect() {
        this.initIbeaconManager(this.iBeaconManager);
    }


    public void initIbeaconManager(final IBeaconManager iBeaconManager) {

        if (iBeaconManager == null) {
            return ;
        }

        iBeaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<IBeacon> iBeacons, Region region) {
                if (iBeacons != null && iBeacons.size() > 0) {
                    Log.i(TAG, "Here are the iBeacons in the region  id : " + region.getProximityUuid());
                    for (IBeacon iBeacon : iBeacons) {
                        int proximity = iBeacon.getProximity();
                        String label = "iBeacon " + iBeacon.getProximityUuid();
                        switch (proximity) {
                            case IBeacon.PROXIMITY_FAR:
                                Log.i(TAG, label + " is far away.");
                                break;
                            case IBeacon.PROXIMITY_IMMEDIATE:
                                Log.i(TAG, label + " is next to you.");
                                break;
                            case IBeacon.PROXIMITY_NEAR:
                                Log.i(TAG, label + " is near.");
                                break;
                            default:
                                Log.i(TAG, "I don't know where is iBeacon " + iBeacon.getProximityUuid());
                        }
                        Log.i(TAG, label + " is about " + iBeacon.getAccuracy() + "meters away");
                    }
                }
//                Log.i(TAG, "unbind du manager");
//                iBeaconManager.unBind(MainActivity.this);
//                Log.i(TAG, "bind du manager");
//                iBeaconManager.bind(MainActivity.this);
            }
        });


        iBeaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(com.radiusnetworks.ibeacon.Region region) {
                Log.i(TAG, "I just saw a region for the firt time, id : " + region.getProximityUuid());
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an iBeacon id : " + region.getProximityUuid());
            }

            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                Log.i(TAG, "I have just switched from seeing/not seeing region : " + state);
            }
        });

        try {
            iBeaconManager.startMonitoringBeaconsInRegion(new Region("ibeaconAwareRegion", null, null, null));
            iBeaconManager.startRangingBeaconsInRegion(new Region("ibeaconAwareRegion", null, null, null));
        } catch (RemoteException e) {
        }

    }
}
