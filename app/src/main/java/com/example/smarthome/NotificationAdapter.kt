package com.example.smarthome

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthome.model.NotificationModel

class NotificationAdapter(
    private val notifications: List<NotificationModel>
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.iv_notification_icon)
        val titleTextView: TextView = itemView.findViewById(R.id.tv_notification_title)
        val messageTextView: TextView = itemView.findViewById(R.id.tv_notification_message)
        val timestampTextView: TextView = itemView.findViewById(R.id.tv_notification_timestamp)
        val unreadIndicator: View = itemView.findViewById(R.id.v_unread_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        holder.titleTextView.text = notification.title
        holder.messageTextView.text = notification.message
        holder.timestampTextView.text = notification.timestamp

        // Set icon berdasarkan title atau tipe notifikasi
        when {
            notification.title.contains("Tandon", ignoreCase = true) -> {
                holder.iconImageView.setImageResource(R.drawable.notification_toren_air_img)
            }
            notification.title.contains("Asap", ignoreCase = true) ||
            notification.title.contains("Pening", ignoreCase = true) -> {
                holder.iconImageView.setImageResource(R.drawable.notification_asap_img)
            }
            else -> {
                holder.iconImageView.setImageResource(R.drawable.notification_toren_air_img)
            }
        }

        // Show/hide unread indicator
        holder.unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = notifications.size
}