package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ResistanceCommand extends Command {
    public ResistanceCommand() {
        super("resistance", "Zeigt alle Typen an, die den angegebenen Typen resistieren", CommandCategory.Pokemon);
        this.setArgumentTemplate(ArgumentManagerTemplate.builder()
                .addEngl("type", "Typ", "Typ, bei dem geschaut werden soll, was ihn resistiert", Translation.Type.TYPE)
                .setExample("!resistance Feuer")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        /*if (e.getArgsLength() == 0) {
            e.reply("Du musst einen Typen angeben!");
            return;
        }
        Translation type = getEnglNameWithType(e.getArg(0));
        if (!type.isFromType(Translation.Type.TYPE)) {
            e.reply("Das ist kein Typ!");
            return;
        }*/

        Translation type = e.getArguments().getTranslation("type");

        JSONObject json = getTypeJSON();
        ArrayList<String> l = json.keySet().stream().filter(s -> json.getJSONObject(s).getJSONObject("damageTaken").getInt(type.getTranslation()) == 2).map(Command::getGerNameNoCheck)
                .map(str -> {
                    if (str.equals("Psychokinese")) return "Psycho";
                    return str;
                }).collect(Collectors.toCollection(ArrayList::new));
        if (l.size() == 0) {
            e.reply("Es wurden keine Typen gefunden, die diesen Typ resistieren! (Das ist für normal ein Fehler, Flo wurde kontaktiert)");
            Command.sendToMe("ResistanceCommand ListSize 0");
            return;
        }
        e.reply("Folgende Typen resistieren " + type.getOtherLang() + ":\n" + String.join("\n", l));
    }
}
