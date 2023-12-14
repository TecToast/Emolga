package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.condAppend
import de.tectoast.emolga.managers.SignupManager
import de.tectoast.emolga.utils.json.LigaStartData
import de.tectoast.emolga.utils.json.SignUpData
import dev.minn.jda.ktx.interactions.components.Modal
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

object SignupModal : ModalListener("signup") {

    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val teamname = e.getValue("teamname")?.asString
        val sdname = e.getValue("sdname")!!.asString
        val experiences = e.getValue("experiences")?.asString
        SignupManager.signupUser(
            e.guild!!.idLong,
            e.user.idLong,
            sdname,
            teamname,
            experiences,
            isChange = name == "change",
            e = e
        )
    }

    fun getModal(data: SignUpData?, lsData: LigaStartData) =
        Modal("signup".condAppend(data != null, ";change"), "Anmeldung".condAppend(data != null, "sanpassung")) {
            if (!lsData.noTeam) short("teamname", "Team-Name", required = true, value = data?.teamname)
            short("sdname", "Showdown-Name", required = true, requiredLength = 1..18, value = data?.sdname)
            if (lsData.withExperiences) paragraph(
                "experiences",
                "Erfahrungen",
                required = true,
                placeholder = "Wie viel Erfahrung hast du im CP-Bereich?",
                value = data?.experiences
            )
        }

}
