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
        // Konstanta untuk perhitungan level air
        private const val MAX_WATER_HEIGHT_CM = 50.0 // Tinggi maksimum tandon dalam cm (diubah dari 100 ke 50)
        private const val MIN_WATER_HEIGHT_CM = 0.0   // Tinggi minimum tandon dalam cm
        private const val MAX_IMAGE_HEIGHT_DP = 200   // Tinggi maksimum gambar dalam dp
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

        // Inisialisasi Firebase
        firebase = FirebaseDatabase.getInstance()

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Setup Firebase listener untuk monitoring data tandon air
        setupFirebaseListener()

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

    // <---- Fungsi setupFirebaseListener untuk mendengarkan perubahan data dari Firebase Realtime Database ---->
    private fun setupFirebaseListener() {
        firebase.reference.child("IoTSystem").child("TandonAir")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        // Debug logging untuk melihat data yang diterima
                        android.util.Log.d("FirebaseData", "Snapshot exists: ${snapshot.exists()}")
                        android.util.Log.d("FirebaseData", "Full data: ${snapshot.value}")
                        android.util.Log.d("FirebaseData", "tinggiAir_cm raw: ${snapshot.child("tinggiAir_cm").value}")
                        android.util.Log.d("FirebaseData", "pompa raw: ${snapshot.child("pompa").value}")

                        // Ambil data tinggi air dengan handling berbagai tipe data
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

                        // Update UI berdasarkan data dari Firebase
                        updateWaterLevelUI(tinggiAirCm)
                        updatePumpStatusFromFirebase(pompStatus)

                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseError", "Error processing data", e)
                        e.printStackTrace()
                        // Jika ada error, set default values
                        updateWaterLevelUI(0.0)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("FirebaseError", "Database error: ${error.message}")
                    // Handle error - set default values
                    updateWaterLevelUI(0.0)
                }
            })
    }

    // <---- Fungsi updateWaterLevelUI untuk memperbarui tampilan level air berdasarkan data sensor ---->
    private fun updateWaterLevelUI(tinggiAirCm: Double) {
        // Hitung persentase level air (0-100%)
        val percentage = calculateWaterPercentage(tinggiAirCm)

        // Update text persentase
        val waterLevelText = findViewById<TextView>(R.id.water_level_text)
        waterLevelText.text = "${percentage.toInt()}%"

        // Update tinggi gambar level air
        updateAirLevelImageHeight(percentage)
    }

    // <---- Fungsi calculateWaterPercentage untuk menghitung persentase level air dari tinggi dalam cm ---->
    private fun calculateWaterPercentage(tinggiAirCm: Double): Double {
        // Pastikan nilai dalam range yang valid
        val clampedHeight = tinggiAirCm.coerceIn(MIN_WATER_HEIGHT_CM, MAX_WATER_HEIGHT_CM)

        // Hitung persentase (0-100%)
        return (clampedHeight / MAX_WATER_HEIGHT_CM) * 100.0
    }

    // <---- Fungsi updateAirLevelImageHeight untuk mengatur tinggi gambar level air sesuai persentase ---->
    private fun updateAirLevelImageHeight(percentage: Double) {
        val airLevelImg = findViewById<ImageView>(R.id.air_level_img)

        // Hitung tinggi berdasarkan persentase dengan maksimum 180dp
        val maxHeightPx = (MAX_IMAGE_HEIGHT_DP * resources.displayMetrics.density).toInt()

        // Hitung tinggi baru berdasarkan persentase (minimum 5% untuk visibility)
        val minPercentage = 5.0
        val adjustedPercentage = percentage.coerceAtLeast(minPercentage)
        val newHeight = (maxHeightPx * (adjustedPercentage / 100.0)).toInt()

        // Simpan tinggi lama untuk animasi
        val oldHeight = airLevelImg.height

        // Animasi perubahan tinggi menggunakan ValueAnimator
        val animator = android.animation.ValueAnimator.ofInt(oldHeight, newHeight)
        animator.duration = 500
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { valueAnimator ->
            val animatedValue = valueAnimator.animatedValue as Int
            val layoutParams = airLevelImg.layoutParams
            layoutParams.height = animatedValue
            airLevelImg.layoutParams = layoutParams
            airLevelImg.requestLayout() // PENTING: Force layout update
        }
        animator.start()
    }

    // <---- Fungsi updatePumpStatusFromFirebase untuk sinkronisasi status pompa dari Firebase ---->
    private fun updatePumpStatusFromFirebase(pompStatus: String) {
        val isOn = pompStatus.equals("ON", ignoreCase = true)

        // Update UI toggle sesuai status dari Firebase
        if (isOn) {
            setToggleToOnState()
        } else {
            setToggleToOffState()
        }

        // Simpan state ke SharedPreferences
        saveWaterToggleState(isOn)
    }

    // <---- Fungsi updateFirebasePumpStatus untuk mengirim perubahan status pompa ke Firebase ---->
    private fun updateFirebasePumpStatus(isOn: Boolean) {
        val status = if (isOn) "ON" else "OFF"
        firebase.reference.child("IoTSystem").child("TandonAir").child("pompa").setValue(status)
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

                // Update status pompa ke Firebase
                updateFirebasePumpStatus(false)

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

                // Update status pompa ke Firebase
                updateFirebasePumpStatus(true)

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

