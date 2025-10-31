package com.example.smarthome

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SpriteScreenActivity : AppCompatActivity() {
    private lateinit var logoImageView: ImageView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var byText: TextView
    private lateinit var universityLayout: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sprite_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initViews()
        startSmartAnimations()
    }

    private fun initViews() {
        logoImageView = findViewById(R.id.logoImageView)
        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        byText = findViewById(R.id.byText)
        universityLayout = findViewById(R.id.universityLayout)
        logoImageView.translationY = -800f
        titleText.translationY = 600f
        titleText.alpha = 0f
        subtitleText.translationY = 600f
        subtitleText.alpha = 0f
        byText.translationY = 600f
        byText.alpha = 0f
        universityLayout.translationY = 600f
        universityLayout.alpha = 0f
    }

    private fun startSmartAnimations() {
        val logoDropAnimation = ObjectAnimator.ofFloat(logoImageView, "translationY", -800f, 120f).apply {
            duration = 900
            interpolator = BounceInterpolator()
        }
        val logoScaleDownDrop = ObjectAnimator.ofFloat(logoImageView, "scaleX", 0.3f, 1.1f).apply {
            duration = 900
            interpolator = OvershootInterpolator()
        }
        val logoScaleDownDropY = ObjectAnimator.ofFloat(logoImageView, "scaleY", 0.3f, 1.1f).apply {
            duration = 900
            interpolator = OvershootInterpolator()
        }
        val logoRotateRight = ObjectAnimator.ofFloat(logoImageView, "rotationY", 0f, 180f).apply {
            duration = 700
            interpolator = AnticipateOvershootInterpolator()
        }
        val logoRotateLeft = ObjectAnimator.ofFloat(logoImageView, "rotationY", 180f, 0f).apply {
            duration = 700
            interpolator = AnticipateOvershootInterpolator()
        }
        val logoMoveUp = ObjectAnimator.ofFloat(logoImageView, "translationY", 120f, -60f).apply {
            duration = 800
            interpolator = OvershootInterpolator(1.5f)
        }
        val logoScaleBackX = ObjectAnimator.ofFloat(logoImageView, "scaleX", 1.1f, 1.0f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }
        val logoScaleBackY = ObjectAnimator.ofFloat(logoImageView, "scaleY", 1.1f, 1.0f).apply {
            duration = 800
            interpolator = DecelerateInterpolator()
        }
        val titleSlideUp = ObjectAnimator.ofFloat(titleText, "translationY", 600f, 0f).apply {
            duration = 700
            interpolator = OvershootInterpolator(0.8f)
            startDelay = 0
        }
        val titleFadeIn = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            startDelay = 0
        }
        val titleScale = ObjectAnimator.ofFloat(titleText, "scaleX", 0.8f, 1.0f).apply {
            duration = 700
            interpolator = OvershootInterpolator()
            startDelay = 0
        }
        val titleScaleY = ObjectAnimator.ofFloat(titleText, "scaleY", 0.8f, 1.0f).apply {
            duration = 700
            interpolator = OvershootInterpolator()
            startDelay = 0
        }
        val subtitleSlideUp = ObjectAnimator.ofFloat(subtitleText, "translationY", 600f, 0f).apply {
            duration = 700
            interpolator = OvershootInterpolator(0.8f)
            startDelay = 100
        }
        val subtitleFadeIn = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            startDelay = 100
        }
        val bySlideUp = ObjectAnimator.ofFloat(byText, "translationY", 600f, 0f).apply {
            duration = 700
            interpolator = OvershootInterpolator(0.8f)
            startDelay = 200
        }
        val byFadeIn = ObjectAnimator.ofFloat(byText, "alpha", 0f, 1f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            startDelay = 200
        }
        val universitySlideUp = ObjectAnimator.ofFloat(universityLayout, "translationY", 600f, 0f).apply {
            duration = 700
            interpolator = OvershootInterpolator(0.8f)
            startDelay = 300
        }
        val universityFadeIn = ObjectAnimator.ofFloat(universityLayout, "alpha", 0f, 1f).apply {
            duration = 700
            interpolator = DecelerateInterpolator()
            startDelay = 300
        }
        val logoDropSet = AnimatorSet().apply {
            playTogether(logoDropAnimation, logoScaleDownDrop, logoScaleDownDropY)
        }
        val logoRotationSet = AnimatorSet().apply {
            playSequentially(logoRotateRight, logoRotateLeft)
        }
        val logoFinalSet = AnimatorSet().apply {
            playTogether(logoMoveUp, logoScaleBackX, logoScaleBackY)
        }
        val textAnimationSet = AnimatorSet().apply {
            playTogether(
                titleSlideUp, titleFadeIn, titleScale, titleScaleY,
                subtitleSlideUp, subtitleFadeIn,
                bySlideUp, byFadeIn,
                universitySlideUp, universityFadeIn
            )
        }
        val logoSequence = AnimatorSet().apply {
            playSequentially(logoDropSet, logoRotationSet)
        }
        val finalSmartSet = AnimatorSet().apply {
            playTogether(logoFinalSet, textAnimationSet)
        }
        val completeSmartAnimation = AnimatorSet().apply {
            playSequentially(logoSequence, finalSmartSet)
        }
        completeSmartAnimation.start()
        val totalAnimationTime = 900 + 700 + 700 + 800
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }, (totalAnimationTime + 2500).toLong())
    }
}