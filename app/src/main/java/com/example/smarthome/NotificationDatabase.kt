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

    interface OnNotificationChangeListener {
        fun onNotificationChanged()
    }
    fun saveNotification(title: String, message: String): NotificationModel {
        val notifications = getAllNotifications().toMutableList()
        val newId = getNextId()
        val timestamp = generateTimestamp(0)
        val notification = NotificationModel(
            id = newId,
            title = title,
            message = message,
            timestamp = timestamp,
            isRead = false
        )
        notifications.add(0, notification)
        saveAllNotifications(notifications)
        notifyListeners()
        return notification
    }

    fun getAllNotifications(): List<NotificationModel> {
        val json = sharedPreferences.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        val type = object : TypeToken<List<NotificationModel>>() {}.type
        return gson.fromJson(json, type)
    }

    fun markAsRead(notificationId: Int) {
        val notifications = getAllNotifications().toMutableList()
        val index = notifications.indexOfFirst { it.id == notificationId }

        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
            saveAllNotifications(notifications)
            notifyListeners()
        }
    }

    fun markAllAsRead() {
        val notifications = getAllNotifications().map { it.copy(isRead = true) }
        saveAllNotifications(notifications)
        notifyListeners()
    }

    fun deleteNotification(notificationId: Int) {
        val notifications = getAllNotifications().toMutableList()
        notifications.removeIf { it.id == notificationId }
        saveAllNotifications(notifications)
        notifyListeners()
    }

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
        notifyListeners()
    }

    fun getUnreadCount(): Int {
        return getAllNotifications().count { !it.isRead }
    }

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
        val isSameDay = notificationTime.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                notificationTime.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        return if (isSameDay) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale("id", "ID"))
            timeFormat.format(notificationTime.time)
        } else {
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            val isYesterday = notificationTime.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    notificationTime.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
            when {
                isYesterday -> "Kemarin"
                else -> {
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
