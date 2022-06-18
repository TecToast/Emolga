package de.tectoast.emolga.modals;

import de.tectoast.emolga.utils.automation.collection.ModalConfigurators;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class ConfiguratorModal extends ModalListener {
    public ConfiguratorModal() {
        super("modalconfigurator");
    }

    @Override
    public void process(ModalInteractionEvent e, String name) {
        ModalConfigurators.configurations.get(name).configurator().handle(e);
    }
}
