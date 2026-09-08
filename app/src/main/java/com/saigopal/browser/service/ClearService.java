package com.saigopal.browser.service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import androidx.preference.PreferenceManager;

import com.saigopal.browser.R;
import com.saigopal.browser.unit.BrowserUnit;

public class ClearService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        System.exit(0); // For remove all WebView thread
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        clear();
        stopSelf();
        return START_STICKY;
    }

    private void clear() {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // P09 (9.8): the decision table moved to ClearPlanner so it is unit-tested; this service
        // only executes the ops. Behaviour (flag keys, semantics) is unchanged.
        com.saigopal.browser.domain.ClearPlanner.Flags flags = new com.saigopal.browser.domain.ClearPlanner.Flags(
            sp.getBoolean(getString(R.string.sp_clear_cache), false),
            sp.getBoolean(getString(R.string.sp_clear_cookie), false),
            sp.getBoolean(getString(R.string.sp_clear_history), false),
            sp.getBoolean(("sp_clearIndexedDB"), false)
        );

        for (com.saigopal.browser.domain.ClearPlanner.Op op : com.saigopal.browser.domain.ClearPlanner.INSTANCE.plan(flags)) {
            if (op instanceof com.saigopal.browser.domain.ClearPlanner.Op.ClearCache) {
                BrowserUnit.clearCache(this);
            } else if (op instanceof com.saigopal.browser.domain.ClearPlanner.Op.ClearCookies) {
                BrowserUnit.clearCookie();
            } else if (op instanceof com.saigopal.browser.domain.ClearPlanner.Op.ClearHistory) {
                BrowserUnit.clearHistory(this);
            } else if (op instanceof com.saigopal.browser.domain.ClearPlanner.Op.ClearIndexedDb) {
                BrowserUnit.clearIndexedDB(this);
            }
        }
    }
}
