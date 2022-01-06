package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.buttons.buttonsaves.TrainerData;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class TrainerDataCommand extends Command {

    public TrainerDataCommand() {
        super("trainerdata", "Zeigt die Pokemon eines Arenaleiters/Top 4 Mitglieds an", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("trainer", "Arenaleiter/Top4", "Der Arenaleiter/Das Top 4 Mitglied, von dem du das Team wissen möchtest", Translation.Type.TRAINER.or("Tom")).setExample("!trainerdata Skyla").build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String trainer = e.getArguments().getTranslation("trainer").getTranslation();
        if(trainer.equals("Tom")) {
            e.reply("Kleinstein und Machollo auf Level 11 :) Machollo hat die beste Fähigkeit im Spiel :D");
            return;
        }
        TrainerData dt = new TrainerData(trainer);
        e.getChannel().sendMessageEmbeds(new EmbedBuilder().setTitle("Welches Team möchtest du sehen? Und sollen die Moves etc auch angezeigt werden?").setColor(Color.CYAN).build()).setActionRows(getTrainerDataActionRow(dt, false)).queue(m -> trainerDataButtons.put(m.getIdLong(), dt));
    }
}
