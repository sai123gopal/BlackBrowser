package com.saigopal.browser.fragment;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.saigopal.browser.R;
import com.saigopal.browser.activity.FeedbackActivity;
import com.saigopal.browser.activity.SettingsClearActivity;
import com.saigopal.browser.activity.SettingsDataActivity;
import com.saigopal.browser.activity.SettingsFilterActivity;
import com.saigopal.browser.activity.SettingsGestureActivity;
import com.saigopal.browser.activity.SettingsStartActivity;
import com.saigopal.browser.activity.SettingsUIActivity;
import com.saigopal.browser.unit.HelperUnit;

import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;


@SuppressWarnings("ConstantConditions")
public class FragmentSettings extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {


    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_setting, rootKey);

       findPreference("settings_filter").setOnPreferenceClickListener(preference -> {
           Intent intent = new Intent(getActivity(), SettingsFilterActivity.class);
           requireActivity().startActivity(intent);
           return false;
       });
        findPreference("settings_data").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SettingsDataActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_ui").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SettingsUIActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_gesture").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SettingsGestureActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_start").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SettingsStartActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_clear").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SettingsClearActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
        findPreference("settings_help").setOnPreferenceClickListener(preference -> {
            showLicenseDialog(getString(R.string.dialogHelp_tipTitle), getString(R.string.dialogHelp_tipText));
            return false;
        });
        findPreference("settings_info").setOnPreferenceClickListener(preference -> {
            showLicenseDialog(getString(R.string.menu_other_info), getString(R.string.changelog_dialog));
            return false;
        });
        findPreference("settings_desktop_mode").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
                String action_text = "Enable";
                AlertDialog.Builder alertDialog =  new AlertDialog.Builder(getContext());
                alertDialog.setTitle("Desktop Mode (Beta)");
                alertDialog.setCancelable(true);
                alertDialog.setIcon(R.drawable.icon_desktop);
                alertDialog.setMessage("Desktop mode is currently in beta.\nSome websites may not load in desktop mode.");
                if(sp.getBoolean("desktop_mode",false)){
                    action_text = "Disable";
                }
                alertDialog.setPositiveButton(action_text, new DialogInterface.OnClickListener() {
                    @SuppressLint("ApplySharedPref")
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(sp.getBoolean("desktop_mode",false)){
                            sp.edit().putBoolean("desktop_mode",false).commit();
                        }else {
                            sp.edit().putBoolean("desktop_mode",true).commit();
                        }

                    }
                });

                alertDialog.show();
                    return false;
            }
        });
        /*findPreference("support").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), SupportActivity.class);
            requireActivity().startActivity(intent);
            return false;
        });
         */
        findPreference("feedback").setOnPreferenceClickListener(preference ->
        {
            startActivity(new Intent(getActivity(), FeedbackActivity.class));
            return false;
        });
        findPreference("settings_appSettings").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
            intent.setData(uri);
            getActivity().startActivity(intent);
            return false;
        });
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("userAgent") || key.equals("sp_search_engine_custom") || key.equals("@string/sp_search_engine")) {
            sp.edit().putInt("restart_changed", 1).apply();
        }
    }


    private void showLicenseDialog(String title, String text) {

        final BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
        View dialogView = View.inflate(getActivity(), R.layout.dialog_text, null);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(title);

        TextView dialog_text = dialogView.findViewById(R.id.dialog_text);
        dialog_text.setText(HelperUnit.textSpannable(text));

         dialog_text.setMovementMethod(LinkMovementMethod.getInstance());
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }




}
