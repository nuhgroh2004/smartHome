package com.example.smarthome.model

data class NotificationModel(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean
)
