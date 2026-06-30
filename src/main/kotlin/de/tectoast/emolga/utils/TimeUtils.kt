package de.tectoast.emolga.utils

import kotlinx.datetime.*
import java.time.LocalDateTime
import kotlin.time.Instant

fun Instant.toJavaLocalDateTime() = toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime()
fun LocalDateTime.toKotlinInstant() = toKotlinLocalDateTime().toInstant(TimeZone.currentSystemDefault())