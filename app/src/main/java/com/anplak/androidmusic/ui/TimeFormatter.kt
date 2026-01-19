package com.anplak.androidmusic.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility object for formatting timestamps and durations for UI display.
 */
object TimeFormatter {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    /**
     * Formats a timestamp for display in the history list.
     * - "10:42 AM today" for today
     * - "Yesterday 3:15 PM" for yesterday
     * - "Jan 15, 2:30 PM" for older dates
     */
    fun formatPlayTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        val date = Date(timestamp)

        return when {
            isSameDay(now, then) -> {
                "${timeFormat.format(date)} today"
            }
            isYesterday(now, then) -> {
                "Yesterday ${timeFormat.format(date)}"
            }
            else -> {
                fullDateFormat.format(date)
            }
        }
    }

    /**
     * Formats a duration in milliseconds for display.
     * - "3:24" for durations under an hour
     * - "1:23:45" for durations of an hour or more
     */
    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats total play time for display in insights.
     * - "2h 15m" for durations with hours
     * - "45m" for durations under an hour
     * - "< 1m" for durations under a minute
     */
    fun formatTotalPlayTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            millis > 0 -> "< 1m"
            else -> "0m"
        }
    }

    /**
     * Formats a relative time description for display.
     * - "Just now" for < 1 minute ago
     * - "5m ago" for < 1 hour ago
     * - "2h ago" for < 24 hours ago
     * - "Yesterday" for yesterday
     * - "Jan 15" for older dates
     */
    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        val nowCal = Calendar.getInstance()
        val thenCal = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 && isSameDay(nowCal, thenCal) -> "${hours}h ago"
            isYesterday(nowCal, thenCal) -> "Yesterday"
            else -> dateFormat.format(Date(timestamp))
        }
    }

    /**
     * Formats a track position as "current / total".
     * - "3:24 / 4:15" format
     */
    fun formatPositionWithTotal(currentMillis: Long, totalMillis: Long): String {
        return "${formatDuration(currentMillis)} / ${formatDuration(totalMillis)}"
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, then: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterday, then)
    }
}
