package de.Flori.Commands.Draft;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import de.Flori.utils.Draft.Draft;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;

public class StopdraftCommand extends Command {
    public StopdraftCommand() {
        super("stopdraft", "`!stopdraft <Name>` Beendet den Draft", CommandCategory.Draft, true);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String name = msg.substring(10);
        ArrayList<Draft> todel = new ArrayList<>();
        for (Draft draft : Draft.drafts) {
            if (draft.name.equals(name)) todel.add(draft);
        }
        if (todel.size() == 0) {
            tco.sendMessage("Dieser Draft existiert nicht!").queue();
        } else {
            for (Draft draft : todel) {
                Draft.drafts.remove(draft);
            }
            tco.sendMessage("Dieser Draft wurde beendet!").queue();
        }
    }
}
