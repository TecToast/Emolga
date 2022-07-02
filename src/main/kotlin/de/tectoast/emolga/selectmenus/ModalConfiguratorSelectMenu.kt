package de.tectoast.emolga.selectmenus

import de.tectoast.emolga.utils.automation.collection.ModalConfigurators
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent

class ModalConfiguratorSelectMenu : MenuListener("modalconfigurator") {
    override fun process(e: SelectMenuInteractionEvent, menuname: String?) {
        e.replyModal(
            ModalConfigurators.configurations[menuname]!!.configurator().buildModal(e.values[0].toInt())
        ).queue()
    }
}