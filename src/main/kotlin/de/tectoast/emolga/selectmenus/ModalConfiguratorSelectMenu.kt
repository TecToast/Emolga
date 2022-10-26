package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.utils.automation.collection.ModalConfigurators
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

class ModalConfiguratorSelectMenu : MenuListener("modalconfigurator") {
    override fun process(e: StringSelectInteractionEvent, menuname: String?) {
        e.replyModal(
            ModalConfigurators.configurations[menuname]!!.configurator().buildModal(e.values[0].toInt())
        ).queue()
    }
}
