package com.example.smarthome

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityControlLampuBinding

class ControlLampuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlLampuBinding
    // <---- Fungsi onCreate untuk inisialisasi activity dan melakukan setup awal UI serta listener untuk pengolahan insets sistem ---->
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityControlLampuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Inisialisasi click listeners untuk toggle switch langsung
        setupToggleListeners()


        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // <---- Fungsi setupToggleListeners untuk menginisialisasi semua click listener pada tombol-tombol toggle lampu di setiap ruangan dalam rumah ---->
    private fun setupToggleListeners() {
        // Set click listener pada toggle switch langsung, bukan container
        // Semua ruangan
        findViewById<LinearLayout>(R.id.toggle_semua_ruangan).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_semua_ruangan, R.id.toggle_semua_ruangan)
        }
        // Ruang tamu
        findViewById<LinearLayout>(R.id.toggle_lampu_1).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_1, R.id.toggle_lampu_1)
        }
        // Ruang tamu
        findViewById<LinearLayout>(R.id.toggle_lampu_2).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_2, R.id.toggle_lampu_2)
        }
        // Ruang keluarga 1
        findViewById<LinearLayout>(R.id.toggle_lampu_3).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_3, R.id.toggle_lampu_3)
        }
        // Ruang keluarga 2
        findViewById<LinearLayout>(R.id.toggle_lampu_4).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_4, R.id.toggle_lampu_4)
        }
        // Kamar 1
        findViewById<LinearLayout>(R.id.toggle_lampu_5).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_5, R.id.toggle_lampu_5)
        }
        // Kamar 2
        findViewById<LinearLayout>(R.id.toggle_lampu_6).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_6, R.id.toggle_lampu_6)
        }
        // Kamar 3
        findViewById<LinearLayout>(R.id.toggle_lampu_7).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_7, R.id.toggle_lampu_7)
        }
        // Kamar mandi
        findViewById<LinearLayout>(R.id.toggle_lampu_8).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_8, R.id.toggle_lampu_8)
        }
        // Dapur
        findViewById<LinearLayout>(R.id.toggle_lampu_9).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_9, R.id.toggle_lampu_9)
        }
        // Tangga
        findViewById<LinearLayout>(R.id.toggle_lampu_10).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_10, R.id.toggle_lampu_10)
        }
        // Ruang makan
        findViewById<LinearLayout>(R.id.toggle_lampu_11).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_11, R.id.toggle_lampu_11)
        }
        // Smoking area
        findViewById<LinearLayout>(R.id.toggle_lampu_12).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_12, R.id.toggle_lampu_12)
        }
        // Teras
        findViewById<LinearLayout>(R.id.toggle_lampu_13).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_13, R.id.toggle_lampu_13)
        }
    }

    // <---- Fungsi toggleLampWithAnimation untuk mendeteksi status toggle saat ini dan mengarahkan ke fungsi animasi yang sesuai untuk perubahan status lampu ---->
    private fun toggleLampWithAnimation(lampContainerId: Int, toggleId: Int) {
        val lampContainer = findViewById<LinearLayout>(lampContainerId)
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
            animateToggleToOff(lampContainer, toggleSwitch, toggleText, toggleThumb)
        } else {
            // Ubah dari OFF ke ON dengan animasi
            animateToggleToOn(lampContainer, toggleSwitch, toggleText, toggleThumb)
        }
    }

    // <---- Fungsi animateToggleToOff untuk mengubah status toggle dari ON ke OFF dengan animasi slide, mengatur ulang posisi elemen dan merubah background ---->
    private fun animateToggleToOff(
        lampContainer: LinearLayout,
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
                lampContainer.setBackgroundResource(R.drawable.bg_off)
                toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_off)
                // Reorder children - untuk OFF: View dulu, TextView kedua
                toggleSwitch.removeAllViews()
                // Buat ulang View untuk OFF state (di bawah)
                val newThumbView = View(this@ControlLampuActivity)
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
                updateContainerTextColors(lampContainer, false)
                toggleSwitch.isEnabled = true
            }
        })
        slideAnimator.start()
    }

    // <---- Fungsi animateToggleToOn untuk mengubah status toggle dari OFF ke ON dengan animasi slide, merekonstruksi UI dengan posisi elemen dan background yang sesuai ---->
    private fun animateToggleToOn(
        lampContainer: LinearLayout,
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
                lampContainer.setBackgroundResource(R.drawable.bg_on)
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
                val newThumbView = View(this@ControlLampuActivity)
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
                updateContainerTextColors(lampContainer, true)
                toggleSwitch.isEnabled = true
            }
        })
        slideAnimator.start()
    }

    // <---- Fungsi dpToPx untuk mengkonversi ukuran dari density-independent pixels (dp) ke pixels (px) untuk konsistensi tampilan pada berbagai device ---->
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    // <---- Fungsi updateContainerTextColors untuk memperbarui warna teks di container lampu berdasarkan status on/off untuk meningkatkan visual feedback ---->
    private fun updateContainerTextColors(lampContainer: LinearLayout, isOn: Boolean) {
        // Cari LinearLayout pertama yang berisi TextView "Lampu" dan nama ruangan
        for (i in 0 until lampContainer.childCount) {
            val child = lampContainer.getChildAt(i)
            if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL && child.childCount >= 2) {
                // Ini adalah LinearLayout yang berisi TextView
                for (j in 0 until child.childCount) {
                    val textView = child.getChildAt(j)
                    if (textView is TextView) {
                        if (isOn) {
                            textView.setTextColor(resources.getColor(android.R.color.white, null))
                            // Untuk text kedua (nama ruangan), buat sedikit transparan
                            if (j == 1) {
                                textView.alpha = 0.8f
                            }
                        } else {
                            textView.setTextColor(resources.getColor(android.R.color.black, null))
                            textView.alpha = 1.0f
                        }
                    }
                }
                break // Keluar dari loop setelah menemukan LinearLayout yang tepat
            }
        }
    }
}