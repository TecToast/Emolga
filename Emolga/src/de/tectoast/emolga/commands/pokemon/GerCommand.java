package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class GerCommand extends Command {
    public GerCommand() {
        super("ger", "`!ger <Name>` Zeigt den deutschen Namen dieser Sache.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String s = msg.substring(5);
        System.out.println(s);
        Translation t = getGerName(s);
        //System.out.println(t);
        if (t.isSuccess()) {
            tco.sendMessage(t.getTranslation()).queue();
            return;
        }
        tco.sendMessage("Es wurde keine Übersetzung für " + s + " gefunden!").queue();
    }
}
