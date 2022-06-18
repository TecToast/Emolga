package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class StatisticsCommand extends Command {

    public StatisticsCommand() {
        super("statistics", "Zeigt Statistiken über die Usage des Bots an", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.reply(new EmbedBuilder().setColor(Color.CYAN).setTitle("Anzahl der Nutzung")
                .setDescription(DBManagers.STATISTICS.buildDescription(e)).build());
    }
}
