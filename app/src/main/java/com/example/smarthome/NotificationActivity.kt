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

        // Inisialisasi database
        notificationDb = NotificationDatabase(this)

        setupRecyclerView()

        // Setup back button
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data setiap kali activity muncul
        loadNotifications()
        // Mark semua notifikasi sebagai sudah dibaca saat user melihat halaman ini
        markAllNotificationsAsRead()
    }

    private fun markAllNotificationsAsRead() {
        // Ambil semua notifikasi yang belum dibaca
        val notifications = notificationDb.getAllNotifications()
        val hasUnread = notifications.any { !it.isRead }

        if (hasUnread) {
            // Mark semua sebagai sudah dibaca
            notificationDb.markAllAsRead()
            // Refresh UI untuk menghilangkan unread indicator
            Handler(Looper.getMainLooper()).postDelayed({
                loadNotifications()
            }, 300) // Delay sedikit agar transisi terlihat smooth
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rv_notifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Buat adapter tanpa callback click karena tidak diperlukan lagi
        notificationAdapter = NotificationAdapter(emptyList()) { notification ->
            // Item click handler - bisa dikosongkan atau digunakan untuk aksi lain
            // Tidak perlu mark as read karena sudah otomatis saat activity dibuka
        }
        recyclerView.adapter = notificationAdapter

        // Load notifikasi dari database
        loadNotifications()
    }

    private fun loadNotifications() {
        // Ambil semua notifikasi dari database lokal
        val notifications = notificationDb.getAllNotifications()

        // Jika tidak ada data di database, generate dummy data untuk testing
        if (notifications.isEmpty()) {
            generateDummyNotifications()
            // Ambil lagi setelah generate dummy
            notificationAdapter.updateData(notificationDb.getAllNotifications())
        } else {
            notificationAdapter.updateData(notifications)
        }
    }

    private fun generateDummyNotifications() {
        // Generate dummy data untuk testing (hanya jika database kosong)
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