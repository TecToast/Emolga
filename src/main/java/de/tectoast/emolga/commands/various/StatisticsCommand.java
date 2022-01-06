package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.database.Database;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.sql.ResultSet;
import java.util.ArrayList;

public class StatisticsCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsCommand.class);

    public StatisticsCommand() {
        super("statistics", "Zeigt Statistiken über die Usage des Bots an", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        ResultSet set = Database.select("SELECT * FROM `statistics` ORDER BY `count` DESC");
        String analysis = "";
        ArrayList<String> otherCmds = new ArrayList<>();
        while (set.next()) {
            int count = set.getInt("count");
            String name = set.getString("name");
            if (name.equals("analysis")) analysis = "Analysierte Replays: " + count;
            else {
                Command c = byName(name.substring(4));
                logger.info("name = " + name);
                if (c.checkBot(e.getJDA(), e.getGuild().getIdLong()))
                    otherCmds.add(c.getPrefix() + c.getName() + ": " + count);
            }
        }
        e.reply(new EmbedBuilder().setColor(Color.CYAN).setTitle("Anzahl der Nutzung").setDescription(analysis + "\n" + String.join("\n", otherCmds)).build());
    }
}
