package de.tectoast.emolga.selectmenus;

import de.tectoast.emolga.buttons.buttonsaves.TrainerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

import java.awt.*;

import static de.tectoast.emolga.commands.Command.getTrainerDataActionRow;
import static de.tectoast.emolga.commands.Command.trainerDataButtons;

public class TrainerDataMenu extends MenuListener {
    public TrainerDataMenu() {
        super("trainerdata");
    }

    @Override
    public void process(SelectMenuInteractionEvent e) {
        TrainerData dt = trainerDataButtons.get(e.getMessageIdLong());
        if (dt == null) {
            e.editMessageEmbeds(new EmbedBuilder().setTitle("Ach Mensch " + e.getMember().getEffectiveName() + ", diese Trainer-Data funktioniert nicht mehr, da seitdem der Bot neugestartet wurde!").setColor(Color.CYAN).build()).queue();
            return;
        }
        boolean withMoveset = dt.isWithMoveset();
        String name = e.getValues().get(0);
        dt.setCurrent(name);
        e.editMessageEmbeds(new EmbedBuilder().setColor(Color.CYAN).setTitle(dt.getNormalName(name)).setDescription(dt.getMonsFrom(name, withMoveset)).build()).setActionRows(getTrainerDataActionRow(dt, withMoveset)).queue();
    }
}
