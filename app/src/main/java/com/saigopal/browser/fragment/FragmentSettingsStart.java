package com.saigopal.browser.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.saigopal.browser.R;
import com.saigopal.browser.activity.WhitelistAdBlock;
import com.saigopal.browser.activity.WhitelistCookie;
import com.saigopal.browser.activity.WhitelistJavascript;
import com.saigopal.browser.activity.WhitelistRemote;



public class FragmentSettingsStart extends PreferenceFragmentCompat {


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_start, rootKey);

        findPreference("start_AdBlock").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), WhitelistAdBlock.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("start_java").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), WhitelistJavascript.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("start_cookie").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), WhitelistCookie.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("start_remote").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), WhitelistRemote.class);
            requireActivity().startActivity(intent);
            return false;
        });
    }
}
