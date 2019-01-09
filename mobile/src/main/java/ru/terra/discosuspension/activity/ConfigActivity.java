package ru.terra.discosuspension.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

import ru.terra.discosuspension.R;

public class ConfigActivity extends PreferenceActivity implements
        OnPreferenceChangeListener {

    public static final String BLUETOOTH_LIST_KEY = "bluetooth_list_preference";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ArrayList<CharSequence> pairedDeviceStrings = new ArrayList<CharSequence>();
        ArrayList<CharSequence> vals = new ArrayList<CharSequence>();
        ListPreference listBtDevices = (ListPreference) getPreferenceScreen()
                .findPreference(BLUETOOTH_LIST_KEY);
        final BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            listBtDevices
                    .setEntries(pairedDeviceStrings.toArray(new CharSequence[0]));
            listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));

            // we shouldn't get here, still warn user
            Toast.makeText(this, "This device does not support Bluetooth.",
                    Toast.LENGTH_LONG).show();

            return;
        }

        final Activity thisActivity = this;
        listBtDevices.setEntries(new CharSequence[1]);
        listBtDevices.setEntryValues(new CharSequence[1]);
        listBtDevices.setOnPreferenceClickListener(preference -> {
            // see what I mean in the previous comment?
            if (!mBtAdapter.isEnabled()) {
                Toast.makeText(thisActivity,
                        "This device does not support Bluetooth or it is disabled.",
                        Toast.LENGTH_LONG).show();
                return false;
            }
            return true;
        });

        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDeviceStrings.add(device.getName() + "\n" + device.getAddress());
                vals.add(device.getAddress());
            }
        }
        listBtDevices.setEntries(pairedDeviceStrings.toArray(new CharSequence[0]));
        listBtDevices.setEntryValues(vals.toArray(new CharSequence[0]));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }
}