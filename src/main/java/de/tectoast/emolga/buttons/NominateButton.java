package de.tectoast.emolga.buttons;

import de.tectoast.emolga.buttons.buttonsaves.Nominate;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import static de.tectoast.emolga.commands.Command.nominateButtons;

public class NominateButton extends ButtonListener {

    public NominateButton() {
        super("nominate");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) {
        Nominate n = nominateButtons.get(e.getMessageIdLong());
        if (n == null) {
            e.reply("Diese Nachricht ist veraltet! Nutze erneut `!nominate`!").queue();
            return;
        }
        if (e.getComponent().getStyle() == ButtonStyle.PRIMARY) {
            n.unnominate(name);
            n.render(e);
        } else if (e.getComponent().getStyle() == ButtonStyle.SECONDARY) {
            n.nominate(name);
            n.render(e);
        } else if (e.getComponent().getStyle() == ButtonStyle.SUCCESS) {
            n.finish(e, name.equals("FINISHNOW"));
        } else if (e.getComponent().getStyle() == ButtonStyle.DANGER) {
            n.render(e);
        }
    }
}
