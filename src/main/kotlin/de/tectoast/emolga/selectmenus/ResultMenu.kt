package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.utils.draft.EnterResult
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

object ResultMenu : MenuListener("result") {
    override suspend fun process(e: StringSelectInteractionEvent, menuname: String?) {
        EnterResult.handleSelect(e, menuname!!)
    }

}
