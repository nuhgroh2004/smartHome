package com.example.smarthome

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityMonitoringTandonAirBinding

class MonitoringTandonAirActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitoringTandonAirBinding
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "water_states"
        private const val KEY_AIR_STATUS = "air_status"
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

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Restore state terakhir dari SharedPreferences
        restoreWaterToggleState()

        // Setup toggle listener untuk kontrol air
        setupToggleListener()

        // Setup back button
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // <---- Fungsi setupToggleListener untuk menginisialisasi click listener pada tombol toggle air ---->
    private fun setupToggleListener() {
        findViewById<LinearLayout>(R.id.toggle_air_btn).setOnClickListener {
            toggleWaterWithAnimation(R.id.air_btn, R.id.toggle_air_btn)
        }
    }

    // <---- Fungsi toggleWaterWithAnimation untuk mendeteksi status toggle saat ini dan mengarahkan ke fungsi animasi yang sesuai untuk perubahan status air ---->
    private fun toggleWaterWithAnimation(containerID: Int, toggleId: Int) {
        val airContainer = findViewById<LinearLayout>(containerID)
        val toggleSwitch = findViewById<LinearLayout>(toggleId)

        // Cari TextView dan View di dalam toggleSwitch
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

        // Cek current state berdasarkan text toggle
        val isCurrentlyOn = toggleText.text.toString().equals("On", ignoreCase = true)

        // Disable toggle sementara untuk mencegah double click
        toggleSwitch.isEnabled = false

        if (isCurrentlyOn) {
            // Ubah dari ON ke OFF dengan animasi
            animateToggleToOff(airContainer, toggleSwitch, toggleText, toggleThumb)
        } else {
            // Ubah dari OFF ke ON dengan animasi
            animateToggleToOn(airContainer, toggleSwitch, toggleText, toggleThumb)
        }
    }

    // <---- Fungsi animateToggleToOff untuk mengubah status toggle dari ON ke OFF dengan animasi slide, mengatur ulang posisi elemen dan merubah background ---->
    private fun animateToggleToOff(
        airContainer: LinearLayout,
        toggleSwitch: LinearLayout,
        toggleText: TextView,
        toggleThumb: View
    ) {
        // Disable toggle untuk mencegah double click
        toggleSwitch.isEnabled = false

        // Simple slide animation - thumb bergerak turun (ON ke OFF)
        val slideAnimator = ObjectAnimator.ofFloat(toggleThumb, "translationY", 0f, -90f)
        slideAnimator.duration = 600
        slideAnimator.interpolator = DecelerateInterpolator()

        slideAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Reset translation
                toggleThumb.translationY = 0f

                // Update backgrounds
                airContainer.setBackgroundResource(R.drawable.bg_off)
                toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_off)

                // Reorder children - untuk OFF: View dulu, TextView kedua
                toggleSwitch.removeAllViews()

                // Buat ulang View untuk OFF state (di bawah)
                val newThumbView = View(this@MonitoringTandonAirActivity)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    25.dpToPx()
                )
                layoutParams.setMargins(0, 0, 0, 4.dpToPx())
                newThumbView.layoutParams = layoutParams
                newThumbView.setBackgroundResource(R.drawable.toggle_thumb_off)

                // Update TextView
                toggleText.text = "Off"
                toggleText.setTextColor(resources.getColor(android.R.color.white, null))
                val textLayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                toggleText.layoutParams = textLayoutParams

                // Add views in OFF order: View first (di bawah), TextView second (di atas)
                toggleSwitch.addView(newThumbView)
                toggleSwitch.addView(toggleText)

                updateContainerTextColors(airContainer, false)

                // Simpan state ke SharedPreferences
                saveWaterToggleState(false)

                toggleSwitch.isEnabled = true
            }
        })

        slideAnimator.start()
    }

    // <---- Fungsi animateToggleToOn untuk mengubah status toggle dari OFF ke ON dengan animasi slide, merekonstruksi UI dengan posisi elemen dan background yang sesuai ---->
    private fun animateToggleToOn(
        airContainer: LinearLayout,
        toggleSwitch: LinearLayout,
        toggleText: TextView,
        toggleThumb: View
    ) {
        // Disable toggle untuk mencegah double click
        toggleSwitch.isEnabled = false

        // Simple slide animation - thumb bergerak naik (OFF ke ON)
        val slideAnimator = ObjectAnimator.ofFloat(toggleThumb, "translationY", 0f, 90f)
        slideAnimator.duration = 600
        slideAnimator.interpolator = DecelerateInterpolator()

        slideAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                // Reset translation
                toggleThumb.translationY = 0f

                // Update backgrounds
                airContainer.setBackgroundResource(R.drawable.bg_on)
                toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_on)

                // Reorder children - untuk ON: TextView dulu, View kedua
                toggleSwitch.removeAllViews()

                // Update TextView
                toggleText.text = "On"
                toggleText.setTextColor(resources.getColor(android.R.color.white, null))
                val textLayoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
                toggleText.layoutParams = textLayoutParams

                // Buat ulang View untuk ON state (di atas)
                val newThumbView = View(this@MonitoringTandonAirActivity)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    25.dpToPx()
                )
                layoutParams.setMargins(0, 0, 0, 4.dpToPx())
                newThumbView.layoutParams = layoutParams
                newThumbView.setBackgroundResource(R.drawable.toggle_thumb_on)

                // Add views in ON order: TextView first (di atas), View second (di bawah)
                toggleSwitch.addView(toggleText)
                toggleSwitch.addView(newThumbView)

                updateContainerTextColors(airContainer, true)

                // Simpan state ke SharedPreferences
                saveWaterToggleState(true)

                toggleSwitch.isEnabled = true
            }
        })

        slideAnimator.start()
    }

    // <---- Fungsi dpToPx untuk mengkonversi ukuran dari density-independent pixels (dp) ke pixels (px) untuk konsistensi tampilan pada berbagai device ---->
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    // <---- Fungsi updateContainerTextColors untuk memperbarui warna teks di container air berdasarkan status on/off untuk meningkatkan visual feedback ---->
    private fun updateContainerTextColors(airContainer: LinearLayout, isOn: Boolean) {
        // Cari LinearLayout pertama yang berisi TextView "Air" dan status
        for (i in 0 until airContainer.childCount) {
            val child = airContainer.getChildAt(i)
            if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL && child.childCount >= 2) {
                // Ini adalah LinearLayout yang berisi TextView
                for (j in 0 until child.childCount) {
                    val textView = child.getChildAt(j)
                    if (textView is TextView) {
                        if (isOn) {
                            textView.setTextColor(resources.getColor(android.R.color.white, null))
                            // Untuk text kedua (status air), buat sedikit transparan
                            if (j == 1) {
                                textView.alpha = 0.8f
                                // Update text status
                                textView.text = "Keran air hidup"
                            }
                        } else {
                            textView.setTextColor(resources.getColor(android.R.color.black, null))
                            textView.alpha = 1.0f
                            // Update text status
                            if (j == 1) {
                                textView.text = "Keran air mati"
                            }
                        }
                    }
                }
                break // Keluar dari loop setelah menemukan LinearLayout yang tepat
            }
        }
    }

    // <---- Fungsi saveWaterToggleState untuk menyimpan state toggle air ke SharedPreferences ---->
    private fun saveWaterToggleState(isOn: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean(KEY_AIR_STATUS, isOn)
            apply()
        }
    }

    // <---- Fungsi restoreWaterToggleState untuk mengembalikan state toggle air dari SharedPreferences saat aplikasi dimulai ---->
    private fun restoreWaterToggleState() {
        val isOn = sharedPreferences.getBoolean(KEY_AIR_STATUS, false)
        if (isOn) {
            setToggleToOnState()
        } else {
            setToggleToOffState()
        }
    }

    // <---- Fungsi setToggleToOnState untuk mengatur toggle ke posisi ON tanpa animasi saat restore ---->
    private fun setToggleToOnState() {
        val airContainer = findViewById<LinearLayout>(R.id.air_btn)
        val toggleSwitch = findViewById<LinearLayout>(R.id.toggle_air_btn)

        // Update backgrounds
        airContainer.setBackgroundResource(R.drawable.bg_on)
        toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_on)

        // Clear and rebuild toggle
        toggleSwitch.removeAllViews()

        // Create TextView for ON state
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

        // Create thumb view for ON state
        val thumbView = View(this)
        val thumbLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            25.dpToPx()
        )
        thumbLayoutParams.setMargins(0, 0, 0, 4.dpToPx())
        thumbView.layoutParams = thumbLayoutParams
        thumbView.setBackgroundResource(R.drawable.toggle_thumb_on)

        // Add views in ON order: TextView first (di atas), View second (di bawah)
        toggleSwitch.addView(toggleText)
        toggleSwitch.addView(thumbView)

        updateContainerTextColors(airContainer, true)
    }

    // <---- Fungsi setToggleToOffState untuk mengatur toggle ke posisi OFF tanpa animasi saat restore ---->
    private fun setToggleToOffState() {
        val airContainer = findViewById<LinearLayout>(R.id.air_btn)
        val toggleSwitch = findViewById<LinearLayout>(R.id.toggle_air_btn)

        // Update backgrounds
        airContainer.setBackgroundResource(R.drawable.bg_off)
        toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_off)

        // Clear and rebuild toggle
        toggleSwitch.removeAllViews()

        // Create thumb view for OFF state
        val thumbView = View(this)
        val thumbLayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            25.dpToPx()
        )
        thumbLayoutParams.setMargins(0, 0, 0, 4.dpToPx())
        thumbView.layoutParams = thumbLayoutParams
        thumbView.setBackgroundResource(R.drawable.toggle_thumb_off)

        // Create TextView for OFF state
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

        // Add views in OFF order: View first (di bawah), TextView second (di atas)
        toggleSwitch.addView(thumbView)
        toggleSwitch.addView(toggleText)

        updateContainerTextColors(airContainer, false)
    }
}