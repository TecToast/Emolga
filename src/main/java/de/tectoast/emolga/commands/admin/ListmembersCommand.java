package de.tectoast.emolga.commands.admin;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

public class ListmembersCommand extends Command {
    public ListmembersCommand() {
        super("listmembers", "Zeigt alle User an, die diese Rolle haben", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("role", "Rolle", "Die Rolle, die die User besitzen sollen", ArgumentManagerTemplate.DiscordType.ID)
                .setExample("!listmembers 760914483534889021")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        ArgumentManager args = e.getArguments();
        Role r = e.getJDA().getRoleById(args.getID("role"));
        tco.getGuild().findMembers(mem -> mem.getRoles().contains(r)).onSuccess(members -> {
            StringBuilder s = new StringBuilder();
            for (Member mem : members) {
                s.append(mem.getEffectiveName()).append("\n");
            }
            if (s.toString().isEmpty()) {
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
