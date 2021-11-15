package com.saigopal.browser.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.saigopal.browser.R;

public class FragmentSettingsGesture extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_gesture, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {

        if (key.equals("sp_gestures_use") || key.equals("sp_gesture_action")) {
            sp.edit().putInt("restart_changed", 1).apply();
        }
    }
}
