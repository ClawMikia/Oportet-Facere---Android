package com.reqsync.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.reqsync.app.ui.onboarding.OnboardingActivity
import com.reqsync.app.ui.MainActivity
import com.reqsync.app.utils.PreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val prefs = PreferencesDataStore(this@SplashActivity)
            val onboardingDone = prefs.onboardingDone.first()
            val destination = if (onboardingDone) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, OnboardingActivity::class.java)
            }
            startActivity(destination)
            finish()
        }
    }
}
