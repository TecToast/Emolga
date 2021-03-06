package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.database.Database;
import org.json.JSONObject;

public class SpoilerTagsCommand extends Command {

    public SpoilerTagsCommand() {
        super("spoilertags", "Aktiviert oder deaktiviert den Spoilerschutz bei Showdown-Ergebnissen. (Gilt serverweit)", CommandCategory.Showdown);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        everywhere = true;
    }

    @Override
    public void process(GuildCommandEvent e) {
        long gid = e.getGuild().getIdLong();
        JSONObject json = getEmolgaJSON();
        if (Database.update("delete from spoilertags where guildid = " + gid) != 0) {
            e.getChannel().sendMessage("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **deaktiviert**!").queue();
            Command.spoilerTags.remove(gid);
            return;
        }
        Database.insert("spoilertags", "guildid", gid);
        Command.spoilerTags.add(gid);
        e.getChannel().sendMessage("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **aktiviert**!").queue();
    }
}
