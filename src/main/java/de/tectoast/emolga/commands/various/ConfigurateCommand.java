package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.tectoast.emolga.utils.automation.collection.ModalConfigurators.configurations;

public class ConfigurateCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurateCommand.class);

    public ConfigurateCommand() {
        super("configurate", "Konfiguriert verschiedene Sachen", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("configuration", "Konfiguration", "Die Konfiguration, die du ändern möchtest",
                        ArgumentManagerTemplate.Text.of(
                                configurations.keySet().stream()
                                        .map(c -> new SubCommand(c, "Einstellungen für \"%s\"".formatted(configurations.get(c).name())))
                                        .toList(),
                                true
                        ))
                .setExample("/configurate DexQuiz")
                .build());
        slash(true, 918865966136455249L, Constants.FPLID);
    }

    @Override
    public void process(GuildCommandEvent e) {
        String conf = e.getArguments().getText("configuration");
        logger.info("configurations = {}", configurations);
        logger.info("conf = {}", conf);
        configurations.get(conf).configurator().initialize(e.getSlashCommandEvent());
    }
}
