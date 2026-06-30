package de.tectoast.emolga.domain.league.signup.model

import de.tectoast.emolga.ktor.utils.Config
import de.tectoast.emolga.ktor.utils.LongType
import kotlinx.serialization.Contextual
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
@Config(name = "Initiale Anmeldungskonfiguration", "Die initiale Konfiguration einer Liga-Anmeldung")
data class LeagueSignupConfig(
    @Config(
        name = "Anmeldungschannel",
        "In welchem Channel sollen die Anmeldungen von Emolga gesammelt werden?",
        longType = LongType.CHANNEL
    ) @Contextual val signupChannel: Long, @Config(
        name = "Anmeldungsnachricht", "Was soll in der Anmeldungsnachricht von Emolga stehen?"
    ) val signupMessage: String, @Config(
        name = "Ankündingungschannel",
        "In welchem Channel soll die Anmeldungsnachricht stehen?",
        longType = LongType.CHANNEL
    ) @Contextual val announceChannel: Long, @Config(
        "Logo-Einstellungen", "Hier kannst du einstellen, ob/wie Logos eingesendet werden."
    ) val logoSettings: LogoSettings? = null, @Config(
        "Maximale Anzahl",
        "Hier kannst du einstellen, bei wie vielen Teilnehmenden die Anmeldung geschlossen werden soll. Bei 0 gibt es keine Begrenzung."
    ) var maxUsers: Int, @Config(
        "Versteckte Spieleranzahl",
        "Hier kannst du einstellen, ob die aktuelle Teilnehmeranzahl in der Anmeldungsnachricht angezeigt werden soll."
    ) var hideUserCount: Boolean = false, @Config(
        "Teams erlaubt",
        "Hier kannst du einstellen, ob man teamen darf",
    ) var allowTeams: Boolean = false, @Config(
        "Teilnehmerrolle",
        "Hier kannst du eine Rolle einstellen, die die Teilnehmer automatisch bekommen sollen.",
        LongType.ROLE
    ) @Contextual val participantRole: Long? = null, @Config(
        "Anmeldungsstruktur", "Hier kannst du einstellen, was die Teilnehmer alles bei der Anmeldung angeben sollen."
    ) val signupStructure: List<SignupInput> = listOf(), @Config(
        "Identifier", "Identifier zum Unterscheiden von verschiedenen Anmeldungen auf einem Server"
    ) @EncodeDefault val identifier: String = ""
) {
    val maxUsersAsString
        get() = maxUsers.takeIf { it > 0 }?.toString() ?: "?"

    fun getInputConfig(id: String) = signupStructure.firstOrNull { it.id == id }
}