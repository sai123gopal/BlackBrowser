package com.saigopal.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.saigopal.browser.R;
import com.saigopal.browser.browser.AlbumController;
import com.saigopal.browser.browser.BrowserController;


class AlbumItem {

    private final Context context;
    private final AlbumController albumController;

    private View albumView;
    View getAlbumView() {
        return albumView;
    }

    private TextView albumTitle;
    void setAlbumTitle(String title) {
        albumTitle.setText(title);
    }

    private BrowserController browserController;
    void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    AlbumItem(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }

    @SuppressWarnings("SameReturnValue")
    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.whitelist_item, null, false);
        albumView.setOnClickListener(v -> {
            browserController.showAlbum(albumController);
            browserController.hideOverview();
        });
        albumView.setOnLongClickListener(v -> {
            browserController.removeAlbum(albumController);
            return true;
        });
        ImageView albumClose = albumView.findViewById(R.id.whitelist_item_cancel);
        albumClose.setVisibility(View.VISIBLE);
        albumClose.setImageResource(R.drawable.icon_close);
        albumTitle = albumView.findViewById(R.id.whitelist_item_domain);
        albumTitle.setText(context.getString(R.string.app_name));
        albumClose.setOnClickListener(v -> browserController.removeAlbum(albumController));
    }

    public void activate() {
        albumTitle.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
    }

    void deactivate() {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        @SuppressLint("Recycle")
        TypedArray arr = context.obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.textColorPrimary});
        int primaryColor = arr.getColor(0, -1);
        albumTitle.setTextColor(primaryColor);
    }
}
