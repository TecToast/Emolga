package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class TempMuteCommand extends Command {
    public TempMuteCommand() {
        super("tempmute", "Muted den User temporär", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "User, der getempmuted werden soll", ArgumentManagerTemplate.DiscordType.USER)
                .add("time", "Zeit", "Zeitspanne, für die der User gemutet werden soll", ArgumentManagerTemplate.Text.any())
                .add("reason", "Grund", "Grund des Tempmutes", ArgumentManagerTemplate.Text.any())
                .setNoCheck(true)
                .setExample("!tempban @BöserUser123 1d Hat böse Wörter verwendet")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Message m = e.getMessage();
        String raw = m.getContentRaw();
        TextChannel tco = e.getChannel();
        if (m.getMentionedMembers().size() != 1) {
            //tco.sendMessage("Du musst einen Spieler taggen!").queue();
            return;
        }
        Member mem = m.getMentionedMembers().get(0);
        String[] splitarr = raw.split(" ");
        //ArrayList<String> split = new ArrayList<>(Arrays.asList(splitarr));
        StringBuilder reasonbuilder = new StringBuilder();
        int time = 0;
        for (int i = 2; i < splitarr.length; i++) {
            if (parseShortTime(splitarr[i]) != -1) {
                time += parseShortTime(splitarr[i]);
            } else reasonbuilder.append(splitarr[i]).append(" ");
        }
        String reason = reasonbuilder.toString().trim().equals("") ? "Nicht angegeben" : reasonbuilder.toString().trim();
        tempMute(tco, e.getMember(), mem, time, reason);
    }
}
