package com.example.conneqt

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2000L
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContentView(R.layout.splash_screen)

        auth = FirebaseAuth.getInstance()

        // Animate center content in
        findViewById<LinearLayout>(R.id.splashCenterLayout)?.apply {
            alpha = 0f
            translationY = 40f
            animate().alpha(1f).translationY(0f).setDuration(800).setStartDelay(200).start()
        }

        // Animate bottom section in
        findViewById<LinearLayout>(R.id.splashBottomLayout)?.apply {
            alpha = 0f
            animate().alpha(1f).setDuration(600).setStartDelay(600).start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, SPLASH_DELAY)
    }

    private fun checkAuthAndNavigate() {
        val currentUser = auth.currentUser

        val destination = if (currentUser != null) {
            // Already logged in → go straight to Dashboard
            MainActivity::class.java
        } else {
            // Not logged in → go to Login (not Signup)
            // Professor can tap "Sign up" from there if needed
            LoginActivity::class.java
        }

        startActivity(Intent(this, destination))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish() // Remove splash from back stack entirely
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Block back press on splash screen
    }
}