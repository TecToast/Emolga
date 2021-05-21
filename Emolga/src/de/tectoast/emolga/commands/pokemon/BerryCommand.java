package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class BerryCommand extends Command {
    public BerryCommand() {
        super("berry", "Zeigt den Namen der Antibeere für diesen Typen an.", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("type", "Typ", "Der Typ der Antibeere", Translation.Type.TYPE)
                .setExample("!berry Rock")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String ger = e.getArguments().getTranslation("type").getTranslation();
        String g;
        String engl;
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
            default:
                g = "Es ist ein unbekannter Fehler aufgetreten! Bitte kontaktiere Flo mit `!flohelp <Nachricht>` !";
                engl = "";
                break;
        }
        e.reply("Deutsch: " + g + "\nEnglisch: " + engl);
    }
}
