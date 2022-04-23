package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;

public class ResetCooldownCommand extends Command {
    public ResetCooldownCommand() {
        super("resetcooldown", "Resettet den Cooldown der angegebenen Person", CommandCategory.Moderator, Constants.BSID, Constants.ASLID);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, dessen Cooldown resettet werden soll", ArgumentManagerTemplate.DiscordType.USER)
                .setExample("!resetcooldown @CoolerUser")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Member mem = e.getArguments().getMember("user");
        String name = "**" + mem.getEffectiveName() + "**";
        JSONObject c = getEmolgaJSON().getJSONObject("cooldowns");
        String gid = e.getGuild().getId();
        if (!c.has(gid)) {
            e.reply("Auf diesem Server hat noch nie jemand seinen Namen geändert!");
            return;
        }
        String mid = mem.getId();
        JSONObject o = c.getJSONObject(gid);
        if (!o.has(mid)) {
            e.reply(name + " hat noch nie seinen Nickname geändert!");
            return;
        }
        long l = Long.parseLong(o.getString(mid));
        long untilnow = System.currentTimeMillis() - l;
        if (untilnow >= 604800000) {
            e.reply(name + " darf seinen Namen bereits wieder ändern!");
            return;
        }
        o.put(mid, "-1");
        e.reply("Der Cooldown von " + name + " wurde resettet!");
        saveEmolgaJSON();
    }
}
