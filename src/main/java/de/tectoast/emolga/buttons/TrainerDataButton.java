package de.tectoast.emolga.buttons;

import de.tectoast.emolga.buttons.buttonsaves.TrainerData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;

import static de.tectoast.emolga.commands.Command.getTrainerDataActionRow;
import static de.tectoast.emolga.commands.Command.trainerDataButtons;

public class TrainerDataButton extends ButtonListener {

    public TrainerDataButton() {
        super("trainerdata");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) {
        TrainerData dt = trainerDataButtons.get(e.getMessageIdLong());
        if (dt == null) {
            e.reply("Dieses Trainer-Data funktioniert nicht mehr, da der Bot seit der Erstellung neugestartet wurde. Bitte ruf den Command nochmal auf :)").setEphemeral(true).queue();
            e.getHook().deleteOriginal().queue();
            return;
        }
        boolean withMoveset = dt.isWithMoveset();
        if (name.equals("CHANGEMODE")) {
            dt.swapWithMoveset();
            e.editComponents(getTrainerDataActionRow(dt, !withMoveset)).queue();
            String title = e.getMessage().getEmbeds().get(0).getTitle();
            if (!title.contains("Und sollen die Moves etc auch angezeigt werden")) {
                e.getHook().editOriginalEmbeds(new EmbedBuilder().setColor(Color.CYAN).setTitle(title).setDescription(dt.getMonsFrom(title, !withMoveset)).build()).queue();
            }
            //return;
        }
        //e.editMessageEmbeds(new EmbedBuilder().setColor(Color.CYAN).setTitle(dt.getNormalName(name)).setDescription(dt.getMonsFrom(name, withMoveset)).build()).queue();
    }
}
