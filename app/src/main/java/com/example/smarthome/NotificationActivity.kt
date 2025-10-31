package com.example.smarthome

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var notificationDb: NotificationDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        notificationDb = NotificationDatabase(this)
        setupRecyclerView()
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
        markAllNotificationsAsRead()
    }

    private fun markAllNotificationsAsRead() {
        val notifications = notificationDb.getAllNotifications()
        val hasUnread = notifications.any { !it.isRead }
        if (hasUnread) {
            notificationDb.markAllAsRead()
            Handler(Looper.getMainLooper()).postDelayed({
                loadNotifications()
            }, 300)
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_notifications)
        recyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(emptyList()) { notification ->
        }
        recyclerView.adapter = notificationAdapter
        loadNotifications()
    }

    private fun loadNotifications() {
        val notifications = notificationDb.getAllNotifications()
        if (notifications.isEmpty()) {
            generateDummyNotifications()
            notificationAdapter.updateData(notificationDb.getAllNotifications())
        } else {
            notificationAdapter.updateData(notifications)
        }
    }

    private fun generateDummyNotifications() {
        val dummyData = listOf(
            "Sensor Asap" to "Asap terdeteksi berpotensi kebakaran",
            "Tandon Air" to "Tandon air penuh",
            "Sensor Asap" to "Asap terdeteksi berpotensi kebakaran",
            "Tandon Air" to "Tandon air penuh",
            "Sensor Asap" to "Asap terdeteksi berpotensi kebakaran"
        )
        dummyData.forEach { (title, message) ->
            notificationDb.saveNotification(title, message)
        }
    }
}