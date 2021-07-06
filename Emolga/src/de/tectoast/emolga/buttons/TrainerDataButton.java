package de.tectoast.emolga.buttons;

import de.tectoast.emolga.utils.TrainerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;

import java.awt.*;

import static de.tectoast.emolga.commands.Command.getTrainerDataButtons;
import static de.tectoast.emolga.commands.Command.trainerData;

public class TrainerDataButton extends ButtonListener {

    public TrainerDataButton() {
        super("trainerdata");
    }

    @Override
    public void process(ButtonClickEvent e, String name) {
        TrainerData dt = trainerData.get(e.getMessageIdLong());
        if (dt == null) {
            e.editMessageEmbeds(new EmbedBuilder().setTitle("Ach Mensch " + e.getMember().getEffectiveName() + ", diese Trainer-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!").setColor(Color.CYAN).build()).queue();
            return;
        }
        boolean withMoveset = dt.isWithMoveset();
        if (name.equals("CHANGEMODE")) {
            dt.swapWithMoveset();
            e.editComponents(getTrainerDataButtons(dt, !withMoveset)).queue();
            String title = e.getMessage().getEmbeds().get(0).getTitle();
            if (!title.contains("Und sollen die Moves etc auch angezeigt werden")) {
                e.getHook().editOriginalEmbeds(new EmbedBuilder().setColor(Color.CYAN).setTitle(title).setDescription(dt.getMonsFrom(title, !withMoveset)).build()).queue();
            }
            return;
        }
        e.editMessageEmbeds(new EmbedBuilder().setColor(Color.CYAN).setTitle(dt.getNormalName(name)).setDescription(dt.getMonsFrom(name, withMoveset)).build()).queue();
    }
}
