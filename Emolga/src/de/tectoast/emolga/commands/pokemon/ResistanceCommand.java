package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class ResistanceCommand extends Command {
    public ResistanceCommand() {
        super("resistance", "`!resistance` <Typ> Zeigt alle Typen an, die den angegebenen Typen resistieren", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
        if (e.getArgsLength() == 0) {
            e.reply("Du musst einen Typen angeben!");
            return;
        }
        Translation type = getEnglNameWithType(e.getArg(0));
        if (!type.isFromType(Translation.Type.TYPE)) {
            e.reply("Das ist kein Typ!");
            return;
        }
        JSONObject json = getTypeJSON();
        ArrayList<String> l = json.keySet().stream().filter(s -> json.getJSONObject(s).getJSONObject("damageTaken").getInt(type.getTranslation()) == 2).map(Command::getGerNameNoCheck).collect(Collectors.toCollection(ArrayList::new));
        if (l.size() == 0) {
            e.reply("Es wurden keine Typen gefunden, die diesen Typ resistieren! (Das ist für normal ein Fehler, Flo wurde kontaktiert)");
            Command.sendToMe("ResistanceCommand ListSize 0");
            return;
        }
        e.reply("Folgende Typen resistieren " + type.getOtherLang() + ":\n" + String.join("\n", l));
    }
}
