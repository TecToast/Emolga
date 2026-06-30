package de.tectoast.emolga.ktor.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Config(
    val name: String,
    val desc: String,
    val longType: LongType = LongType.NONE,
    val prio: Int = Int.MAX_VALUE,
    val keyIsDiscordUser: Boolean = false,
)


enum class LongType {
    NONE, CHANNEL, ROLE
}

fun List<Annotation>.findConfig(): Config? {
    return this.find { a -> a is Config } as Config?
}