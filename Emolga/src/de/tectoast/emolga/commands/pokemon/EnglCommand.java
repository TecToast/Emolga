package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class EnglCommand extends Command {
    public EnglCommand() {
        super("engl", "`!engl <Name>` Zeigt den englischen Namen dieser Sache.", CommandCategory.Pokemon);
    }

    @Override
    public void process(CommandEvent e) {

        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String s = msg.substring(6);
        String str = getEnglName(s);
        if (!str.equals("")) {
            tco.sendMessage(str).queue();
            return;
        }
        tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
    }
}
