package de.Flori.Commands.Admin;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ListmembersCommand extends Command {
    public ListmembersCommand() {
        super("listmembers", "`!listmembers <Rolle>` Zeigt alle User an, die diese Rolle haben", CommandCategory.Admin);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
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
        tco.getGuild().findMembers(mem -> mem.getRoles().contains(r)).onSuccess(members -> {
            StringBuilder s = new StringBuilder();
            for (Member mem : members) {
                s.append(mem.getEffectiveName()).append("\n");
            }
            if (s.toString().equals("")) {
                tco.sendMessage("Kein Member hat die Rolle " + r.getName() + "!").queue();
                return;
            }
            s.append("Insgesamt: ").append(members.size());
            tco.sendMessage("User mit der Rolle " + r.getName() + ":\n" + s).queue();
        }).onError(t -> {
            t.printStackTrace();
            tco.sendMessage("Es ist ein Fehler beim Laden der Member aufgetreten!").queue();
        });
    }
}
