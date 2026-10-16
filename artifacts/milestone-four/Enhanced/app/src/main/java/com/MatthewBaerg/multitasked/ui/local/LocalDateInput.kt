package com.MatthewBaerg.multitasked.ui.local

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Dates use midnight UTC, matching the timestamp convention of the shared task model. */
object LocalDateInput {
    private fun formatter() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
        timeZone = TimeZone.getTimeZone("UTC")
    }
    fun parse(text: String): Long? {
        val value = text.trim()
        if (value.isEmpty()) return null
        require(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(value)) { "Use YYYY-MM-DD for the date" }
        return try { requireNotNull(formatter().parse(value)).time }
        catch (_: java.text.ParseException) { throw IllegalArgumentException("Enter a valid calendar date") }
    }
    fun format(value: Long?): String = value?.let { formatter().format(Date(it)) } ?: ""
}
