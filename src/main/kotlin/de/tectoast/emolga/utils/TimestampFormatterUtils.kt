package de.tectoast.emolga.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

fun DateTimeFormatter.parseToInstant(str: String): Instant {
    val localDateTime = LocalDateTime.parse(str, this)
    return Instant.fromEpochSeconds(localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond())
}

fun DateTimeFormatter.format(instant: Instant): String = format(instant.toJavaInstant())
