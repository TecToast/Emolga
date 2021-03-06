package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class AllLearnCommand extends Command {
    public AllLearnCommand() {
        super("alllearn", "`!alllearn <Attacke>:<pokemon 1> <pokemon 2> ...` Zeigt, welche der angegeben pokemon die angegebene Attacke lernen können.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        try {
            String st = msg.substring(10);
            String atk = Command.getGerName(st.split(":")[0]).split(";")[1];
            StringBuilder str = new StringBuilder();
            if (st.contains("\n")) {
                for (String s : st.split(":")[1].split("\n")) {
                    str.append(Command.canLearn(s.startsWith("A-") || s.startsWith("G-") || s.startsWith("M-") ? s.substring(2) : s, s.startsWith("A-") ? "" : (s.startsWith("G-") ? "Galar" : "Normal"), atk, msg, e.getGuild().getId().equals("747357029714231299") ? 5 : 8, getModByGuild(e)) ? s + "\n" : "");
                }
            } else {
                for (String s : st.split(":")[1].split(" ")) {
                    str.append(Command.canLearn(s.startsWith("A-") || s.startsWith("G-") || s.startsWith("M-") ? s.substring(2) : s, s.startsWith("A-") ? "" : (s.startsWith("G-") ? "Galar" : "Normal"), atk, msg, e.getGuild().getId().equals("747357029714231299") ? 5 : 8, getModByGuild(e)) ? s + "\n" : "");
                }
            }
            if (str.toString().equals("")) str.append("Kein pokemon kann diese Attacke erlernen!");
            tco.sendMessage(str.toString()).queue();
        } catch (Exception ex) {
            ex.printStackTrace();
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
        }
    }
}
