package com.saigopal.browser.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Message;

import androidx.preference.PreferenceManager;

import android.util.AttributeSet;
import android.view.*;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.saigopal.browser.R;
import com.saigopal.browser.browser.AlbumController;
import com.saigopal.browser.browser.BrowserController;
import com.saigopal.browser.browser.Javascript;
import com.saigopal.browser.browser.SClickHandler;
import com.saigopal.browser.browser.SDownloadListener;
import com.saigopal.browser.browser.SGestureListener;
import com.saigopal.browser.browser.SWebChromeClient;
import com.saigopal.browser.browser.SWebViewClient;
import com.saigopal.browser.browser.Remote;
import com.saigopal.browser.unit.BrowserUnit;
import com.saigopal.browser.unit.HelperUnit;


import java.util.HashMap;
import java.util.Objects;


public class SWebView extends WebView implements AlbumController {

    private OnScrollChangeListener onScrollChangeListener;
    public SWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }



    @Override
    public void onScrollChanged(int l, int t, int old_l, int old_t) {
        super.onScrollChanged(l, t, old_l, old_t);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(t, old_t);
        }
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public interface OnScrollChangeListener {

        void onScrollChange(int scrollY, int oldScrollY);
    }

    private Context context;

    private AlbumItem album;
    private SWebViewClient webViewClient;
    private SWebChromeClient webChromeClient;
    private SDownloadListener downloadListener;
    private SClickHandler clickHandler;
    private GestureDetector gestureDetector;

    private Javascript javaHosts;
    private Remote remoteHosts;
    private SharedPreferences sp;
    private WebSettings webSettings;

    private boolean foreground;

    public boolean isForeground() {
        return foreground;
    }

    private BrowserController browserController = null;

    public BrowserController getBrowserController() {
        return browserController;
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public SWebView(Context context) {
        super(context); // Cannot create a dialog, the WebView context is not an activity

        this.context = context;
        this.foreground = false;

        this.javaHosts = new Javascript(this.context);
        this.remoteHosts = new Remote(this.context);
        this.album = new AlbumItem(this.context, this, this.browserController);
        this.webViewClient = new SWebViewClient(this);
        this.webChromeClient = new SWebChromeClient(this);
        this.downloadListener = new SDownloadListener(this.context);
        this.clickHandler = new SClickHandler(this);
        this.gestureDetector = new GestureDetector(context, new SGestureListener(this));

        initWebView();
        initPreferences();
        initAlbum();
    }

    @SuppressWarnings("SameReturnValue")
    @SuppressLint("ClickableViewAccessibility")
    private synchronized void initWebView() {
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
        setOnTouchListener((view, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            return false;
        });
    }

    @TargetApi(Build.VERSION_CODES.O)
    public synchronized void initPreferences() {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String userAgent = sp.getString("userAgent", "");
        webSettings = getSettings();

        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        if (!userAgent.isEmpty()) {
            webSettings.setUserAgentString(userAgent);
        }

        webViewClient.enableAdBlock(sp.getBoolean(context.getString(R.string.sp_ad_block), true));
        webSettings.setTextZoom(Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));
        webSettings.setAllowFileAccessFromFileURLs(sp.getBoolean(("sp_remote"), false));
        webSettings.setAllowUniversalAccessFromFileURLs(sp.getBoolean(("sp_remote"), false));
        webSettings.setDomStorageEnabled(sp.getBoolean(("sp_remote"), false));
        webSettings.setBlockNetworkImage(!sp.getBoolean(context.getString(R.string.sp_images), true));
        webSettings.setJavaScriptEnabled(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setJavaScriptCanOpenWindowsAutomatically(sp.getBoolean(context.getString(R.string.sp_javascript), true));
        webSettings.setGeolocationEnabled(sp.getBoolean(context.getString(R.string.sp_location), false));
    }

    private synchronized void initAlbum() {
        album.setAlbumTitle(context.getString(R.string.app_name));
        album.setBrowserController(browserController);
    }

    public synchronized HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        if (sp.getBoolean(context.getString(R.string.sp_savedata), false)) {
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public synchronized void loadUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            Toasty.show(context, R.string.toast_load_error);
            return;
        }

        setDesktopMode(this,true,url);

        HelperUnit.initRendering(this);

        if (javaHosts.isWhite(url) || sp.getBoolean(context.getString(R.string.sp_javascript), true)) {
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setJavaScriptEnabled(true);
        } else {
            webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
            webSettings.setJavaScriptEnabled(false);
        }
        if (remoteHosts.isWhite(url) || sp.getBoolean("sp_remote", true)) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
            webSettings.setDomStorageEnabled(true);
        } else {
            webSettings.setAllowFileAccessFromFileURLs(false);
            webSettings.setAllowUniversalAccessFromFileURLs(false);
            webSettings.setDomStorageEnabled(false);
        }

        super.loadUrl(BrowserUnit.queryWrapper(context, url.trim()), getRequestHeaders());
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    public void setAlbumTitle(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        foreground = true;
        album.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        foreground = false;
        album.deactivate();
    }

    public synchronized void update(int progress) {
        if (foreground) {
            browserController.updateProgress(progress);
        }
        if (isLoadFinish()) {
            browserController.updateAutoComplete();
        }
    }

    public synchronized void update(String title) {
        album.setAlbumTitle(title);
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public void onLongPress() {
        Message click = clickHandler.obtainMessage();
        click.setTarget(clickHandler);
        requestFocusNodeHref(click);
    }


    public static void setDesktopMode(WebView webView,boolean enabled, String url) {

        SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(webView.getContext());
        if (sh.getBoolean("desktop_mode",false)) {

            if (!url.contains("Newtab.html")) {
                final WebSettings webSettings = webView.getSettings();

                webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36");
                webSettings.setUseWideViewPort(enabled);
                webSettings.setLoadWithOverviewMode(enabled);
                webSettings.setSupportZoom(enabled);
                webSettings.setBuiltInZoomControls(enabled);

            }
        }

    }
}
