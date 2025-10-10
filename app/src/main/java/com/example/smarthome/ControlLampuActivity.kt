package com.example.smarthome

import android.animation.ObjectAnimator
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        private const val PREFS_NAME = "lamp_states"
        private const val KEY_SEMUA_RUANGAN = "semua_ruangan"
        private const val KEY_LAMPU_1 = "lampu_1"
        private const val KEY_LAMPU_2 = "lampu_2"
        private const val KEY_LAMPU_3 = "lampu_3"
        private const val KEY_LAMPU_4 = "lampu_4"
        private const val KEY_LAMPU_5 = "lampu_5"
        private const val KEY_LAMPU_6 = "lampu_6"
        private const val KEY_LAMPU_7 = "lampu_7"
        private const val KEY_LAMPU_8 = "lampu_8"
        private const val KEY_LAMPU_9 = "lampu_9"
        private const val KEY_LAMPU_10 = "lampu_10"
        private const val KEY_LAMPU_11 = "lampu_11"
        private const val KEY_LAMPU_12 = "lampu_12"
        private const val KEY_LAMPU_13 = "lampu_13"
    }

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

        // Inisialisasi SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // Inisialisasi click listeners untuk toggle switch langsung
        setupToggleListeners()

        // Restore saved toggle states
        restoreToggleStates()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    // <---- Fungsi setupToggleListeners untuk menginisialisasi semua click listener pada tombol-tombol toggle lampu di setiap ruangan dalam rumah ---->
    private fun setupToggleListeners() {
        // Set click listener pada toggle switch langsung, bukan container
        // Semua ruangan - Master toggle that controls all other lamps
        findViewById<LinearLayout>(R.id.toggle_semua_ruangan).setOnClickListener {
            toggleAllLampsWithAnimation()
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

                // Save state to SharedPreferences
                saveToggleState(lampContainer.id, false)

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

                // Save state to SharedPreferences
                saveToggleState(lampContainer.id, true)

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

    // <---- Fungsi restoreToggleStates untuk mengembalikan status toggle lampu sesuai dengan yang tersimpan di SharedPreferences saat aplikasi dibuka ---->
    private fun restoreToggleStates() {
        val lampIds = mapOf(
            R.id.lampu_semua_ruangan to R.id.toggle_semua_ruangan,
            R.id.lampu_1 to R.id.toggle_lampu_1,
            R.id.lampu_2 to R.id.toggle_lampu_2,
            R.id.lampu_3 to R.id.toggle_lampu_3,
            R.id.lampu_4 to R.id.toggle_lampu_4,
            R.id.lampu_5 to R.id.toggle_lampu_5,
            R.id.lampu_6 to R.id.toggle_lampu_6,
            R.id.lampu_7 to R.id.toggle_lampu_7,
            R.id.lampu_8 to R.id.toggle_lampu_8,
            R.id.lampu_9 to R.id.toggle_lampu_9,
            R.id.lampu_10 to R.id.toggle_lampu_10,
            R.id.lampu_11 to R.id.toggle_lampu_11,
            R.id.lampu_12 to R.id.toggle_lampu_12,
            R.id.lampu_13 to R.id.toggle_lampu_13
        )

        lampIds.forEach { (lampId, toggleId) ->
            val isOn = sharedPreferences.getBoolean(getKeyForLamp(lampId), false)
            if (isOn) {
                setToggleToOnState(lampId, toggleId)
            } else {
                setToggleToOffState(lampId, toggleId)
            }
        }
    }

    // <---- Fungsi setToggleToOnState untuk mengatur toggle ke posisi ON tanpa animasi saat restore ---->
    private fun setToggleToOnState(lampContainerId: Int, toggleId: Int) {
        val lampContainer = findViewById<LinearLayout>(lampContainerId)
        val toggleSwitch = findViewById<LinearLayout>(toggleId)

        // Update backgrounds
        lampContainer.setBackgroundResource(R.drawable.bg_on)
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

        // Add views in ON order: TextView first, View second
        toggleSwitch.addView(toggleText)
        toggleSwitch.addView(thumbView)

        updateContainerTextColors(lampContainer, true)
    }

    // <---- Fungsi setToggleToOffState untuk mengatur toggle ke posisi OFF tanpa animasi saat restore ---->
    private fun setToggleToOffState(lampContainerId: Int, toggleId: Int) {
        val lampContainer = findViewById<LinearLayout>(lampContainerId)
        val toggleSwitch = findViewById<LinearLayout>(toggleId)

        // Update backgrounds
        lampContainer.setBackgroundResource(R.drawable.bg_off)
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

        // Add views in OFF order: View first, TextView second
        toggleSwitch.addView(thumbView)
        toggleSwitch.addView(toggleText)

        updateContainerTextColors(lampContainer, false)
    }

    // <---- Fungsi getKeyForLamp untuk mendapatkan key SharedPreferences berdasarkan ID lampu ---->
    private fun getKeyForLamp(lampId: Int): String {
        return when (lampId) {
            R.id.lampu_semua_ruangan -> KEY_SEMUA_RUANGAN
            R.id.lampu_1 -> KEY_LAMPU_1
            R.id.lampu_2 -> KEY_LAMPU_2
            R.id.lampu_3 -> KEY_LAMPU_3
            R.id.lampu_4 -> KEY_LAMPU_4
            R.id.lampu_5 -> KEY_LAMPU_5
            R.id.lampu_6 -> KEY_LAMPU_6
            R.id.lampu_7 -> KEY_LAMPU_7
            R.id.lampu_8 -> KEY_LAMPU_8
            R.id.lampu_9 -> KEY_LAMPU_9
            R.id.lampu_10 -> KEY_LAMPU_10
            R.id.lampu_11 -> KEY_LAMPU_11
            R.id.lampu_12 -> KEY_LAMPU_12
            R.id.lampu_13 -> KEY_LAMPU_13
            else -> ""
        }
    }

    // <---- Fungsi saveToggleState untuk menyimpan status toggle ke SharedPreferences ---->
    private fun saveToggleState(lampContainerId: Int, isOn: Boolean) {
        val key = getKeyForLamp(lampContainerId)
        if (key.isNotEmpty()) {
            sharedPreferences.edit().putBoolean(key, isOn).apply()
        }
    }

    // <---- Fungsi toggleAllLampsWithAnimation untuk mengubah status semua lampu sekaligus dengan animasi, berdasarkan status toggle "Semua Ruangan" ---->
    private fun toggleAllLampsWithAnimation() {
        val toggleSemuaRuangan = findViewById<LinearLayout>(R.id.toggle_semua_ruangan)
        val lampSemuaRuangan = findViewById<LinearLayout>(R.id.lampu_semua_ruangan)

        // Cari TextView dan View di dalam toggleSwitch
        var toggleText: TextView? = null
        var toggleThumb: View? = null
        for (i in 0 until toggleSemuaRuangan.childCount) {
            val child = toggleSemuaRuangan.getChildAt(i)
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

        // Disable semua toggle sementara untuk mencegah interaksi selama animasi
        disableAllToggles(true)

        if (isCurrentlyOn) {
            // Ubah semua lampu ke OFF dengan animasi
            animateToggleToOff(lampSemuaRuangan, toggleSemuaRuangan, toggleText, toggleThumb)
            // Set semua lampu lainnya ke OFF tanpa animasi
            Handler(Looper.getMainLooper()).postDelayed({
                setAllOtherLampsToOff()
            }, 100) // Delay kecil untuk memastikan animasi master toggle sudah dimulai
        } else {
            // Ubah semua lampu ke ON dengan animasi
            animateToggleToOn(lampSemuaRuangan, toggleSemuaRuangan, toggleText, toggleThumb)
            // Set semua lampu lainnya ke ON tanpa animasi
            Handler(Looper.getMainLooper()).postDelayed({
                setAllOtherLampsToOn()
            }, 100) // Delay kecil untuk memastikan animasi master toggle sudah dimulai
        }

        // Save state untuk master toggle
        saveToggleState(R.id.lampu_semua_ruangan, !isCurrentlyOn)

        // Enable kembali semua toggle setelah delay singkat
        Handler(Looper.getMainLooper()).postDelayed({
            disableAllToggles(false)
        }, 700) // Tunggu sampai animasi selesai
    }

    // <---- Fungsi setAllOtherLampsToOff untuk mengatur semua lampu selain "Semua Ruangan" ke posisi OFF tanpa animasi ---->
    private fun setAllOtherLampsToOff() {
        val lampIds = arrayOf(
            R.id.lampu_1 to R.id.toggle_lampu_1,
            R.id.lampu_2 to R.id.toggle_lampu_2,
            R.id.lampu_3 to R.id.toggle_lampu_3,
            R.id.lampu_4 to R.id.toggle_lampu_4,
            R.id.lampu_5 to R.id.toggle_lampu_5,
            R.id.lampu_6 to R.id.toggle_lampu_6,
            R.id.lampu_7 to R.id.toggle_lampu_7,
            R.id.lampu_8 to R.id.toggle_lampu_8,
            R.id.lampu_9 to R.id.toggle_lampu_9,
            R.id.lampu_10 to R.id.toggle_lampu_10,
            R.id.lampu_11 to R.id.toggle_lampu_11,
            R.id.lampu_12 to R.id.toggle_lampu_12,
            R.id.lampu_13 to R.id.toggle_lampu_13
        )

        lampIds.forEach { (lampId, toggleId) ->
            setToggleToOffState(lampId, toggleId)
            // Save state to SharedPreferences
            saveToggleState(lampId, false)
        }
    }

    // <---- Fungsi setAllOtherLampsToOn untuk mengatur semua lampu selain "Semua Ruangan" ke posisi ON tanpa animasi ---->
    private fun setAllOtherLampsToOn() {
        val lampIds = arrayOf(
            R.id.lampu_1 to R.id.toggle_lampu_1,
            R.id.lampu_2 to R.id.toggle_lampu_2,
            R.id.lampu_3 to R.id.toggle_lampu_3,
            R.id.lampu_4 to R.id.toggle_lampu_4,
            R.id.lampu_5 to R.id.toggle_lampu_5,
            R.id.lampu_6 to R.id.toggle_lampu_6,
            R.id.lampu_7 to R.id.toggle_lampu_7,
            R.id.lampu_8 to R.id.toggle_lampu_8,
            R.id.lampu_9 to R.id.toggle_lampu_9,
            R.id.lampu_10 to R.id.toggle_lampu_10,
            R.id.lampu_11 to R.id.toggle_lampu_11,
            R.id.lampu_12 to R.id.toggle_lampu_12,
            R.id.lampu_13 to R.id.toggle_lampu_13
        )

        lampIds.forEach { (lampId, toggleId) ->
            setToggleToOnState(lampId, toggleId)
            // Save state to SharedPreferences
            saveToggleState(lampId, true)
        }
    }

    // <---- Fungsi disableAllToggles untuk menonaktifkan atau mengaktifkan semua toggle dalam keadaan tertentu, seperti saat animasi berlangsung ---->
    private fun disableAllToggles(disable: Boolean) {
        val toggleIds = arrayOf(
            R.id.toggle_semua_ruangan,
            R.id.toggle_lampu_1, R.id.toggle_lampu_2, R.id.toggle_lampu_3, R.id.toggle_lampu_4,
            R.id.toggle_lampu_5, R.id.toggle_lampu_6, R.id.toggle_lampu_7, R.id.toggle_lampu_8,
            R.id.toggle_lampu_9, R.id.toggle_lampu_10, R.id.toggle_lampu_11, R.id.toggle_lampu_12, R.id.toggle_lampu_13
        )
        toggleIds.forEach { toggleId ->
            findViewById<LinearLayout>(toggleId).isEnabled = !disable
        }
    }
}
