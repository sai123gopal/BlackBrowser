package com.saigopal.browser.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.ValueCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.saigopal.browser.R;

public class MainActivity extends AppCompatActivity {

    public static final int INPUT_FILE_REQUEST_CODE = 1;
    private ValueCallback<Uri[]> mFilePathCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Legacy chrome stays available for one release cycle via LEGACY_SHELL=true.
        if (!com.saigopal.browser.BuildConfig.LEGACY_SHELL) {
            com.saigopal.browser.ui.shell.ComposeShellHost.INSTANCE.install(this);
            com.saigopal.browser.ui.ads.AdsController.INSTANCE.maybeShowConsent(this);
            return;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!com.saigopal.browser.BuildConfig.LEGACY_SHELL) {
            com.saigopal.browser.ui.shell.ShellActivityHolder.INSTANCE.post(intent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!com.saigopal.browser.BuildConfig.LEGACY_SHELL) {
            return;
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        kotlin.jvm.functions.Function1<Integer, kotlin.Unit> trimmer = com.saigopal.browser.ui.shell.ShellActivityHolder.INSTANCE.getMemoryTrimmer();
        if (trimmer != null) {
            trimmer.invoke(level);
        }
    }

    @Override
    public void onDestroy() {
        if (!com.saigopal.browser.BuildConfig.LEGACY_SHELL) {
            com.saigopal.browser.ui.shell.BrowserShellStore.INSTANCE.release(this);
            com.saigopal.browser.ui.screens.games.GamesHubStore.INSTANCE.release(this);
            super.onDestroy();
            return;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!com.saigopal.browser.BuildConfig.LEGACY_SHELL) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                Runnable opener = com.saigopal.browser.ui.shell.ShellActivityHolder.INSTANCE.getMenuOpener();
                if (opener != null) {
                    opener.run();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
    
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
        if(mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data != null) {
                // If there is not data, then we may have taken a photo
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }
}
