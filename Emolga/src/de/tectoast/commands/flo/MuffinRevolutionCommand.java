package de.tectoast.commands.flo;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

public class MuffinRevolutionCommand extends Command {
    public MuffinRevolutionCommand() {
        super("muffinrevolution", "`!muffinrevolution` MUFFINREVOLUTION", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Guild g = e.getGuild();
        JSONObject o = new JSONObject();
        e.getChannel().sendMessage("Möge die **Muffin-Revolution** beginnen! <:Muffin:814756664653774858>").queue();
        g.loadMembers().onSuccess(list -> {
            for (Member member : list) {
                if(member.isOwner()) continue;
                if(member.getId().equals(e.getJDA().getSelfUser().getId())) member.modifyNickname("Muffinleader").queue();
                if(!g.getSelfMember().canInteract(member)) continue;
                o.put(member.getId(), member.getEffectiveName());
                member.modifyNickname("Muffin").queue();
            }
            getEmolgaJSON().getJSONObject("muffinreset").put(g.getId(), o);
            saveEmolgaJSON();
        });
    }
}
