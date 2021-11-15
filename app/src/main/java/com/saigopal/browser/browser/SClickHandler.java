package com.saigopal.browser.browser;

import android.os.Handler;
import android.os.Message;

import com.saigopal.browser.view.SWebView;


public class SClickHandler extends Handler {
    private final SWebView webView;

    public SClickHandler(SWebView webView) {
        super();
        this.webView = webView;
    }


    @SuppressWarnings("NullableProblems")
    @Override
    public void handleMessage(Message message) {
        super.handleMessage(message);
        webView.getBrowserController().onLongPress(message.getData().getString("url"));
    }
}
