package com.saigopal.browser.browser;

import android.net.Uri;
import android.view.View;
import android.webkit.*;

import com.saigopal.browser.view.SWebView;


public class SWebChromeClient extends WebChromeClient {

    private final SWebView sWebView;

    public SWebChromeClient(SWebView sWebView) {
        super();
        this.sWebView = sWebView;
    }

    /**
     * Views outside the browser shell (the games hub hosts its own SWebView with no
     * BrowserController) must not crash on engine callbacks — every controller hand-off
     * below is null-guarded.
     */
    private BrowserController controller() {
        return sWebView.getBrowserController();
    }


    @Override
    public void onProgressChanged(WebView view, int progress) {
        super.onProgressChanged(view, progress);
        sWebView.update(progress);
        String title = view.getTitle();
        if (title == null || title.isEmpty()) {
            sWebView.update(view.getUrl());
        } else {
            sWebView.update(title);
        }
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        BrowserController controller = controller();
        if (controller == null) return;
        controller.onShowCustomView(view, callback);
        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        BrowserController controller = controller();
        if (controller == null) return;
        controller.onHideCustomView();
        super.onHideCustomView();
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
        BrowserController controller = controller();
        if (controller == null) return false; // unhandled: the system falls back to no chooser
        controller.showFileChooser(filePathCallback);
        return true;
    }

    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
        // P09 (B2): grants are the controller's decision (shell prompt / legacy auto-grant), never silent.
        BrowserController controller = controller();
        if (controller == null) {
            callback.invoke(origin, false, false); // no shell to ask: deny rather than crash or auto-grant
            return;
        }
        controller.onGeolocationPrompt(origin, callback);
        super.onGeolocationPermissionsShowPrompt(origin, callback);
    }
}
