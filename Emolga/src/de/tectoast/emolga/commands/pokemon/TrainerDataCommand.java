package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.TrainerData;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class TrainerDataCommand extends Command {

    public TrainerDataCommand() {
        super("trainerdata", "Zeigt die Pokemon eines Arenaleiters/Top 4 Mitglieds an", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("trainer", "Arenaleiter/Top4", "Der Arenaleiter/Das Top 4 Mitglied, von dem du das Team wissen möchtest", Translation.Type.TRAINER).setExample("!trainerdata Skyla").build());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        String trainer = e.getArguments().getTranslation("trainer").getTranslation();
        TrainerData dt = new TrainerData(trainer);
        e.getChannel().sendMessage(new EmbedBuilder().setTitle("Welches Team möchtest du sehen? Und sollen die Moves etc auch angezeigt werden?").setColor(Color.CYAN).build()).setActionRows(getTrainerDataButtons(dt, false)).queue(m -> trainerData.put(m.getIdLong(), dt));
    }
}
