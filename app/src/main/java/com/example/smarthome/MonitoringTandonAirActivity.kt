package com.example.smarthome

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityMonitoringTandonAirBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MonitoringTandonAirActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitoringTandonAirBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebase: FirebaseDatabase
    companion object {
        private const val PREFS_NAME = "water_states"
        private const val KEY_AIR_STATUS = "air_status"
        private const val MAX_WATER_HEIGHT_CM = 50.0
        private const val MIN_WATER_HEIGHT_CM = 0.0
        private const val MAX_IMAGE_HEIGHT_DP = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringTandonAirBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        firebase = FirebaseDatabase.getInstance()
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        setupFirebaseListener()
        restoreWaterToggleState()
        setupToggleListener()
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupFirebaseListener() {
        firebase.reference.child("IoTSystem").child("TandonAir")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        android.util.Log.d("FirebaseData", "Snapshot exists: ${snapshot.exists()}")
                        android.util.Log.d("FirebaseData", "Full data: ${snapshot.value}")
                        android.util.Log.d("FirebaseData", "tinggiAir_cm raw: ${snapshot.child("tinggiAir_cm").value}")
                        android.util.Log.d("FirebaseData", "pompa raw: ${snapshot.child("pompa").value}")
                        val tinggiAirRaw = snapshot.child("tinggiAir_cm").value
                        val tinggiAirCm = when (tinggiAirRaw) {
                            is Double -> tinggiAirRaw
                            is Float -> tinggiAirRaw.toDouble()
                            is Long -> tinggiAirRaw.toDouble()
                            is Int -> tinggiAirRaw.toDouble()
                            is String -> tinggiAirRaw.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                        val pompStatus = snapshot.child("pompa").getValue(String::class.java) ?: "OFF"
                        android.util.Log.d("FirebaseData", "Processed tinggiAirCm: $tinggiAirCm")
                        android.util.Log.d("FirebaseData", "Processed pompStatus: $pompStatus")
                        updateWaterLevelUI(tinggiAirCm)
                        updatePumpStatusFromFirebase(pompStatus)
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseError", "Error processing data", e)
                        e.printStackTrace()
                        updateWaterLevelUI(0.0)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("FirebaseError", "Database error: ${error.message}")
                    updateWaterLevelUI(0.0)
                }
            })
    }

    private fun updateWaterLevelUI(tinggiAirCm: Double) {
        val percentage = calculateWaterPercentage(tinggiAirCm)
        val waterLevelText = findViewById<TextView>(R.id.water_level_text)
        waterLevelText.text = "${percentage.toInt()}%"
        updateAirLevelImageHeight(percentage)
    }

    private fun calculateWaterPercentage(tinggiAirCm: Double): Double {
        val clampedHeight = tinggiAirCm.coerceIn(MIN_WATER_HEIGHT_CM, MAX_WATER_HEIGHT_CM)
        return (clampedHeight / MAX_WATER_HEIGHT_CM) * 100.0
    }

    private fun updateAirLevelImageHeight(percentage: Double) {
        val airLevelImg = findViewById<ImageView>(R.id.air_level_img)
        val maxHeightPx = (MAX_IMAGE_HEIGHT_DP * resources.displayMetrics.density).toInt()
        val minPercentage = 5.0
        val adjustedPercentage = percentage.coerceAtLeast(minPercentage)
        val newHeight = (maxHeightPx * (adjustedPercentage / 100.0)).toInt()
        val oldHeight = airLevelImg.height
        val animator = android.animation.ValueAnimator.ofInt(oldHeight, newHeight)
        animator.duration = 500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            val layoutParams = airLevelImg.layoutParams
            layoutParams.height = animatedValue
            airLevelImg.layoutParams = layoutParams
            airLevelImg.requestLayout()
        }
        animator.start()
    }

    private fun updatePumpStatusFromFirebase(pompStatus: String) {
        val isOn = pompStatus.equals("ON", ignoreCase = true)
        if (isOn) {
            setToggleToOnState()
        } else {
            setToggleToOffState()
        }
        saveWaterToggleState(isOn)
    }

    private fun updateFirebasePumpStatus(isOn: Boolean) {
        val status = if (isOn) "ON" else "OFF"
        firebase.reference.child("IoTSystem").child("TandonAir").child("pompa").setValue(status)
    }

    private fun setupToggleListener() {
        findViewById<LinearLayout>(R.id.toggle_air_btn).setOnClickListener {
            toggleWaterWithAnimation(R.id.air_btn, R.id.toggle_air_btn)
        }
    }

    private fun toggleWaterWithAnimation(containerID: Int, toggleId: Int) {
        val airContainer = findViewById<LinearLayout>(containerID)
        val toggleSwitch = findViewById<LinearLayout>(toggleId)
        var toggleText: TextView? = null
        var toggleThumb: View? = null
        for (i in 0 until toggleSwitch.childCount) {
            val child = toggleSwitch.getChildAt(i)
            if (child is TextView) {
                toggleText = child
            } else {
                toggleThumb = child
            }
        }
        if (toggleText == null || toggleThumb == null) {
            return
        }
        val isCurrentlyOn = toggleText.text.toString().equals("On", ignoreCase = true)
        toggleSwitch.isEnabled = false
        if (isCurrentlyOn) {
            animateToggleToOff(airContainer, toggleSwitch, toggleText, toggleThumb)
        } else {
            animateToggleToOn(airContainer, toggleSwitch, toggleText, toggleThumb)
        }
    }

    private fun animateToggleToOff(
        airContainer: LinearLayout,
        toggleSwitch: LinearLayout,
        toggleText: TextView,
        toggleThumb: View
    ) {
        toggleSwitch.isEnabled = false
        val slideAnimator = ObjectAnimator.ofFloat(toggleThumb, "translationY", 0f, -90f)
        slideAnimator.duration = 600
        slideAnimator.interpolator = DecelerateInterpolator()
        slideAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                toggleThumb.translationY = 0f
                airContainer.setBackgroundResource(R.drawable.bg_off)
                toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_off)
                toggleSwitch.removeAllViews()
                val newThumbView = View(this@MonitoringTandonAirActivity)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    25.dpToPx()
                )
                layoutParams.setMargins(0, 0, 0, 4.dpToPx())
                newThumbView.layoutParams = layoutParams
                newThumbView.setBackgroundResource(R.drawable.toggle_thumb_off)
                toggleText.text = "Off"
                toggleText.setTextColor(resources.getColor(android.R.color.white, null))
                val textLayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                toggleText.layoutParams = textLayoutParams
                toggleSwitch.addView(newThumbView)
                toggleSwitch.addView(toggleText)
                updateContainerTextColors(airContainer, false)
                saveWaterToggleState(false)
                updateFirebasePumpStatus(false)
                toggleSwitch.isEnabled = true
            }
        })
        slideAnimator.start()
    }

    private fun animateToggleToOn(
        airContainer: LinearLayout,
        toggleSwitch: LinearLayout,
        toggleText: TextView,
        toggleThumb: View
    ) {
        toggleSwitch.isEnabled = false
        val slideAnimator = ObjectAnimator.ofFloat(toggleThumb, "translationY", 0f, 90f)
        slideAnimator.duration = 600
        slideAnimator.interpolator = DecelerateInterpolator()

        slideAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                toggleThumb.translationY = 0f
                airContainer.setBackgroundResource(R.drawable.bg_on)
                toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_on)
                toggleSwitch.removeAllViews()
                toggleText.text = "On"
                toggleText.setTextColor(resources.getColor(android.R.color.white, null))
                val textLayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                toggleText.layoutParams = textLayoutParams
                val newThumbView = View(this@MonitoringTandonAirActivity)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    25.dpToPx()
                )
                layoutParams.setMargins(0, 0, 0, 4.dpToPx())
                newThumbView.layoutParams = layoutParams
                newThumbView.setBackgroundResource(R.drawable.toggle_thumb_on)
                toggleSwitch.addView(toggleText)
                toggleSwitch.addView(newThumbView)
                updateContainerTextColors(airContainer, true)
                saveWaterToggleState(true)
                updateFirebasePumpStatus(true)
                toggleSwitch.isEnabled = true
            }
        })

        slideAnimator.start()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun updateContainerTextColors(airContainer: LinearLayout, isOn: Boolean) {
        for (i in 0 until airContainer.childCount) {
            val child = airContainer.getChildAt(i)
            if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL && child.childCount >= 2) {
                for (j in 0 until child.childCount) {
                    val textView = child.getChildAt(j)
                    if (textView is TextView) {
                        if (isOn) {
                            textView.setTextColor(resources.getColor(android.R.color.white, null))
                            if (j == 1) {
                                textView.alpha = 0.8f
                                textView.text = "Keran air hidup"
                            }
                        } else {
                            textView.setTextColor(resources.getColor(android.R.color.black, null))
                            textView.alpha = 1.0f
                            if (j == 1) {
                                textView.text = "Keran air mati"
                            }
                        }
                    }
                }
                break
            }
        }
    }

    private fun saveWaterToggleState(isOn: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_AIR_STATUS, isOn)
            apply()
        }
    }

    private fun restoreWaterToggleState() {
        val isOn = sharedPreferences.getBoolean(KEY_AIR_STATUS, false)
        if (isOn) {
            setToggleToOnState()
        } else {
            setToggleToOffState()
        }
    }

    private fun setToggleToOnState() {
        val airContainer = findViewById<LinearLayout>(R.id.air_btn)
        val toggleSwitch = findViewById<LinearLayout>(R.id.toggle_air_btn)
        airContainer.setBackgroundResource(R.drawable.bg_on)
        toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_on)
        toggleSwitch.removeAllViews()
        val toggleText = TextView(this)
        toggleText.text = "On"
        toggleText.setTextColor(resources.getColor(android.R.color.white, null))
        toggleText.textSize = 12f
        toggleText.gravity = android.view.Gravity.CENTER
        toggleText.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        val textLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        toggleText.layoutParams = textLayoutParams
        val thumbView = View(this)
        val thumbLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            25.dpToPx()
        )
        thumbLayoutParams.setMargins(0, 0, 0, 4.dpToPx())
        thumbView.layoutParams = thumbLayoutParams
        thumbView.setBackgroundResource(R.drawable.toggle_thumb_on)
        toggleSwitch.addView(toggleText)
        toggleSwitch.addView(thumbView)
        updateContainerTextColors(airContainer, true)
    }

    private fun setToggleToOffState() {
        val airContainer = findViewById<LinearLayout>(R.id.air_btn)
        val toggleSwitch = findViewById<LinearLayout>(R.id.toggle_air_btn)
        airContainer.setBackgroundResource(R.drawable.bg_off)
        toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_off)
        toggleSwitch.removeAllViews()
        val thumbView = View(this)
        val thumbLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            25.dpToPx()
        )
        thumbLayoutParams.setMargins(0, 0, 0, 4.dpToPx())
        thumbView.layoutParams = thumbLayoutParams
        thumbView.setBackgroundResource(R.drawable.toggle_thumb_off)
        val toggleText = TextView(this)
        toggleText.text = "Off"
        toggleText.setTextColor(resources.getColor(android.R.color.white, null))
        toggleText.textSize = 12f
        toggleText.gravity = android.view.Gravity.CENTER
        toggleText.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        val textLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        toggleText.layoutParams = textLayoutParams
        toggleSwitch.addView(thumbView)
        toggleSwitch.addView(toggleText)
        updateContainerTextColors(airContainer, false)
    }
}

