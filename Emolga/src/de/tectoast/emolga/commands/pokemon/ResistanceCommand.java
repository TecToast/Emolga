package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ResistanceCommand extends Command {
    public ResistanceCommand() {
        super("resistance", "`!resistance <Typ> Zeigt alle Typen an, die den angegebenen Typen resistieren", CommandCategory.Pokemon);
        this.wip = true;
    }

    @Override
    public void process(GuildCommandEvent e) {
        if(e.getArgsLength() == 0) {
            e.reply("Du musst einen Typen angeben!");
            return;
        }
        String type = getEnglNameWithType(e.getArg(0));
        if(!type.startsWith("type;")) {
            e.reply("Das ist kein Typ!");
            return;
        }
        type = type.split(";")[1];
        JSONObject json = getTypeJSON();
        ArrayList<String> list = new ArrayList<>();
        for (String s : json.keySet()) {
            if(json.getJSONObject(s).getJSONObject("damageTaken").getInt(type) == 2) {
                list.add(getGerNameNoCheck(s));
            }
        }
        String finalType = type;
        ArrayList<String> l = json.keySet().stream().filter(s -> json.getJSONObject(s).getJSONObject("damageTaken").getInt(finalType) == 2).map(Command::getGerNameNoCheck).collect(Collectors.toCollection(ArrayList::new));
    }
}
