package com.saigopal.browser.browser;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.saigopal.browser.view.SWebView;


public class SGestureListener extends GestureDetector.SimpleOnGestureListener {
    private final SWebView webView;
    private boolean longPress = true;

    public SGestureListener(SWebView webView) {
        super();
        this.webView = webView;
    }



    @Override
    public void onLongPress(MotionEvent e) {
        if (longPress) {
            webView.onLongPress();
        }
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        longPress = false;
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        longPress = true;
    }
}
