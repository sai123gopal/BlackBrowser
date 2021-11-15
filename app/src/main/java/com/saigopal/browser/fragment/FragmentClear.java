package com.saigopal.browser.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.saigopal.browser.R;
import com.saigopal.browser.unit.HelperUnit;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.preference.PreferenceFragmentCompat;


@SuppressWarnings("ConstantConditions")
public class FragmentClear extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preference_clear, rootKey);

        findPreference("sp_deleteDatabase").setOnPreferenceClickListener(preference -> {
            final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            final BottomSheetDialog dialog = new BottomSheetDialog(requireActivity());
            View dialogView = View.inflate(getActivity(), R.layout.dialog_action, null);
            TextView textView = dialogView.findViewById(R.id.dialog_text);
            textView.setText(R.string.hint_database);
            Button action_ok = dialogView.findViewById(R.id.action_ok);
            action_ok.setOnClickListener(view -> {
                dialog.cancel();
                getActivity().deleteDatabase("data.db");
                sp.edit().putInt("restart_changed", 1).apply();
                getActivity().finish();
            });
            dialog.setContentView(dialogView);
            dialog.show();
            HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
            return false;
        });
    }
}
