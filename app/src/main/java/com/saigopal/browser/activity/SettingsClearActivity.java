package com.saigopal.browser.activity;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.saigopal.browser.R;
import com.saigopal.browser.fragment.FragmentClear;
import com.saigopal.browser.service.ClearService;
import com.saigopal.browser.unit.HelperUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;



public class SettingsClearActivity extends AppCompatActivity {

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        HelperUnit.applyTheme(this);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new FragmentClear())
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_clear, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_clear:
                final BottomSheetDialog dialog = new BottomSheetDialog(SettingsClearActivity.this);
                View dialogView = View.inflate(SettingsClearActivity.this, R.layout.dialog_action, null);
                TextView textView = dialogView.findViewById(R.id.dialog_text);
                textView.setText(R.string.hint_database);
                Button action_ok = dialogView.findViewById(R.id.action_ok);
                action_ok.setOnClickListener(view -> {
                    Intent toClearService = new Intent(SettingsClearActivity.this, ClearService.class);
                    startService(toClearService);
                    dialog.cancel();
                });
                dialog.setContentView(dialogView);
                dialog.show();
                HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
                break;
            default:
                break;
        }
        return true;
    }
}