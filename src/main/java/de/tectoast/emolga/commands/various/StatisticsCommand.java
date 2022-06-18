package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

public class StatisticsCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsCommand.class);

    public StatisticsCommand() {
        super("statistics", "Zeigt Statistiken über die Usage des Bots an", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        e.reply(new EmbedBuilder().setColor(Color.CYAN).setTitle("Anzahl der Nutzung")
                .setDescription(DBManagers.STATISTICS.buildDescription(e)).build());
    }
}
