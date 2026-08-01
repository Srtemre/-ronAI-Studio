package com.htmltoapk.studio.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

    fun format(ts: Long): String =
        if (ts <= 0L) "—" else fmt.format(Date(ts))
}
