package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

public class RevolutionCommand extends Command {

    public RevolutionCommand() {
        super("revolution", "muhahahahaha", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "Name", "Name der Revolution", ArgumentManagerTemplate.Text.any())
                .setExample("!revolution Emolga")
                .build());
        setCustomPermissions(PermissionPreset.CULT);
    }

    @Override
    public void process(GuildCommandEvent e) {
        if (e.getArgsLength() == 0) {
            e.reply("Du musst einen Revolution-Namen angeben!");
            return;
        }
        String name = e.getArg(0);
        Guild g = e.getGuild();
        JSONArray arr = getEmolgaJSON().getJSONArray("activerevolutions");
        boolean isRevo = arr.toList().contains(g.getIdLong());
        JSONObject o = new JSONObject();
        e.getChannel().sendMessage("Möge die **" + name + "-Revolution** beginnen! :D").queue();
        g.loadMembers().onSuccess(list -> {
            for (Member member : list) {
                if (member.isOwner()) continue;
                if (member.getId().equals(e.getJDA().getSelfUser().getId()))
                    member.modifyNickname(name + "leader").queue();
                if (!g.getSelfMember().canInteract(member)) continue;
                if (!isRevo)
                    o.put(member.getId(), member.getEffectiveName());
                member.modifyNickname(name).queue();
            }
            for (GuildChannel gc : g.getChannels()) {
                gc.getManager().setName((gc.getType() == ChannelType.TEXT ? name.toLowerCase() : name) + "-" + gc.getName()).queue();
            }
            if (!isRevo) {
                getEmolgaJSON().getJSONObject("revolutionreset").put(g.getId(), o);
                arr.put(g.getIdLong());
            }
            saveEmolgaJSON();
        });
    }
}
