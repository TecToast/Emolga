package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.DateToStringSerializer
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class TipGame(
    val tips: MutableMap<Int, TipGamedayData> = mutableMapOf(),
    @Serializable(with = DateToStringSerializer::class)
    val lastSending: Date,
    @Serializable(with = DateToStringSerializer::class)
    val lastLockButtons: Date,
    val interval: String,
    val amount: Int,
    val channel: Long
)

@Serializable
class TipGamedayData(
    val userdata: MutableMap<Long, MutableMap<Int, Int>> = mutableMapOf(),
    val evaluated: MutableList<Int> = mutableListOf()
)
