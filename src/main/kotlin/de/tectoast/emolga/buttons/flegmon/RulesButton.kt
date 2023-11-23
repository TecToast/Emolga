package de.tectoast.emolga.buttons.flegmon

import de.tectoast.emolga.buttons.ButtonListener
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object RulesButton : ButtonListener("ruleaccept") {
    private const val ACCEPTED_RULES_ROLE = 605635673885507614
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val g = e.guild!!
        g.addRoleToMember(e.user, g.getRoleById(ACCEPTED_RULES_ROLE)!!).queue()
        e.reply_("Du hast die Regeln akzeptiert und hast jetzt Zugriff auf den Server!", ephemeral = true).queue()
    }
}
