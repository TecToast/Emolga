package de.tectoast.commands.draft;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.draft.Draft;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class DraftsetupCommand extends Command {
    public DraftsetupCommand() {
        super("draftsetup", "`!draftsetup <Name> <Textchannel>` Startet das Draften der Liga in diesem Channel (im angegebenen Textchannel werden die Teams angezeigt)", CommandCategory.Draft);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String name = msg.split(" ")[1];
        String tcid = m.getMentionedChannels().get(0).getId();
        new Draft(tco, name, tcid, false);
    }
}
