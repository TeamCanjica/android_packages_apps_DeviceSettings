/*
 * Copyright (C) 2012 The CyanogenMod Project
 * Copyright (C) 2014 TeamCanjica https://github.com/TeamCanjica
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.teamcanjica.settings.device.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;

import com.teamcanjica.settings.device.DeviceSettings;
import com.teamcanjica.settings.device.R;
import com.teamcanjica.settings.device.Utils;

public class AdvancedFragmentActivity extends PreferenceFragment {

	private static final String TAG = "NovaThor_Settings_Advanced";

	private static final String FILE_ACCELEROMETER_CALIB = "/sys/class/sensors/accelerometer_sensor/calibration";
	private static final String FILE_BLN = "/sys/class/misc/backlightnotification/enabled";
	private static final String FILE_BURNING_LED = "/sys/class/camera/flash/burning_led";
	private static final String FILE_VOLTAGE1 = "/sys/kernel/liveopp/arm_step3";
	private static final String FILE_VOLTAGE2 = "/sys/kernel/liveopp/arm_step4";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.advanced_preferences);

		getPreferenceScreen().findPreference(DeviceSettings.KEY_CALIBRATE_ACCELEROMETER).setEnabled(
				((CheckBoxPreference) findPreference("use_accelerometer_calibration")).isChecked());

		getActivity().getActionBar().setTitle(getResources().getString(R.string.advanced_name));
		getActivity().getActionBar().setIcon(getResources().getDrawable(R.drawable.advanced_icon));

		// Compatibility check for janice (BLN)
		if (Utils.isJanice()) {
			getPreferenceScreen().removePreference(findPreference(DeviceSettings.KEY_BACKLIGHT));
		}

	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {

		String key = preference.getKey();

		Log.w(TAG, "key: " + key);

		if (key.equals(DeviceSettings.KEY_SWITCH_STORAGE)) {
			SystemProperties.set("persist.sys.vold.switchexternal", (((CheckBoxPreference) preference)
					.isChecked() ? "1" : "0"));
			Utils.showDialog(getActivity(),
					getString(R.string.reboot_dialog_head),
					getString(R.string.reboot_dialog_message),
					true);
		} else if (key.equals(DeviceSettings.KEY_USE_ACCELEROMETER_CALIBRATION)) {
			Utils.writeValue(FILE_ACCELEROMETER_CALIB, ((CheckBoxPreference) preference)
					.isChecked());
			getPreferenceScreen().findPreference(DeviceSettings.KEY_CALIBRATE_ACCELEROMETER).setEnabled(
					((CheckBoxPreference) preference).isChecked());
		} else if (key.equals(DeviceSettings.KEY_CALIBRATE_ACCELEROMETER)) {
			// when calibration data utilization is disabled and enabled back,
			// calibration is done at the same time by driver
			Utils.writeValue(FILE_ACCELEROMETER_CALIB, "0");
			Utils.writeValue(FILE_ACCELEROMETER_CALIB, "1");
			Utils.showDialog(getActivity(),
					getString(R.string.accelerometer_dialog_head),
					getString(R.string.accelerometer_dialog_message),
					false);
		} else if (key.equals(DeviceSettings.KEY_DISABLE_BLN)) {
			Utils.writeValue(FILE_BLN, !((CheckBoxPreference) preference)
					.isChecked());
		} else if (key.equals(DeviceSettings.KEY_BURNING_LED)) {
			Utils.writeValue(FILE_BURNING_LED, ((CheckBoxPreference) preference)
					.isChecked());
		} else if (key.equals(DeviceSettings.KEY_ENABLE_VOLTAGE)) {
			Utils.writeValue(FILE_VOLTAGE1, "set_volt=" + (((CheckBoxPreference) preference)
					.isChecked() ? "1" : "0 "));
			if (Utils.isJanice()) {
				Utils.writeValue(FILE_VOLTAGE2, "set_volt=" + (((CheckBoxPreference) preference)
					.isChecked() ? "1" : "0 "));
			}
		}

		return true;
	}

	public static void restore(Context context) {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);

		SystemProperties.set("persist.sys.vold.switchexternal", sharedPrefs.getBoolean(
				DeviceSettings.KEY_SWITCH_STORAGE, false) ? "1" : "0");

		Utils.writeValue(FILE_BURNING_LED, sharedPrefs.getBoolean(
				DeviceSettings.KEY_BURNING_LED, false) ? "1" : "0");

		Utils.writeValue(FILE_BLN, sharedPrefs.getBoolean(
				DeviceSettings.KEY_DISABLE_BLN, false) ? "0" : "1");

		boolean accelerometerCalib = sharedPrefs.getBoolean(
				DeviceSettings.KEY_USE_ACCELEROMETER_CALIBRATION, true);
		if (!accelerometerCalib)
			Utils.writeValue(FILE_ACCELEROMETER_CALIB, "0");

		Utils.writeValue(FILE_VOLTAGE1, "set_volt=" + (sharedPrefs.getBoolean(
				DeviceSettings.KEY_ENABLE_VOLTAGE, false) ? "1" : "0"));

		if (Utils.isJanice())
			Utils.writeValue(FILE_VOLTAGE2, "set_volt=" + (sharedPrefs.getBoolean(
					DeviceSettings.KEY_ENABLE_VOLTAGE, false) ? "1" : "0"));

	}

}
