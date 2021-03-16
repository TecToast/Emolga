package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class GoinCommand extends Command {
    public GoinCommand() {
        super("goin", "`!goin <VID> <Link>` Spielt in VID den Link ab", CommandCategory.Flo);
    }

    @Override
    public void process(CommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String[] split = msg.split(" ");
        VoiceChannel vc = EmolgaMain.jda.getVoiceChannelById(split[1]);
        loadAndPlay(tco, split[2], vc);
    }
}
