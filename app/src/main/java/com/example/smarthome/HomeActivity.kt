package com.example.smarthome

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

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
}