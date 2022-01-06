package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import org.jsolf.JSONObject;

import java.util.HashMap;

public class ResistanceCommand extends Command {
    public ResistanceCommand() {
        super("resistance", "Zeigt alle Typen an, die der angegebene Typ resistiert", CommandCategory.Pokemon);
        this.setArgumentTemplate(ArgumentManagerTemplate.builder()
                .addEngl("type", "Typ", "Typ, bei dem geschaut werden soll, was er resistiert", Translation.Type.of(Translation.Type.TYPE, Translation.Type.POKEMON))
                .setExample("!resistance Feuer")
                .build());
        disable();
    }

    @Override
    public void process(GuildCommandEvent e) {
        Translation type = e.getArguments().getTranslation("type");
        if(type.isFromType(Translation.Type.TYPE)) {
            JSONObject json = getTypeJSON();
            HashMap<String, Integer> map = new HashMap<>();
            StringBuilder b = new StringBuilder();
            JSONObject typejson = json.getJSONObject(type.getTranslation()).getJSONObject("damageTaken");
            typejson.keySet().forEach(str -> {
                int damageTaken = typejson.getInt(str);
                if (damageTaken > 1) {
                    Translation t = (Translation) Translation.Type.TYPE.validate(str, Translation.Language.GERMAN, "default");
                    if (t != null) {
                        if (damageTaken == 3) b.append("**");
                        b.append(t.getTranslation());
                        if (damageTaken == 3) b.append("**");
                        b.append("\n");
                    }
                }
            });
            if (b.length() == 0) {
                e.reply("Es wurden keine Typen gefunden, die diesen Typ resistieren! (Das ist für normal ein Fehler, Flo wurde kontaktiert)");
                Command.sendToMe("ResistanceCommand ListSize 0");
                return;
            }
            e.reply("Folgende Typen resistiert " + type.getOtherLang() + ":\n" + b);
        } else {
            e.reply("Macht Flo noch irgendwann :)");
        }
    }
}
