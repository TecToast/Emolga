package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.stream.Collectors;

public class GetIdsCommand extends Command {
    public GetIdsCommand() {
        super("getids", "`!getids` <Rolle>`", CommandCategory.Flo);
    }

    @Override
    public void process(CommandEvent e) {
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        Member member = e.getMember();
        Role r;
        if (m.getMentionedRoles().size() == 0) {
            if (m.getContentDisplay().split(" ").length == 2) {
                try {
                    r = e.getGuild().getRoleById(m.getContentDisplay().split(" ")[1]);
                } catch (Exception ex) {
                    tco.sendMessage(member.getAsMention() + " Du musst eine Rolle angeben!").queue();
                    return;
                }
            } else {
                tco.sendMessage(member.getAsMention() + " Du musst eine Rolle angeben!").queue();
                return;
            }
        } else
            r = m.getMentionedRoles().get(0);
        tco.getGuild().findMembers(mem -> mem.getRoles().contains(r)).onSuccess(members -> tco.sendMessage(members.stream().map(mem -> mem.getEffectiveName() + ": " + mem.getId()).collect(Collectors.joining("\n"))).queue()).onError(t -> {
            t.printStackTrace();
            tco.sendMessage("Es ist ein Fehler beim Laden der Member aufgetreten!").queue();
        });
    }
}
