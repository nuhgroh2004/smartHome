package com.example.smarthome

import android.content.Context
import android.content.SharedPreferences
import com.example.smarthome.model.NotificationModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class NotificationDatabase(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_LAST_ID = "last_id"

        // Listener untuk perubahan notifikasi
        private val listeners = mutableListOf<OnNotificationChangeListener>()

        fun addListener(listener: OnNotificationChangeListener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        fun removeListener(listener: OnNotificationChangeListener) {
            listeners.remove(listener)
        }

        private fun notifyListeners() {
            listeners.forEach { it.onNotificationChanged() }
        }
    }

    // Interface untuk listener
    interface OnNotificationChangeListener {
        fun onNotificationChanged()
    }

    // Menyimpan notifikasi baru
    fun saveNotification(title: String, message: String): NotificationModel {
        val notifications = getAllNotifications().toMutableList()

        // Generate ID baru
        val newId = getNextId()

        // Buat timestamp
        val timestamp = generateTimestamp(0)

        // Buat notifikasi baru dengan status unread
        val notification = NotificationModel(
            id = newId,
            title = title,
            message = message,
            timestamp = timestamp,
            isRead = false
        )

        // Tambahkan ke list (paling atas)
        notifications.add(0, notification)

        // Simpan ke SharedPreferences
        saveAllNotifications(notifications)

        // Notify semua listeners
        notifyListeners()

        return notification
    }

    // Ambil semua notifikasi
    fun getAllNotifications(): List<NotificationModel> {
        val json = sharedPreferences.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<NotificationModel>>() {}.type
        return gson.fromJson(json, type)
    }

    // Tandai notifikasi sebagai sudah dibaca
    fun markAsRead(notificationId: Int) {
        val notifications = getAllNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }

        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveAllNotifications(notifications)
            notifyListeners()
        }
    }

    // Tandai semua notifikasi sebagai sudah dibaca
    fun markAllAsRead() {
        val notifications = getAllNotifications().map { it.copy(isRead = true) }
        saveAllNotifications(notifications)
        notifyListeners()
    }

    // Hapus notifikasi
    fun deleteNotification(notificationId: Int) {
        val notifications = getAllNotifications().toMutableList()
        notifications.removeIf { it.id == notificationId }
        saveAllNotifications(notifications)
        notifyListeners()
    }

    // Hapus semua notifikasi
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        notifyListeners()
    }

    // Hitung jumlah notifikasi yang belum dibaca
    fun getUnreadCount(): Int {
        return getAllNotifications().count { !it.isRead }
    }

    // Private helper functions
    private fun saveAllNotifications(notifications: List<NotificationModel>) {
        val json = gson.toJson(notifications)
        sharedPreferences.edit().putString(KEY_NOTIFICATIONS, json).apply()
    }

    private fun getNextId(): Int {
        val lastId = sharedPreferences.getInt(KEY_LAST_ID, 0)
        val newId = lastId + 1
        sharedPreferences.edit().putInt(KEY_LAST_ID, newId).apply()
        return newId
    }

    private fun generateTimestamp(hoursAgo: Int): String {
        val notificationTime = Calendar.getInstance()
        notificationTime.add(Calendar.HOUR_OF_DAY, -hoursAgo)

        val now = Calendar.getInstance()

        // Cek apakah notifikasi di hari yang sama
        val isSameDay = notificationTime.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                notificationTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

        return if (isSameDay) {
            // Jika masih hari yang sama, tampilkan jam
            val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            timeFormat.format(notificationTime.time)
        } else {
            // Jika sudah berganti hari, tampilkan tanggal
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)

            val isYesterday = notificationTime.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    notificationTime.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

            when {
                isYesterday -> "Kemarin"
                else -> {
                    // Format: "16 Sep" atau "16 Sep 2024" jika tahun berbeda
                    val dateFormat = if (notificationTime.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                        SimpleDateFormat("dd MMM", Locale("id", "ID"))
                    } else {
                        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                    }
                    dateFormat.format(notificationTime.time)
                }
            }
        }
    }
}
