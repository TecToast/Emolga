package de.tectoast.emolga.selectmenus;

import de.tectoast.emolga.utils.automation.collection.ModalConfigurators;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

public class ModalConfiguratorSelectMenu extends MenuListener {
    public ModalConfiguratorSelectMenu() {
        super("modalconfigurator");
    }

    @Override
    public void process(SelectMenuInteractionEvent e, String name) {
        e.replyModal(
                ModalConfigurators.configurations.get(name).configurator().buildModal(Integer.parseInt(e.getValues().get(0)))
        ).queue();
    }
}
