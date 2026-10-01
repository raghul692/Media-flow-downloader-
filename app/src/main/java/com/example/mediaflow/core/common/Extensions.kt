package com.example.mediaflow.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    val clampedGroup = digitGroups.coerceIn(0, units.size - 1)
    val value = this / Math.pow(1024.0, clampedGroup.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[clampedGroup])
}

fun Long.formatDownloadSpeed(): String {
    if (this <= 0) return "0 KB/s"
    return "${(this).formatFileSize()}/s"
}

fun Long.formatDuration(): String {
    if (this <= 0) return "00:00"
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun Long.formatEta(): String {
    if (this <= 0) return "--"
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun Long.formatTimestamp(): String {
    if (this <= 0) return ""
    val formatter = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}
