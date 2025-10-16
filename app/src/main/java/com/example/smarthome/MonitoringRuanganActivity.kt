package com.example.smarthome

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthome.databinding.ActivityMonitoringRuanganBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MonitoringRuanganActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMonitoringRuanganBinding
    private lateinit var firebase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMonitoringRuanganBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebase = FirebaseDatabase.getInstance()
        setupFirebaseListener()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupFirebaseListener() {
        firebase.reference.child("IoTSystem").child("Lingkungan")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        android.util.Log.d("FirebaseData", "Lingkungan Snapshot exists: ${snapshot.exists()}")
                        android.util.Log.d("FirebaseData", "Lingkungan Full data: ${snapshot.value}")

                        // Get temperature
                        val suhuRaw = snapshot.child("suhu").value
                        val suhu = when (suhuRaw) {
                            is Double -> suhuRaw
                            is Float -> suhuRaw.toDouble()
                            is Long -> suhuRaw.toDouble()
                            is Int -> suhuRaw.toDouble()
                            is String -> suhuRaw.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        // Get humidity
                        val kelembabanRaw = snapshot.child("kelembapan").value
                        val kelembaban = when (kelembabanRaw) {
                            is Double -> kelembabanRaw
                            is Float -> kelembabanRaw.toDouble()
                            is Long -> kelembabanRaw.toDouble()
                            is Int -> kelembabanRaw.toDouble()
                            is String -> kelembabanRaw.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }

                        val status = snapshot.child("status").getValue(String::class.java) ?: "NORMAL"

                        android.util.Log.d("FirebaseData", "Suhu: $suhu, Kelembaban: $kelembaban, Status: $status")

                        updateTemperatureUI(suhu)
                        updateHumidityUI(kelembaban)

                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseError", "Error processing Lingkungan data", e)
                        e.printStackTrace()
                        updateTemperatureUI(0.0)
                        updateHumidityUI(0.0)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("FirebaseError", "Database error: ${error.message}")
                    updateTemperatureUI(0.0)
                    updateHumidityUI(0.0)
                }
            })
    }

    private fun updateTemperatureUI(suhu: Double) {
        val suhuText = findViewById<TextView>(R.id.tv_temperature)
        // Format to 0 decimal place
        suhuText.text = String.format("%.0f", suhu)
    }

    private fun updateHumidityUI(kelembaban: Double) {
        val kelembabanText = findViewById<TextView>(R.id.tv_humidity)
        // Format to 0 decimal place
        kelembabanText.text = String.format("%.0f", kelembaban)
    }
}