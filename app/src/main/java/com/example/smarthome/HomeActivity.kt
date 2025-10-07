package com.example.smarthome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityHomeBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.w3c.dom.Text
import android.view.View
import android.view.animation.AnimationUtils
import android.animation.ObjectAnimator
import android.animation.AnimatorSet

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var doubleBackToExitPressedOnce = false

    private lateinit var firebase:FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseDatabase.getInstance()
        firebase.reference.child("data").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val textView = findViewById<TextView>(R.id.greetingText)
                textView.text = snapshot.value.toString()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        /* ------------------------ fungsional untuk tombol berpidah activity -------------------------- */
        binding.cardLampu.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, ControlLampuActivity::class.java)
                startActivity(intent)
            }
        }
        binding.cardTandonAir.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, MonitoringTandonAirActivity::class.java)
                startActivity(intent)
            }
        }
        binding.cardMonitoringRuangan.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, MonitoringRuanganActivity::class.java)
                startActivity(intent)
            }
        }
        binding.notificationBell.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, NotificationActivity::class.java)
                startActivity(intent)
            }
        }
        binding.cardMonitoringListrik.setOnClickListener {
            animateClick(it) {
                val intent = Intent(this, MonitoringListrikActivity::class.java)
                startActivity(intent)
            }
        }
        /* ------------------------ fungsional untuk tombol berpidah activity -------------------------- */
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            finishAffinity() // Close all activities and exit app
            return
        }

        this.doubleBackToExitPressedOnce = true
        val toast = Toast.makeText(this, "Tekan 1 kali lagi untuk keluar", Toast.LENGTH_SHORT)
        toast.show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000) // Reset after 2 seconds
    }

    // Fungsi untuk animasi klik
    private fun animateClick(view: View, action: () -> Unit) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f)
            )
            duration = 100
        }

        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f)
            )
            duration = 100
        }

        scaleDown.start()
        scaleDown.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {}
            override fun onAnimationEnd(animation: android.animation.Animator) {
                scaleUp.start()
                Handler(Looper.getMainLooper()).postDelayed({
                    action()
                }, 50)
            }
            override fun onAnimationCancel(animation: android.animation.Animator) {}
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
    }

}