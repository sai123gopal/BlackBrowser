package com.saigopal.browser.task;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.saigopal.browser.R;
import com.saigopal.browser.unit.BrowserUnit;
import com.saigopal.browser.unit.HelperUnit;
import com.saigopal.browser.view.Toasty;

import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.RequiresApi;


@SuppressLint("StaticFieldLeak")
public class ExportWhiteListTask extends AsyncTask<Void, Void, Boolean> {

    private final Context context;
    private BottomSheetDialog dialog;
    private String path;
    private final int table;

    public ExportWhiteListTask(Context context, int i) {
        this.context = context;
        this.dialog = null;
        this.path = null;
        this.table = i;
    }

    @Override
    protected void onPreExecute() {
        dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_progress, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(context.getString(R.string.toast_wait_a_minute));
        dialog.setContentView(dialogView);
        //noinspection ConstantConditions
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        switch (table) {
            case 0:
                path = BrowserUnit.exportWhitelist(context, 0);
                break;
            case 1:
                path = BrowserUnit.exportWhitelist(context, 1);
                break;
            case 3:
                path = BrowserUnit.exportWhitelist(context, 3);
                break;
            case 4:
                path = BrowserUnit.exportBookmarks(context);
                break;
            default:
                path = BrowserUnit.exportWhitelist(context, 2);
                break;
        }
        return !isCancelled() && path != null && !path.isEmpty();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        dialog.hide();
        dialog.dismiss();

        if (result) {
            Toasty.show(context, context.getString(R.string.toast_export_successful));
        } else {
            Toasty.show(context, R.string.toast_export_failed);
        }
    }
}
