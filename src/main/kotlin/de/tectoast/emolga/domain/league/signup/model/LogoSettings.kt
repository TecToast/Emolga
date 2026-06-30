package de.tectoast.emolga.domain.league.signup.model

import de.tectoast.emolga.ktor.utils.Config
import de.tectoast.emolga.ktor.utils.LongType
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
@Config("Logo-Einstellungen", "Wie/wo sollen die Logos landen?")
sealed interface LogoSettings {
    @Serializable
    @SerialName("Channel")
    @Config("Logo-Channel", "Die Logos landen in einem bestimmten Channel.")
    data class Channel(
        @Config(
            "Channel", "Der Channel, in dem die Logos landen sollen", LongType.CHANNEL
        ) @Contextual val channelId: Long = 0
    ) : LogoSettings

    @Serializable
    @SerialName("WithSignupMessage")
    @Config("An der Anmeldung", "Die Logos werden an die Anmeldenachricht selbst drangehängt.")
    data object WithSignupMessage : LogoSettings

    @Serializable
    @SerialName("NotInDiscord")
    @Config("Nicht im Discord", "Die Logos werden nicht im Discord gespeichert.")
    data object NotInDiscord : LogoSettings
}
