package de.tectoast.emolga.modals

import de.tectoast.emolga.utils.automation.collection.ModalConfigurators
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class ConfiguratorModal : ModalListener("modalconfigurator") {
    override fun process(e: ModalInteractionEvent, name: String?) {
        ModalConfigurators.configurations[name]!!.configurator().handle(e)
    }
}