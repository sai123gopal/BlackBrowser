package com.saigopal.browser.browser;

import android.net.Uri;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.AutoCompleteTextView;

public interface BrowserController {
    void updateAutoComplete();
    void updateProgress(int progress);
    void showAlbum(AlbumController albumController);
    void removeAlbum(AlbumController albumController);
    void showFileChooser(ValueCallback<Uri[]> filePathCallback);
    void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback);
    void onLongPress(String url);
    void hideOverview ();
    void onHideCustomView();

    /** P09 (B2): the shell decides geolocation grants via a user prompt; no more silent auto-grant. */
    void onGeolocationPrompt(String origin, GeolocationPermissions.Callback callback);
}