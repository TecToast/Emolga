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
            case "Feuer" -> {
                g = "Koakobeere";
                engl = "Occa Berry";
            }
            case "Wasser" -> {
                g = "Foepasbeere";
                engl = "Passho Berry";
            }
            case "Elektro" -> {
                g = "Kerzalbeere";
                engl = "Wacan Berry";
            }
            case "Pflanze" -> {
                g = "Grindobeere";
                engl = "Rindo Berry";
            }
            case "Eis" -> {
                g = "Kiroyabeere";
                engl = "Yache Berry";
            }
            case "Kampf" -> {
                g = "Rospelbeere";
                engl = "Chople Berry";
            }
            case "Gift" -> {
                g = "Grarzbeere";
                engl = "Kebia Berry";
            }
            case "Boden" -> {
                g = "Schukebeere";
                engl = "Shuca Berry";
            }
            case "Flug" -> {
                g = "Kobabeere";
                engl = "Coba Berry";
            }
            case "Psycho" -> {
                g = "Pyapabeere";
                engl = "Payapa Berry";
            }
            case "Käfer" -> {
                g = "Tanigabeere";
                engl = "Tanga Berry";
            }
            case "Gestein" -> {
                g = "Chiaribeere";
                engl = "Charti Berry";
            }
            case "Geist" -> {
                g = "Zitarzbeere";
                engl = "Kasib Berry";
            }
            case "Drache" -> {
                g = "Terirobeere";
                engl = "Haban Berry";
            }
            case "Unlicht" -> {
                g = "Burleobeere";
                engl = "Colbur Berry";
            }
            case "Stahl" -> {
                g = "Babiribeere";
                engl = "Babiri Berry";
            }
            case "Normal" -> {
                g = "Latchibeere";
                engl = "Chilan Berry";
            }
            case "Fee" -> {
                g = "Hibisbeere";
                engl = "Roseli Berry";
            }
            default -> {
                g = "Es ist ein unbekannter Fehler aufgetreten! Bitte kontaktiere Flo mit `!flohelp <Nachricht>` !";
                engl = "";
            }
        }
        e.reply("Deutsch: " + g + "\nEnglisch: " + engl);
    }
}
