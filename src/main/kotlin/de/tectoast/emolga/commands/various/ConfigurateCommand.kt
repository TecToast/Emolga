package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.automation.collection.ModalConfigurators
import org.slf4j.LoggerFactory

class ConfigurateCommand : Command("configurate", "Konfiguriert verschiedene Sachen", CommandCategory.Various) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("configuration", "Konfiguration", "Die Konfiguration, die du ändern möchtest",
                ArgumentManagerTemplate.Text.of(
                    ModalConfigurators.configurations.keys
                        .map {
                            SubCommand(
                                it, "Einstellungen für \"${ModalConfigurators.configurations[it]!!.name}\""
                            )
                        },
                    true
                ))
            .setExample("/configurate DexQuiz")
            .build()
        slash(true, 918865966136455249L, Constants.FPLID, Constants.CULTID)
    }

    override fun process(e: GuildCommandEvent) {
        val conf = e.arguments.getText("configuration")
        logger.info("configurations = {}", ModalConfigurators.configurations)
        logger.info("conf = {}", conf)
        ModalConfigurators.configurations[conf]!!.configurator().initialize(e.slashCommandEvent!!)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ConfigurateCommand::class.java)
    }
}