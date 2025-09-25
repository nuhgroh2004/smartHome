package com.example.smarthome

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ControlLampuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control_lampu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi click listeners untuk toggle switch langsung
        setupToggleListeners()
    }

    private fun setupToggleListeners() {
        // Set click listener pada toggle switch langsung, bukan container

        // Semua ruangan
        findViewById<LinearLayout>(R.id.toggle_semua_ruangan).setOnClickListener {
            toggleLamp(R.id.lampu_semua_ruangan, R.id.toggle_semua_ruangan)
        }

        // Ruang tamu
        findViewById<LinearLayout>(R.id.toggle_ruang_tamu).setOnClickListener {
            toggleLamp(R.id.lampu_ruang_tamu, R.id.toggle_ruang_tamu)
        }

        // Ruang keluarga 1
        findViewById<LinearLayout>(R.id.toggle_ruang_keluarga_1).setOnClickListener {
            toggleLamp(R.id.lampu_ruang_keluarga_1, R.id.toggle_ruang_keluarga_1)
        }

        // Ruang keluarga 2
        findViewById<LinearLayout>(R.id.toggle_ruang_keluarga_2).setOnClickListener {
            toggleLamp(R.id.lampu_ruang_keluarga_2, R.id.toggle_ruang_keluarga_2)
        }

        // Kamar 1
        findViewById<LinearLayout>(R.id.toggle_kamar_1).setOnClickListener {
            toggleLamp(R.id.lampu_kamar_1, R.id.toggle_kamar_1)
        }

        // Kamar 2
        findViewById<LinearLayout>(R.id.toggle_kamar_2).setOnClickListener {
            toggleLamp(R.id.lampu_kamar_2, R.id.toggle_kamar_2)
        }

        // Kamar 3
        findViewById<LinearLayout>(R.id.toggle_kamar_3).setOnClickListener {
            toggleLamp(R.id.lampu_kamar_3, R.id.toggle_kamar_3)
        }

        // Kamar mandi
        findViewById<LinearLayout>(R.id.toggle_kamar_mandi).setOnClickListener {
            toggleLamp(R.id.lampu_kamar_mandi, R.id.toggle_kamar_mandi)
        }

        // Dapur
        findViewById<LinearLayout>(R.id.toggle_dapur).setOnClickListener {
            toggleLamp(R.id.lampu_dapur, R.id.toggle_dapur)
        }

        // Tangga
        findViewById<LinearLayout>(R.id.toggle_tangga).setOnClickListener {
            toggleLamp(R.id.lampu_tangga, R.id.toggle_tangga)
        }

        // Ruang makan
        findViewById<LinearLayout>(R.id.toggle_ruang_makan).setOnClickListener {
            toggleLamp(R.id.lampu_ruang_makan, R.id.toggle_ruang_makan)
        }

        // Smoking area
        findViewById<LinearLayout>(R.id.toggle_smoking_area).setOnClickListener {
            toggleLamp(R.id.lampu_smoking_area, R.id.toggle_smoking_area)
        }

        // Teras
        findViewById<LinearLayout>(R.id.toggle_teras).setOnClickListener {
            toggleLamp(R.id.lampu_teras, R.id.toggle_teras)
        }

        // Jemuran
        findViewById<LinearLayout>(R.id.toggle_jemuran).setOnClickListener {
            toggleLamp(R.id.lampu_jemuran, R.id.toggle_jemuran)
        }
    }

    private fun toggleLamp(lampContainerId: Int, toggleId: Int) {
        val lampContainer = findViewById<LinearLayout>(lampContainerId)
        val toggleSwitch = findViewById<LinearLayout>(toggleId)

        // Cari TextView dan View di dalam toggleSwitch
        var toggleText: TextView? = null
        var toggleThumb: View? = null

        for (i in 0 until toggleSwitch.childCount) {
            val child = toggleSwitch.getChildAt(i)
            if (child is TextView) {
                toggleText = child
            } else if (child is View && child !is TextView) {
                toggleThumb = child
            }
        }

        if (toggleText == null || toggleThumb == null) {
            return
        }

        // Cek current state berdasarkan text toggle
        val isCurrentlyOn = toggleText.text.toString().equals("On", ignoreCase = true)

        if (isCurrentlyOn) {
            // Ubah dari ON ke OFF
            // 1. Background container lampu
            lampContainer.setBackgroundResource(R.drawable.bg_off)

            // 2. Background toggle switch
            toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_off)

            // 3. Text toggle
            toggleText.text = "Off"
            toggleText.setTextColor(resources.getColor(android.R.color.white, null))

            // 4. Toggle thumb (posisi bawah untuk OFF)
            toggleThumb.setBackgroundResource(R.drawable.toggle_thumb_on)

            // 5. Update warna text pada container
            updateContainerTextColors(lampContainer, false)

        } else {
            // Ubah dari OFF ke ON
            // 1. Background container lampu
            lampContainer.setBackgroundResource(R.drawable.bg_on)

            // 2. Background toggle switch
            toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_on)

            // 3. Text toggle
            toggleText.text = "On"
            toggleText.setTextColor(resources.getColor(android.R.color.white, null))

            // 4. Toggle thumb (posisi atas untuk ON)
            toggleThumb.setBackgroundResource(R.drawable.toggle_thumb_off)

            // 5. Update warna text pada container
            updateContainerTextColors(lampContainer, true)
        }
    }

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