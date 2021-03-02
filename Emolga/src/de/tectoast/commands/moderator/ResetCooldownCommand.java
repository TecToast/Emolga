package de.tectoast.commands.moderator;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.util.List;

public class ResetCooldownCommand extends Command {
    public ResetCooldownCommand() {
        super("resetcooldown", "`!resetcooldown @User` Resettet den Cooldown der angegebenen Person", CommandCategory.Moderator, byName("nickname"));
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        JSONObject json = getEmolgaJSON();
        List<Member> list = e.getMessage().getMentionedMembers();
        TextChannel tco = e.getChannel();
        String gid = e.getGuild().getId();
        if (list.size() != 1) {
            tco.sendMessage("Du musst einen Member taggen!").queue();
            return;
        }
        Member mem = list.get(0);
        String mid = mem.getId();
        String name = "**" + mem.getEffectiveName() + "**";
        JSONObject c = json.getJSONObject("cooldowns");
        if (!c.has(gid)) {
            tco.sendMessage("Auf diesem Server hat noch nie jemand seinen Namen geändert!").queue();
            return;
        }
        JSONObject o = c.getJSONObject(gid);
        if (!o.has(mid)) {
            tco.sendMessage(name + " hat noch nie seinen Nickname geändert!").queue();
            return;
        }
        long l = Long.parseLong(o.getString(mid));
        long untilnow = System.currentTimeMillis() - l;
        if (untilnow >= 604800000) {
            tco.sendMessage(name + " darf seinen Namen bereits wieder ändern!").queue();
            return;
        }
        o.put(mid, "-1");
        tco.sendMessage("Der Cooldown von " + name + " wurde resettet!").queue();
        saveEmolgaJSON();
    }
}
