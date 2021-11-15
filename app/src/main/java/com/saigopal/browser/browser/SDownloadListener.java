package com.saigopal.browser.browser;

import android.content.Context;
import android.webkit.DownloadListener;

import com.saigopal.browser.unit.BrowserUnit;

public class SDownloadListener implements DownloadListener {
    private final Context context;

    public SDownloadListener(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
        BrowserUnit.download(context, url, contentDisposition, mimeType);
    }
}
