package de.Flori.Commands.Draft;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class DeldraftCommand extends Command {
    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String name = msg.substring(10);
        JSONObject json = getEmolgaJSON();
        if (json.has("drafts")) {
            JSONObject drafts = json.getJSONObject("drafts");
            if (drafts.has(name)) {
                drafts.remove(name);
                saveEmolgaJSON();
                tco.sendMessage("Dieser Draft wurde gelöscht!").queue();
            } else {
                tco.sendMessage("Dieser Draft existiert nicht!").queue();
            }
        } else {
            tco.sendMessage("Es wurde noch kein Draft erstellt!").queue();
        }
    }

    public DeldraftCommand() {
        super("deldraft", "`!deldraft <Name>` Löscht den Draft mit dem angegebenen Namen", CommandCategory.Draft, true);
    }
}
