package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.stream.Collectors;

public class GetIdsCommand extends Command {
    public GetIdsCommand() {
        super("getids", "Holt die Namen und die IDs der Leute mit der Rolle", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("role", "Rolle", "Die Rolle, von der die IDs geholt werden sollen", ArgumentManagerTemplate.DiscordType.ID)
                .setExample("!getids 1234567889990076868")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Message m = e.getMessage();
        TextChannel tco = e.getChannel();
        Member member = e.getMember();
        Role r = e.getJDA().getRoleById(e.getArguments().getID("role"));
        tco.getGuild().findMembers(mem -> mem.getRoles().contains(r)).onSuccess(members -> tco.sendMessage(members.stream().map(mem -> mem.getEffectiveName() + ": " + mem.getId()).collect(Collectors.joining("\n"))).queue()).onError(t -> {
            t.printStackTrace();
            tco.sendMessage("Es ist ein Fehler beim Laden der Member aufgetreten!").queue();
        });
    }
}
