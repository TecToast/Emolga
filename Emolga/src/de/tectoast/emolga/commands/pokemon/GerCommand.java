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
        String str = getGerName(s);
        System.out.println(str);
        if (!str.equals("")) {
            tco.sendMessage(str.split(";")[1]).queue();
            return;
        }
        tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
    }
}
