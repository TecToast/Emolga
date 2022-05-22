package de.tectoast.emolga.buttons;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import static de.tectoast.emolga.commands.Command.*;

public class ControlCentralButton extends ButtonListener {
    public ControlCentralButton() {
        super("controlcentral");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) {
        boolean b = true;
        switch (name) {
            case "ej" -> emolgajson = load("./emolgadata.json");
            case "saveemolgajson" -> saveEmolgaJSON();
            default -> b = false;
        }
        if (b) e.reply("Done!").setEphemeral(true).queue();
        else e.reply("Not recognized! " + name).setEphemeral(true).queue();
    }
}
