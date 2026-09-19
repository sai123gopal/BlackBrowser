package com.saigopal.browser.browser;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;

import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.saigopal.browser.R;
import com.saigopal.browser.database.Record;
import com.saigopal.browser.database.RecordAction;
import com.saigopal.browser.unit.BrowserUnit;
import com.saigopal.browser.unit.HelperUnit;
import com.saigopal.browser.unit.RecordUnit;
import com.saigopal.browser.view.Toasty;
import com.saigopal.browser.view.SWebView;

import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;

public class SWebViewClient extends WebViewClient {

    private final SWebView sWebView;
    private final Context context;
    private final SharedPreferences sp;
    private final AdBlock adBlock;
    private final Cookie cookie;

    private final boolean white;
    private boolean enable;
    public void enableAdBlock(boolean enable) {
        this.enable = enable;
    }

    public AdBlock getAdBlock() {
        return adBlock;
    }

    public SWebViewClient(SWebView sWebView) {
        super();
        this.sWebView = sWebView;
        this.context = sWebView.getContext();
        this.sp = PreferenceManager.getDefaultSharedPreferences(context);
        this.adBlock = new AdBlock(this.context);
        this.cookie = new Cookie(this.context);
        this.white = false;
        this.enable = true;
    }


    private volatile String currentPageUrl = "";

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        this.currentPageUrl = (url != null) ? url : "";
        sWebView.setCurrentUrl(this.currentPageUrl);
        try {
            String orig = view.getOriginalUrl();
            if (orig != null) {
                sWebView.setSafeOriginalUrl(orig);
            }
        } catch (Throwable ignored) {}
        sWebView.update(15);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (url != null && !url.isEmpty()) {
            this.currentPageUrl = url;
            sWebView.setCurrentUrl(url);
            try {
                String orig = view.getOriginalUrl();
                if (orig != null) {
                    sWebView.setSafeOriginalUrl(orig);
                }
            } catch (Throwable ignored) {}
        }
        sWebView.update(100);

        boolean isInco = sp.getBoolean("IsIncognito",false);

        if (!isInco)
        {
            if (sp.getBoolean("saveHistory", true)) {
                RecordAction action = new RecordAction(context);
                action.open(true);
                if (action.checkUrl(url, RecordUnit.TABLE_HISTORY)) {
                    action.deleteURL(url, RecordUnit.TABLE_HISTORY);
                }
                action.addHistory(new Record(sWebView.getTitle(), url, System.currentTimeMillis(), 0));
                action.close();
            }
        }

            if (sWebView.isForeground()) {
                sWebView.invalidate();
            } else {
                sWebView.postInvalidate();
            }

    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
        super.doUpdateVisitedHistory(view, url, isReload);
        if (url != null && !url.isEmpty()) {
            this.currentPageUrl = url;
            sWebView.setCurrentUrl(url);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        final Uri uri = Uri.parse(url);
        return handleUri(view, uri);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        final Uri uri = request.getUrl();
        return handleUri(view, uri);
    }

    private boolean handleUri(WebView webView, final Uri uri) {

        String url = uri.toString();
        Uri parsedUri = Uri.parse(url);
        PackageManager packageManager = context.getPackageManager();
        Intent browseIntent = new Intent(Intent.ACTION_VIEW).setData(parsedUri);

        CookieManager manager = CookieManager.getInstance();

        if (com.saigopal.browser.BuildConfig.LEGACY_SHELL) {
            SWebView.setDesktopMode(webView, true, currentPageUrl);
        }

        if (sp.getBoolean("IsIncognito",false)){
            manager.setAcceptCookie(false);
        } else if (cookie.isWhite(url) || sp.getBoolean(context.getString(R.string.sp_cookies), true)) {
            manager.setAcceptCookie(true);
            manager.getCookie(url);
        }else {
            manager.setAcceptCookie(false);
        }

        if (url.startsWith("http")) {
            webView.loadUrl(url, sWebView.getRequestHeaders());
            return true;
        }

        if (browseIntent.resolveActivity(packageManager) != null) {
            context.startActivity(browseIntent);
            return true;
        }


        if (url.startsWith("intent:")) {
            try {
                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    try {
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toasty.show(context, R.string.toast_load_error);
                    }
                    return true;
                }
                //try to find fallback url
                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                if (fallbackUrl != null) {
                    webView.loadUrl(fallbackUrl);
                    return true;
                }
                //invite to install
                Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + intent.getPackage()));
                if (marketIntent.resolveActivity(packageManager) != null) {
                    context.startActivity(marketIntent);
                    return true;
                }
            } catch (URISyntaxException e) {
                //not an intent uri
                return false;
            }
        }
        return true;//do nothing in other cases
    }

    private static final WebResourceResponse EMPTY_RESPONSE = new WebResourceResponse(
            BrowserUnit.MIME_TYPE_TEXT_PLAIN,
            BrowserUnit.URL_ENCODING,
            new ByteArrayInputStream(new byte[0])
    );

    private boolean isGamezopContext(String pageUrl, String requestUrl) {
        if (pageUrl != null && pageUrl.toLowerCase().contains("gamezop.com")) {
            return true;
        }
        if (requestUrl != null && requestUrl.toLowerCase().contains("gamezop.com")) {
            return true;
        }
        String current = sWebView.getCurrentUrl();
        if (current != null && current.toLowerCase().contains("gamezop.com")) {
            return true;
        }
        String orig = sWebView.getSafeOriginalUrl();
        if (orig != null && orig.toLowerCase().contains("gamezop.com")) {
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (url == null) {
            return super.shouldInterceptRequest(view, url);
        }
        String pageUrl = this.currentPageUrl;
        if (pageUrl == null || pageUrl.isEmpty()) {
            pageUrl = sWebView.getCurrentUrl();
        }
        if (isGamezopContext(pageUrl, url)) {
            return super.shouldInterceptRequest(view, url);
        }
        if (enable && !white && sp.getBoolean(context.getString(R.string.sp_ad_block), true)) {
            if (pageUrl != null && !pageUrl.isEmpty() && adBlock.isWhite(pageUrl)) {
                return super.shouldInterceptRequest(view, url);
            }
            if (adBlock.isAd(url)) {
                sWebView.incrementBlockedAdsCount();
                synchronized (SWebViewClient.class) {
                    int allTime = sp.getInt("stats_ads_blocked", 0);
                    sp.edit().putInt("stats_ads_blocked", allTime + 1).apply();
                }
                return EMPTY_RESPONSE;
            }
        }
        return super.shouldInterceptRequest(view, url);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request == null || request.getUrl() == null) {
            return super.shouldInterceptRequest(view, request);
        }

        // Never intercept or block the main document itself
        if (request.isForMainFrame()) {
            String mainUrl = request.getUrl().toString();
            this.currentPageUrl = mainUrl;
            sWebView.setCurrentUrl(mainUrl);
            return super.shouldInterceptRequest(view, request);
        }

        String reqUrl = request.getUrl().toString();
        String pageUrl = this.currentPageUrl;
        if (pageUrl == null || pageUrl.isEmpty()) {
            pageUrl = sWebView.getCurrentUrl();
        }
        if (isGamezopContext(pageUrl, reqUrl)) {
            return super.shouldInterceptRequest(view, request);
        }

        if (enable && !white && sp.getBoolean(context.getString(R.string.sp_ad_block), true)) {
            if (pageUrl != null && !pageUrl.isEmpty() && adBlock.isWhite(pageUrl)) {
                return super.shouldInterceptRequest(view, request);
            }
            if (adBlock.isAd(reqUrl)) {
                sWebView.incrementBlockedAdsCount();
                synchronized (SWebViewClient.class) {
                    int allTime = sp.getInt("stats_ads_blocked", 0);
                    sp.edit().putInt("stats_ads_blocked", allTime + 1).apply();
                }
                return EMPTY_RESPONSE;
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message doNotResend, final Message resend) {
        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_action, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(R.string.dialog_content_resubmission);
        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(view1 -> {
            resend.sendToTarget();
            dialog.cancel();
        });
        dialog.setOnCancelListener(dialog1 -> {
            doNotResend.sendToTarget();
            dialog1.cancel();
        });
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

        String message = "\"SSL Certificate error.\"";
        switch (error.getPrimaryError()) {
            case SslError.SSL_UNTRUSTED:
                message = "\"Certificate authority is not trusted.\"";
                break;
            case SslError.SSL_EXPIRED:
                message = "\"Certificate has expired.\"";
                break;
            case SslError.SSL_IDMISMATCH:
                message = "\"Certificate Hostname mismatch.\"";
                break;
            case SslError.SSL_NOTYETVALID:
                message = "\"Certificate is not yet valid.\"";
                break;
            case SslError.SSL_DATE_INVALID:
                message = "\"Certificate date is invalid.\"";
                break;
            case SslError.SSL_INVALID:
                message = "\"Certificate is invalid.\"";
                break;
        }
        String text = message + " - " + context.getString(R.string.dialog_content_ssl_error);

        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_action, null);
        TextView textView = dialogView.findViewById(R.id.dialog_text);
        textView.setText(text);
        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(view1 -> {
            handler.proceed();
            dialog.cancel();
        });
        dialog.setOnCancelListener(dialog1 -> {
            handler.cancel();
            dialog1.cancel();
        });
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onReceivedHttpAuthRequest(WebView view, @NonNull final HttpAuthHandler handler, String host, String realm) {

        final BottomSheetDialog dialog = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_edit_title, null);

        final EditText pass_userNameET = dialogView.findViewById(R.id.edit_userName);
        final EditText pass_userPWET = dialogView.findViewById(R.id.edit_PW);
        pass_userNameET.setVisibility(View.VISIBLE);
        pass_userPWET.setVisibility(View.VISIBLE);

        TextInputLayout login_title = dialogView.findViewById(R.id.edit_title);
        login_title.setVisibility(View.GONE);
        ImageView ib_icon = dialogView.findViewById(R.id.edit_icon);
        ib_icon.setVisibility(View.GONE);

        Button action_ok = dialogView.findViewById(R.id.action_ok);
        action_ok.setOnClickListener(view1 -> {
            String user = pass_userNameET.getText().toString().trim();
            String pass = pass_userPWET.getText().toString().trim();
            handler.proceed(user, pass);
            dialog.cancel();
        });
        dialog.setOnCancelListener(dialog1 -> {
            handler.cancel();
            dialog1.cancel();
        });
        dialog.setContentView(dialogView);
        dialog.show();
        HelperUnit.setBottomSheetBehavior(dialog, dialogView, BottomSheetBehavior.STATE_EXPANDED);
    }

}
