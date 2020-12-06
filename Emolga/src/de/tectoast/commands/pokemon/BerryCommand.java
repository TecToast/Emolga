package de.tectoast.commands.pokemon;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class BerryCommand extends Command {
    public BerryCommand() {
        super("berry", "`!berry <Typ>` Zeigt den Namen der Antibeere für diesen Typen an.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String str = getGerName(msg.substring(7));
        if (str.equals("") || !str.startsWith("type")) {
            tco.sendMessage("Das ist kein Typ!").queue();
            return;
        }
        String ger = str.split(";")[1];
        String g = "Fehler";
        String err = "Fehler";
        switch (ger) {
            case "Feuer":
                g = "Koakobeere";
                err = "Occa Berry";
                break;
            case "Wasser":
                g = "Foepasbeere";
                err = "Passho Berry";
                break;
            case "Elektro":
                g = "Kerzalbeere";
                err = "Wacan Berry";
                break;
            case "Pflanze":
                g = "Grindobeere";
                err = "Rindo Berry";
                break;
            case "Eis":
                g = "Kiroyabeere";
                err = "Yache Berry";
                break;
            case "Kampf":
                g = "Rospelbeere";
                err = "Chople Berry";
                break;
            case "Gift":
                g = "Grarzbeere";
                err = "Kebia Berry";
                break;
            case "Boden":
                g = "Schukebeere";
                err = "Shuca Berry";
                break;
            case "Flug":
                g = "Kobabeere";
                err = "Coba Berry";
                break;
            case "Psycho":
                g = "Pyapabeere";
                err = "Payapa Berry";
                break;
            case "Käfer":
                g = "Tanigabeere";
                err = "Tanga Berry";
                break;
            case "Gestein":
                g = "Chiaribeere";
                err = "Charti Berry";
                break;
            case "Geist":
                g = "Zitarzbeere";
                err = "Kasib Berry";
                break;
            case "Drache":
                g = "Terirobeere";
                err = "Haban Berry";
                break;
            case "Unlicht":
                g = "Burleobeere";
                err = "Colbur Berry";
                break;
            case "Stahl":
                g = "Babiribeere";
                err = "Babiri Berry";
                break;
            case "Normal":
                g = "Latchibeere";
                err = "Chilan Berry";
                break;
        }
        tco.sendMessage("Deutsch: " + g + "\nEnglisch: " + err).queue();
    }
}
