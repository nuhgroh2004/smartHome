package com.example.smarthome

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MonitoringTandonAirActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_monitoring_tandon_air)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup toggle listener untuk kontrol air
        setupToggleListener()

        // Setup back button
        findViewById<View>(R.id.btn_back).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
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
}