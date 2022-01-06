package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.sql.DBManagers;

public class EnableMusicCommand extends Command {

    public EnableMusicCommand() {
        super("enablemusic", "Enabled Musik auf einem Server", CommandCategory.Flo);
    }

    @Override
    public void process(GuildCommandEvent e) {
        long id = e.getGuild().getIdLong();
        DBManagers.MUSIC_GUILDS.addGuild(id);
        CommandCategory.musicGuilds.add(id);
        e.reply("Alles klar, Meister :)");
    }
}
