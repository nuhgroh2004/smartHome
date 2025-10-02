package com.example.smarthome

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var doubleBackToExitPressedOnce = false

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

        /* ------------------------ fungsional untuk tombol berpidah activity -------------------------- */
        binding.cardLampu.setOnClickListener {
            val intent = Intent(this, ControlLampuActivity::class.java)
            startActivity(intent)
        }
        binding.cardTandonAir.setOnClickListener {
            val intent = Intent(this, MonitoringTandonAirActivity::class.java)
            startActivity(intent)
        }
        binding.cardMonitoringRuangan.setOnClickListener {
            val intent = Intent(this, MonitoringRuanganActivity::class.java)
            startActivity(intent)
        }
        binding.notificationBell.setOnClickListener {
            val intent = Intent(this, NotificationActivity::class.java)
            startActivity(intent)
        }
        binding.cardMonitoringListrik.setOnClickListener {
            val intent = Intent(this, MonitoringListrikActivity::class.java)
            startActivity(intent)
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
}