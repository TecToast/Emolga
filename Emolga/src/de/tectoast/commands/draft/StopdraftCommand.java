package de.tectoast.commands.draft;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.draft.Draft;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.ArrayList;

public class StopdraftCommand extends Command {
    public StopdraftCommand() {
        super("stopdraft", "`!stopdraft <Name>` Beendet den draft", CommandCategory.Draft, true);
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
            tco.sendMessage("Dieser draft existiert nicht!").queue();
        } else {
            for (Draft draft : todel) {
                Draft.drafts.remove(draft);
            }
            tco.sendMessage("Dieser draft wurde beendet!").queue();
        }
    }
}
