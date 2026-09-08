package com.saigopal.browser.ui.design.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Debug-only host for the design-system gallery. Not shipped. */
class DesignGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ComponentGalleryScreen() }
    }
}
