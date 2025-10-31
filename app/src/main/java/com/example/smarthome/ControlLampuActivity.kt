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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ControlLampuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityControlLampuBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebase: FirebaseDatabase
    companion object {
        private const val PREFS_NAME = "lamp_states"
        private const val KEY_SEMUA_RUANGAN = "semua_ruangan"
        private const val KEY_LAMPU_1 = "lampu_1"
        private const val KEY_LAMPU_2 = "lampu_2"
        private const val KEY_LAMPU_3 = "lampu_3"
        private const val KEY_LAMPU_4 = "lampu_4"
        private const val KEY_LAMPU_5 = "lampu_5"
    }

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
        firebase = FirebaseDatabase.getInstance()
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        setupFirebaseListener()
        setupToggleListeners()
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupFirebaseListener() {
        firebase.reference.child("IoTSystem").child("Lampu")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        android.util.Log.d("FirebaseData", "Lampu Snapshot exists: ${snapshot.exists()}")
                        android.util.Log.d("FirebaseData", "Lampu Full data: ${snapshot.value}")
                        updateLampFromFirebase(snapshot, "Lampu1", R.id.lampu_1, R.id.toggle_lampu_1)
                        updateLampFromFirebase(snapshot, "Lampu2", R.id.lampu_2, R.id.toggle_lampu_2)
                        updateLampFromFirebase(snapshot, "Lampu3", R.id.lampu_3, R.id.toggle_lampu_3)
                        updateLampFromFirebase(snapshot, "Lampu4", R.id.lampu_4, R.id.toggle_lampu_4)
                        updateLampFromFirebase(snapshot, "Lampu5", R.id.lampu_5, R.id.toggle_lampu_5)
                        updateSemuaRuanganStatus(snapshot)
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseError", "Error processing lamp data", e)
                        e.printStackTrace()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("FirebaseError", "Database error: ${error.message}")
                }
            })
    }

    private fun updateLampFromFirebase(snapshot: DataSnapshot, lampKey: String, lampId: Int, toggleId: Int) {
        val status = snapshot.child(lampKey).child("status").getValue(String::class.java) ?: "OFF"
        val isOn = status.equals("ON", ignoreCase = true)
        android.util.Log.d("FirebaseData", "$lampKey status: $status")
        if (isOn) {
            setToggleToOnState(lampId, toggleId)
        } else {
            setToggleToOffState(lampId, toggleId)
        }
        saveToggleState(lampId, isOn)
    }

    private fun updateSemuaRuanganStatus(snapshot: DataSnapshot) {
        var allOn = true
        var allOff = true
        for (i in 1..5) {
            val lampKey = "Lampu$i"
            val status = snapshot.child(lampKey).child("status").getValue(String::class.java) ?: "OFF"
            if (status.equals("ON", ignoreCase = true)) {
                allOff = false
            } else {
                allOn = false
            }
        }
        if (allOn) {
            setToggleToOnState(R.id.lampu_semua_ruangan, R.id.toggle_semua_ruangan)
            saveToggleState(R.id.lampu_semua_ruangan, true)
        } else if (allOff) {
            setToggleToOffState(R.id.lampu_semua_ruangan, R.id.toggle_semua_ruangan)
            saveToggleState(R.id.lampu_semua_ruangan, false)
        }
    }

    private fun updateFirebaseLampStatus(lampNumber: Int, isOn: Boolean) {
        val status = if (isOn) "ON" else "OFF"
        val lampKey = "Lampu$lampNumber"
        firebase.reference.child("IoTSystem").child("Lampu").child(lampKey).child("status").setValue(status)
            .addOnSuccessListener {
                android.util.Log.d("Firebase", "$lampKey status updated to $status")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Firebase", "Failed to update $lampKey status", e)
            }
    }

    private fun updateAllFirebaseLampStatus(isOn: Boolean) {
        val status = if (isOn) "ON" else "OFF"
        for (i in 1..5) {
            val lampKey = "Lampu$i"
            firebase.reference.child("IoTSystem").child("Lampu").child(lampKey).child("status").setValue(status)
        }
    }

    private fun setupToggleListeners() {
        findViewById<LinearLayout>(R.id.toggle_semua_ruangan).setOnClickListener {
            toggleAllLampsWithAnimation()
        }
        findViewById<LinearLayout>(R.id.toggle_lampu_1).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_1, R.id.toggle_lampu_1, 1)
        }
        findViewById<LinearLayout>(R.id.toggle_lampu_2).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_2, R.id.toggle_lampu_2, 2)
        }
        findViewById<LinearLayout>(R.id.toggle_lampu_3).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_3, R.id.toggle_lampu_3, 3)
        }
        findViewById<LinearLayout>(R.id.toggle_lampu_4).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_4, R.id.toggle_lampu_4, 4)
        }
        findViewById<LinearLayout>(R.id.toggle_lampu_5).setOnClickListener {
            toggleLampWithAnimation(R.id.lampu_5, R.id.toggle_lampu_5, 5)
        }
    }

    private fun toggleLampWithAnimation(lampContainerId: Int, toggleId: Int, lampNumber: Int) {
        val lampContainer = findViewById<LinearLayout>(lampContainerId)
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
            animateToggleToOff(lampContainer, toggleSwitch, toggleText, toggleThumb, lampNumber)
        } else {
            animateToggleToOn(lampContainer, toggleSwitch, toggleText, toggleThumb, lampNumber)
        }
    }

    private fun animateToggleToOff(
        lampContainer: LinearLayout,
        toggleSwitch: LinearLayout,
        toggleText: TextView,
        toggleThumb: View,
        lampNumber: Int
    ) {
        toggleSwitch.isEnabled = false
        val slideAnimator = ObjectAnimator.ofFloat(toggleThumb, "translationY", 0f, -90f)
        slideAnimator.duration = 600
        slideAnimator.interpolator = DecelerateInterpolator()
        slideAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                toggleThumb.translationY = 0f
                lampContainer.setBackgroundResource(R.drawable.bg_off)
                toggleSwitch.setBackgroundResource(R.drawable.toggle_bg_off)
                toggleSwitch.removeAllViews()
                val newThumbView = View(this@ControlLampuActivity)
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
                updateContainerTextColors(lampContainer, false)
                saveToggleState(lampContainer.id, false)
                updateFirebaseLampStatus(lampNumber, false)
                toggleSwitch.isEnabled = true
            }
        })
        slideAnimator.start()
    }

    private fun animateToggleToOn(
        lampContainer: LinearLayout,
        toggleSwitch: LinearLayout,
        toggleText: TextView,
        toggleThumb: View,
        lampNumber: Int
    ) {
        toggleSwitch.isEnabled = false
        val slideAnimator = ObjectAnimator.ofFloat(toggleThumb, "translationY", 0f, 90f)
        slideAnimator.duration = 600
        slideAnimator.interpolator = DecelerateInterpolator()
        slideAnimator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                toggleThumb.translationY = 0f
                lampContainer.setBackgroundResource(R.drawable.bg_on)
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
                val newThumbView = View(this@ControlLampuActivity)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    25.dpToPx()
                )
                layoutParams.setMargins(0, 0, 0, 4.dpToPx())
                newThumbView.layoutParams = layoutParams
                newThumbView.setBackgroundResource(R.drawable.toggle_thumb_on)
                toggleSwitch.addView(toggleText)
                toggleSwitch.addView(newThumbView)
                updateContainerTextColors(lampContainer, true)
                saveToggleState(lampContainer.id, true)
                updateFirebaseLampStatus(lampNumber, true)
                toggleSwitch.isEnabled = true
            }
        })
        slideAnimator.start()
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun updateContainerTextColors(lampContainer: LinearLayout, isOn: Boolean) {
        for (i in 0 until lampContainer.childCount) {
            val child = lampContainer.getChildAt(i)
            if (child is LinearLayout && child.orientation == LinearLayout.VERTICAL && child.childCount >= 2) {
                for (j in 0 until child.childCount) {
                    val textView = child.getChildAt(j)
                    if (textView is TextView) {
                        if (isOn) {
                            textView.setTextColor(resources.getColor(android.R.color.white, null))
                            if (j == 1) {
                                textView.alpha = 0.8f
                            }
                        } else {
                            textView.setTextColor(resources.getColor(android.R.color.black, null))
                            textView.alpha = 1.0f
                        }
                    }
                }
                break
            }
        }
    }

    private fun restoreToggleStates() {
        val lampIds = mapOf(
            R.id.lampu_semua_ruangan to R.id.toggle_semua_ruangan,
            R.id.lampu_1 to R.id.toggle_lampu_1,
            R.id.lampu_2 to R.id.toggle_lampu_2,
            R.id.lampu_3 to R.id.toggle_lampu_3,
            R.id.lampu_4 to R.id.toggle_lampu_4,
            R.id.lampu_5 to R.id.toggle_lampu_5
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

    private fun setToggleToOnState(lampContainerId: Int, toggleId: Int) {
        val lampContainer = findViewById<LinearLayout>(lampContainerId)
        val toggleSwitch = findViewById<LinearLayout>(toggleId)
        lampContainer.setBackgroundResource(R.drawable.bg_on)
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
        updateContainerTextColors(lampContainer, true)
    }

    private fun setToggleToOffState(lampContainerId: Int, toggleId: Int) {
        val lampContainer = findViewById<LinearLayout>(lampContainerId)
        val toggleSwitch = findViewById<LinearLayout>(toggleId)
        lampContainer.setBackgroundResource(R.drawable.bg_off)
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
        updateContainerTextColors(lampContainer, false)
    }

    private fun getKeyForLamp(lampId: Int): String {
        return when (lampId) {
            R.id.lampu_semua_ruangan -> KEY_SEMUA_RUANGAN
            R.id.lampu_1 -> KEY_LAMPU_1
            R.id.lampu_2 -> KEY_LAMPU_2
            R.id.lampu_3 -> KEY_LAMPU_3
            R.id.lampu_4 -> KEY_LAMPU_4
            R.id.lampu_5 -> KEY_LAMPU_5
            else -> ""
        }
    }

    private fun saveToggleState(lampContainerId: Int, isOn: Boolean) {
        val key = getKeyForLamp(lampContainerId)
        if (key.isNotEmpty()) {
            sharedPreferences.edit().putBoolean(key, isOn).apply()
        }
    }

    private fun toggleAllLampsWithAnimation() {
        val toggleSemuaRuangan = findViewById<LinearLayout>(R.id.toggle_semua_ruangan)
        val lampSemuaRuangan = findViewById<LinearLayout>(R.id.lampu_semua_ruangan)
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
        val isCurrentlyOn = toggleText.text.toString().equals("On", ignoreCase = true)
        disableAllToggles(true)
        if (isCurrentlyOn) {
            animateToggleToOff(lampSemuaRuangan, toggleSemuaRuangan, toggleText, toggleThumb, 0)
            Handler(Looper.getMainLooper()).postDelayed({
                setAllOtherLampsToOff()
                updateAllFirebaseLampStatus(false)
            }, 100)
        } else {
            animateToggleToOn(lampSemuaRuangan, toggleSemuaRuangan, toggleText, toggleThumb, 0)
            Handler(Looper.getMainLooper()).postDelayed({
                setAllOtherLampsToOn()
                updateAllFirebaseLampStatus(true)
            }, 100)
        }
        saveToggleState(R.id.lampu_semua_ruangan, !isCurrentlyOn)
        Handler(Looper.getMainLooper()).postDelayed({
            disableAllToggles(false)
        }, 700)
    }

    private fun setAllOtherLampsToOff() {
        val lampIds = arrayOf(
            R.id.lampu_1 to R.id.toggle_lampu_1,
            R.id.lampu_2 to R.id.toggle_lampu_2,
            R.id.lampu_3 to R.id.toggle_lampu_3,
            R.id.lampu_4 to R.id.toggle_lampu_4,
            R.id.lampu_5 to R.id.toggle_lampu_5
        )
        lampIds.forEach { (lampId, toggleId) ->
            setToggleToOffState(lampId, toggleId)
            saveToggleState(lampId, false)
        }
    }

    private fun setAllOtherLampsToOn() {
        val lampIds = arrayOf(
            R.id.lampu_1 to R.id.toggle_lampu_1,
            R.id.lampu_2 to R.id.toggle_lampu_2,
            R.id.lampu_3 to R.id.toggle_lampu_3,
            R.id.lampu_4 to R.id.toggle_lampu_4,
            R.id.lampu_5 to R.id.toggle_lampu_5
        )
        lampIds.forEach { (lampId, toggleId) ->
            setToggleToOnState(lampId, toggleId)
            saveToggleState(lampId, true)
        }
    }

    private fun disableAllToggles(disable: Boolean) {
        val toggleIds = arrayOf(
            R.id.toggle_semua_ruangan,
            R.id.toggle_lampu_1, R.id.toggle_lampu_2, R.id.toggle_lampu_3, R.id.toggle_lampu_4,
            R.id.toggle_lampu_5
        )
        toggleIds.forEach { toggleId ->
            findViewById<LinearLayout>(toggleId).isEnabled = !disable
        }
    }
}
