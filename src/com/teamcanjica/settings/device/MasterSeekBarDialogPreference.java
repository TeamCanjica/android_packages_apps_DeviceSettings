package com.teamcanjica.settings.device;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class MasterSeekBarDialogPreference extends
		DialogPreference implements SeekBar.OnSeekBarChangeListener, OnPreferenceChangeListener {
	// Layout widgets.
	private SeekBar seekBar = null;
	private TextView valueText = null;

	// Custom xml attributes.
	private int maximumValue = 0;
	private int minimumValue = 0;
	private float stepSize = 0;
	private String units = null;

	private int value = 0;

	private static final String TAG = "GalaxyAce2_Settings_Seekbar";

	private static final String FILE_READAHEADKB = "/sys/block/mmcblk0/queue/read_ahead_kb";
	private static final String FILE_CPU_VOLTAGE = "/sys/kernel/liveopp/arm_step";
	private static final String FILE_CYCLE_CHARGING = "/sys/kernel/abb-fg/fg_cyc";
	private static final int defaultVoltValues[] = {0x18, 0x1a, 0x20, 0x24, 0x2f, 0x32, 0x3f, 0x3f, 0x3f, 0x3f};
	private static final int steps[] = {50, 37, 25, 12, 0};

	/**
	 * The SeekBarDialogPreference constructor.
	 * @param context of this preference.
	 * @param attrs custom xml attributes.
	 */
	public MasterSeekBarDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.setOnPreferenceChangeListener(this);

		TypedArray typedArray = context.obtainStyledAttributes(attrs,
			R.styleable.MasterSeekBarDialogPreference);

		maximumValue = typedArray.getInteger(
			R.styleable.MasterSeekBarDialogPreference_maximumValue, 0);
		minimumValue = typedArray.getInteger(
			R.styleable.MasterSeekBarDialogPreference_minimumValue, 0);
		stepSize = typedArray.getFloat(
			R.styleable.MasterSeekBarDialogPreference_stepSize, 1);
		units = typedArray.getString(
			R.styleable.MasterSeekBarDialogPreference_units);

		typedArray.recycle();
	}

	/**
	 * {@inheritDoc}
	 */
	protected View onCreateDialogView() {
		LayoutInflater layoutInflater = LayoutInflater.from(getContext());

		View view = layoutInflater.inflate(
			R.layout.preference_seek_bar_dialog, null);

		seekBar = (SeekBar)view.findViewById(R.id.seekbar);
		valueText = (TextView)view.findViewById(R.id.valueText);

		// Get the persistent value and correct it for the minimum value.
		value = getPersistedInt(minimumValue) - minimumValue;

		// You're never know...
		if (value < 0) {
			value = 0;
		}

		seekBar.setOnSeekBarChangeListener(this);
		seekBar.setKeyProgressIncrement((int) stepSize);
		seekBar.setMax(maximumValue - minimumValue);
		SharedPreferences prefs = getContext().getSharedPreferences(DeviceSettings.KEY_SEEKBARVAL, Context.MODE_PRIVATE);
		value = prefs.getInt("seekBarValue", 512);
		seekBar.setProgress(value);

		return view;
	}
	/**
	 * {@inheritDoc}
	 */
	public void onProgressChanged(SeekBar seek, int newValue,
			boolean fromTouch) {
		// Round the value to the closest integer value.
		if (stepSize >= 1) {
			value = (int) (Math.round(newValue/stepSize)*stepSize);
		}
		else {
			value = newValue;
		}

		// Set the valueText text.
		valueText.setText(String.valueOf(value + minimumValue) +
			(units == null ? "" : units));

		callChangeListener(value);
	}
	/**
	 * {@inheritDoc}
	 */
	public void onStartTrackingTouch(SeekBar seek) {
	}
	/**
	 * {@inheritDoc}
	 */
	public void onStopTrackingTouch(SeekBar seek) {
	}
	/**
	 * {@inheritDoc}
	 */
	public void onClick(DialogInterface dialog, int which) {
		// if the positive button is clicked, we persist the value.
		if (which == DialogInterface.BUTTON_POSITIVE) {
			SharedPreferences prefs = getContext().getSharedPreferences(DeviceSettings.KEY_SEEKBARVAL, Context.MODE_PRIVATE);
			prefs.edit().putInt("seekBarValue", seekBar.getProgress()).commit();
			if (shouldPersist()) {
				persistInt(value + minimumValue);
			}
		}

		super.onClick(dialog, which);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {

		String key = preference.getKey();
		Log.w(TAG, "key: " + key);


		if (key.equals(DeviceSettings.KEY_READAHEADKB)) {
			Utils.writeValue(FILE_READAHEADKB, String.valueOf((Integer) newValue + 128));
		} else if (key.equals(DeviceSettings.KEY_CPU_VOLTAGE)) {
			int i = 0;
			while (steps[i] != (Integer) newValue) {
			    i++;
			}
			for (int j = 0; j <= defaultVoltValues.length-1; j++) {
			    Utils.writeValue(FILE_CPU_VOLTAGE + String.valueOf(j), "varm=0x" + Integer.toHexString(defaultVoltValues[j] - i));
			}
		} else if (key.equals(DeviceSettings.KEY_DISCHARGING_THRESHOLD)) {
			Utils.writeValue(FILE_CYCLE_CHARGING, "dischar=" + String.valueOf((Integer) newValue));
		} else if (key.equals(DeviceSettings.KEY_RECHARGING_THRESHOLD)) {
			Utils.writeValue(FILE_CYCLE_CHARGING, "rechar=" + String.valueOf((Integer) newValue));
		}

		return true;
	}

	public static void restore(Context context) {

		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		Utils.writeValue(FILE_CYCLE_CHARGING, 
				"dischar=" + String.valueOf(sharedPrefs.getString(DeviceSettings.KEY_DISCHARGING_THRESHOLD, "100")));
		Utils.writeValue(FILE_CYCLE_CHARGING,
				"rechar=" + String.valueOf(sharedPrefs.getString(DeviceSettings.KEY_RECHARGING_THRESHOLD, "100")));

		Utils.writeValue(FILE_READAHEADKB,
				String.valueOf(sharedPrefs.getString(DeviceSettings.KEY_READAHEADKB, "512")));

		int i = 0;
		while (steps[i] != Integer.parseInt(sharedPrefs.getString(DeviceSettings.KEY_CPU_VOLTAGE, "50"))) {
		    i++;
		}
		for (int j = 0; j <= defaultVoltValues.length-1; j++) {
		    Utils.writeValue(FILE_CPU_VOLTAGE + String.valueOf(j), "varm=0x" + Integer.toHexString(defaultVoltValues[j] - i));
		}
	}
}
