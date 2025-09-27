package com.example.smarthome

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.model.NotificationModel
import java.text.SimpleDateFormat
import java.util.*

class NotificationActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()

        // Setup back button
        findViewById<android.widget.ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_notifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val notifications = generateDummyNotifications()
        notificationAdapter = NotificationAdapter(notifications)
        recyclerView.adapter = notificationAdapter
    }

    private fun generateDummyNotifications(): List<NotificationModel> {
        val notifications = mutableListOf<NotificationModel>()
        val calendar = Calendar.getInstance()

        // Generate 50 dummy notifications untuk testing scroll
        for (i in 1..50) {
            val isWaterNotification = i % 3 == 0 // Setiap 3 item akan jadi notifikasi air

            if (isWaterNotification) {
                notifications.add(
                    NotificationModel(
                        id = i,
                        title = "Tandon Air",
                        message = "Tandon air sudah penuh",
                        timestamp = generateTimestamp(calendar, i),
                        isRead = i % 5 != 0 // Setiap 5 item akan unread
                    )
                )
            } else {
                notifications.add(
                    NotificationModel(
                        id = i,
                        title = "Pendeteksi Asap",
                        message = "Asap terdeteksi berpotensi kebakaran",
                        timestamp = generateTimestamp(calendar, i),
                        isRead = i % 4 != 0 // Setiap 4 item akan unread
                    )
                )
            }
        }

        return notifications.sortedBy { it.id } // Sort terbaru dulu (id kecil = terbaru)
    }

    private fun generateTimestamp(calendar: Calendar, index: Int): String {
        calendar.add(Calendar.HOUR_OF_DAY, -index) // Mundur beberapa jam untuk setiap item

        val now = Calendar.getInstance()
        val diff = now.timeInMillis - calendar.timeInMillis
        val hours = diff / (1000 * 60 * 60)
        val days = hours / 24

        return when {
            hours < 1 -> "Baru saja"
            hours < 24 -> "${hours}j yang lalu"
            days == 1L -> "Kemarin"
            days < 30 -> "${days} hari lalu"
            else -> {
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                sdf.format(calendar.time)
            }
        }.also {
            calendar.add(Calendar.HOUR_OF_DAY, index) // Reset calendar
        }
    }
}