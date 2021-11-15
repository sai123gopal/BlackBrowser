package com.saigopal.browser.browser;

import android.app.Activity;
import android.net.Uri;
import android.view.View;
import android.webkit.*;

import com.saigopal.browser.unit.HelperUnit;
import com.saigopal.browser.view.SWebView;


public class SWebChromeClient extends WebChromeClient {

    private final SWebView sWebView;

    public SWebChromeClient(SWebView sWebView) {
        super();
        this.sWebView = sWebView;
    }



    @Override
    public void onProgressChanged(WebView view, int progress) {
        super.onProgressChanged(view, progress);
        sWebView.update(progress);
        if (view.getTitle().isEmpty()) {
            sWebView.update(view.getUrl());
        } else {
            sWebView.update(view.getTitle());
        }
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        sWebView.getBrowserController().onShowCustomView(view, callback);
        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        sWebView.getBrowserController().onHideCustomView();
        super.onHideCustomView();
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        sWebView.getBrowserController().showFileChooser(filePathCallback);
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        Activity activity =  (Activity) sWebView.getContext();
        HelperUnit.grantPermissionsLoc(activity);
        callback.invoke(origin, true, false);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }
}
