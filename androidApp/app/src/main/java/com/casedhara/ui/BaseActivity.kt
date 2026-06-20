package com.casedhara.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Base activity — window background is controlled entirely by themes.xml
 * (Theme.CaseDhara sets android:windowBackground = @color/saffron).
 * Do NOT set any background here — it would override the theme and cause a flash.
 */
abstract class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed: window.setBackgroundDrawableResource(...) — was causing background flash
    }
}