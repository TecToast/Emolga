package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.utils.DBF
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

object DumbestFliesMenu : MenuListener("dumbestflies") {
    override suspend fun process(e: StringSelectInteractionEvent, menuname: String?) {
        DBF.addVote(e)
    }
}
