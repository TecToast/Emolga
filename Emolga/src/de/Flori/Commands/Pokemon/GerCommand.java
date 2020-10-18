package de.Flori.Commands.Pokemon;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class GerCommand extends Command {
    public GerCommand() {
        super("ger", "`!ger <Name>` Zeigt den deutschen Namen dieser Sache.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
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
        Document d;
        try {
            try {
                d = Jsoup.connect("https://www.pokewiki.de/" + name).get();
            } catch (Exception ex) {
                d = Jsoup.connect("https://www.pokewiki.de/" + eachWordUpperCase(name)).get();
            }
        } catch (IOException ioException) {
            tco.sendMessage("Es ist ein Fehler aufgetreten!").queue();
            ioException.printStackTrace();
            return;
        }
        tco.sendMessage(d.select("span[lang=\"en\"]").text()).queue();
    }
}
