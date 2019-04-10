package ceit.aut.digitrec.gui.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	public static final int CODE = 0;
	public static final String PREF_SAVE = "pref_save";
	public static final String KEY_PREF = "pref_algorithm";
	private static final String TRAIN_HW = "pref_train_hw";
	private static final String TRAIN_IM = "pref_train_im";
	private static final String ONLINE_RATE = "pref_online_rate";

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		if (preference.getKey().equals(TRAIN_HW)) {
			Intent intent = new Intent(MainActivity.REMOVE_WEIGHTS);
			intent.setClass(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		}
		if (preference.getKey().equals(TRAIN_IM)) {
			Intent intent = new Intent(MainActivity.REMOVE_WEIGHTS_IM);
			intent.setClass(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (ONLINE_RATE.equals(key)) {
			correctOnlineDecimalValueIfNeeded(this);
		}
	}

	/**
	 * This performs conversion of real values that contain the ',' decimal
	 * delimiter to the '.' delimiter. These are automatically commited in the
	 * default shared preferences. In addition, if the values are invalid,
	 * default application falues are recovered.
	 *
	 * @param context
	 *            Context for getting and editing preferences.
	 */
	public static void correctOnlineDecimalValueIfNeeded(final Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String learningRateValue = prefs.getString(ONLINE_RATE,
				context.getString(R.string.pref_online_update_defvalue));

		boolean changed = false;

		if (learningRateValue.contains(",")) {
			learningRateValue = learningRateValue.replace(",", ".");
			changed = true;
		}

		try {
			Double.parseDouble(learningRateValue);
		} catch (NumberFormatException e) {
			learningRateValue = context
					.getString(R.string.pref_online_update_defvalue);
			changed = true;
		}

		if (changed) {
			prefs.edit().putString(ONLINE_RATE, learningRateValue)
					.commit();
		}
	}

	/**
	 * Gets the learning rate from the default shared preferences.
	 *
	 * @param context
	 *            Context for getting preferences.
	 * @return Learning rate preference.
	 */
	public static double getOnlineLearningRate(final Context context) {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		String learningRate = prefs.getString(ONLINE_RATE,
				context.getString(R.string.pref_online_update_defvalue));
		return Double.parseDouble(learningRate);
	}
}
