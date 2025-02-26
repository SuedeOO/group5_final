package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.AccelerometerService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;
import cs.umass.edu.myactivitiestoolkit.services.msband.BandService;

/**
 * Created by Jon on 12/10/2017.
 */

public class ThrowFragment extends Fragment {
    /**
     * Used during debugging to identify logs by class.
     */
    @SuppressWarnings("unused")
    private static final String TAG = ThrowFragment.class.getName();

    /**
     * The switch which toggles the {@link AccelerometerService}.
     **/
    public Switch throwSwitch;
    private TextView txtBestThrow;
    private TextView txtLastThrow;
    private ServiceManager serviceManager;

    /**
     * The receiver listens for messages from the {@link AccelerometerService}, e.g. was the
     * service started/stopped, and updates the status views accordingly. It also
     * listens for sensor data and displays the sensor readings to the user.
     */
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // System.out.println("looooook at me");
            if (intent.getAction() != null) {
                //System.out.println("looook not null");
                if (intent.getAction().equals(Constants.ACTION.BROADCAST_MESSAGE)) {
                    //System.out.println("??");
                    int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);
                    if (message == Constants.MESSAGE.ACCELEROMETER_SERVICE_STOPPED) {
                        throwSwitch.setChecked(false);
                    } else if (message == Constants.MESSAGE.BAND_SERVICE_STOPPED) {
                        throwSwitch.setChecked(false);
                    }
                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_LAST_THROW)) {
                    //System.out.println("last");
                    int distance = intent.getIntExtra(Constants.KEY.LAST_THROW, 0);
                    displayLastThrow(distance);
                } else if (intent.getAction().equals(Constants.ACTION.BROADCAST_BEST_THROW)) {
                    //System.out.println("highest");
                    int distance = intent.getIntExtra(Constants.KEY.BEST_THROW, 0);
                    displayBestThrow(distance);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.serviceManager = ServiceManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_throw, container, false);
        txtBestThrow = (TextView) view.findViewById(R.id.txtBestThrow);
        txtLastThrow = (TextView) view.findViewById(R.id.txtLastThrow);

        throwSwitch = (Switch) view.findViewById(R.id.throwSwitch);
        throwSwitch.setChecked(serviceManager.isServiceRunning(AccelerometerService.class));
        throwSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
                if (enabled) {
                    Intent intent = new Intent();
                    intent.putExtra(Constants.PAGE.PAGE_ZERO,0);
                    //System.out.println("Enabled!!!!");
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    boolean runOverMSBand = preferences.getBoolean(getString(R.string.pref_msband_key),
                            getResources().getBoolean(R.bool.pref_msband_default));
                    if (runOverMSBand) {
                        serviceManager.startSensorService(BandService.class);
                    } else {
                        serviceManager.startSensorService(AccelerometerService.class);
                    }
                } else {
                    if (serviceManager.isServiceRunning(AccelerometerService.class))
                        serviceManager.stopSensorService(AccelerometerService.class);
                    if (serviceManager.isServiceRunning(BandService.class))
                        serviceManager.stopSensorService(BandService.class);
                }
            }
        });

        return view;
    }

    private void displayLastThrow(final int distance) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtLastThrow.setText(String.format(Locale.getDefault(), getString(R.string.last_throw), distance));
            }
        });
    }


    private void displayBestThrow(final int distance) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtBestThrow.setText(String.format(Locale.getDefault(), getString(R.string.best_throw), distance));
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
        filter.addAction(Constants.ACTION.BROADCAST_AVERAGE_ACCELERATION);
        filter.addAction(Constants.ACTION.BROADCAST_BEST_THROW);
        filter.addAction(Constants.ACTION.BROADCAST_LAST_THROW);
        broadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        try {
            broadcastManager.unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        super.onStop();
    }

}