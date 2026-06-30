package de.tectoast.emolga.domain.league.signup.model

import de.tectoast.emolga.ktor.utils.Config
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Config("Anmeldungseingabe", "Eine Option, die bei der Anmeldung angegeben werden muss")
sealed interface SignupInput {
    val id: String

    @Serializable
    @SerialName("SDName")
    @Config("Showdown-Name", "Selbsterklärend", prio = 2)
    data object SDName : SignupInput {
        override val id = SDNAME_ID
    }

    @Serializable
    @SerialName("TeamName")
    @Config("Team-Name", "Selbsterklärend", prio = 1)
    data object TeamName : SignupInput {
        override val id = TEAMNAME_ID
    }

    @Serializable
    @SerialName("OfList")
    @Config(
        "Aus einer Liste",
        "Für den Fall, dass die Teilnehmenden aus einer Menge an Optionen auswählen sollen (zum Beispiel Ligapräferenzen)"
    )
    data class OfList(
        @Config("Name", "Der Name dieser Option, z.B. Liga") val name: String = "Liga",
        @Config("Liste", "Die Liste, aus der man auswählen soll") val list: List<String> = listOf(),
        @Config(
            "Sichtbar für alle", "Ob die Auswahl in der Anmeldungsnachricht des Teilnehmenden erscheinen soll"
        ) val visibleForAll: Boolean = true
    ) : SignupInput {
        override val id = "$OFLIST_PREFIX_ID$name"
    }

    @Serializable
    @SerialName("YTChannel")
    @Config(
        "YT-Channel",
        "Wenn dies eine YouTube-Liga ist, können die Teilnehmenden hier ihren Kanal angeben, damit später die Automatisierungen funktionieren"
    )
    data object YTChannel : SignupInput {
        override val id = YT_CHANNEL_ID
    }

    companion object {
        const val SDNAME_ID = "sdname"
        const val TEAMNAME_ID = "teamname"
        const val OFLIST_PREFIX_ID = "oflist_"
        const val LOGO_ID = "logo"
        const val YT_CHANNEL_ID = "ytchannel"
        const val TEAMMATE_ID = "teammate"
    }
}