package de.tectoast.commands.flo;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.emolga.bot.EmolgaMain;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class GoinCommand extends Command {
    public GoinCommand() {
        super("goin", "`!goin <VID> <Link>` Spielt in VID den Link ab", CommandCategory.Flo, "447357526997073930");
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String[] split = msg.split(" ");
        VoiceChannel vc = EmolgaMain.jda.getVoiceChannelById(split[1]);
        loadAndPlay(tco, split[2], vc);
    }
}
