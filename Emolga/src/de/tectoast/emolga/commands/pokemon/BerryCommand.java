package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

public class BerryCommand extends Command {
    public BerryCommand() {
        super("berry", "`!berry <Typ>` Zeigt den Namen der Antibeere für diesen Typen an.", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        Translation t = getGerName(msg.substring(7));
        if (!t.isFromType(Translation.Type.TYPE)) {
            tco.sendMessage("Das ist kein Typ!").queue();
            return;
        }
        String ger = t.getTranslation();
        String g = "Fehler";
        String engl = "Fehler";
        switch (ger) {
            case "Feuer":
                g = "Koakobeere";
                engl = "Occa Berry";
                break;
            case "Wasser":
                g = "Foepasbeere";
                engl = "Passho Berry";
                break;
            case "Elektro":
                g = "Kerzalbeere";
                engl = "Wacan Berry";
                break;
            case "Pflanze":
                g = "Grindobeere";
                engl = "Rindo Berry";
                break;
            case "Eis":
                g = "Kiroyabeere";
                engl = "Yache Berry";
                break;
            case "Kampf":
                g = "Rospelbeere";
                engl = "Chople Berry";
                break;
            case "Gift":
                g = "Grarzbeere";
                engl = "Kebia Berry";
                break;
            case "Boden":
                g = "Schukebeere";
                engl = "Shuca Berry";
                break;
            case "Flug":
                g = "Kobabeere";
                engl = "Coba Berry";
                break;
            case "Psycho":
                g = "Pyapabeere";
                engl = "Payapa Berry";
                break;
            case "Käfer":
                g = "Tanigabeere";
                engl = "Tanga Berry";
                break;
            case "Gestein":
                g = "Chiaribeere";
                engl = "Charti Berry";
                break;
            case "Geist":
                g = "Zitarzbeere";
                engl = "Kasib Berry";
                break;
            case "Drache":
                g = "Terirobeere";
                engl = "Haban Berry";
                break;
            case "Unlicht":
                g = "Burleobeere";
                engl = "Colbur Berry";
                break;
            case "Stahl":
                g = "Babiribeere";
                engl = "Babiri Berry";
                break;
            case "Normal":
                g = "Latchibeere";
                engl = "Chilan Berry";
                break;
            case "Fee":
                g = "Hibisbeere";
                engl = "Roseli Berry";
                break;
        }
        tco.sendMessage("Deutsch: " + g + "\nEnglisch: " + engl).queue();
    }
}
