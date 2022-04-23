package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;

public class ResistedCommand extends Command {
    public ResistedCommand() {
        super("resisted", "Zeigt alle Typen an, die der angegebene Typ resistiert", CommandCategory.Pokemon);
        this.setArgumentTemplate(ArgumentManagerTemplate.builder()
                .addEngl("type", "Typ", "Typ, bei dem geschaut werden soll, was er resistiert", Translation.Type.TYPE)
                .setExample("!resistance Feuer")
                .build());
        disable();
    }

    @Override
    public void process(GuildCommandEvent e) {
        Translation type = e.getArguments().getTranslation("type");
        JSONObject json = getTypeJSON();
        StringBuilder b = new StringBuilder();
        json.keySet().forEach(str -> {
            int damageTaken = json.getJSONObject(str).getJSONObject("damageTaken").getInt(type.getTranslation());
            if (damageTaken > 1) {
                if(damageTaken == 3) b.append("**");
                b.append(((Translation) Translation.Type.TYPE.validate(str, Translation.Language.GERMAN, "default")).getTranslation());
                if(damageTaken == 3) b.append("**");
                b.append("\n");
            }
        });
        if (b.length() == 0) {
            e.reply("Es wurden keine Typen gefunden, die diesen Typ resistieren! (Das ist für normal ein Fehler, Flo wurde kontaktiert)");
            Command.sendToMe("ResistanceCommand ListSize 0");
            return;
        }
        e.reply("Folgende Typen resistieren " + type.getOtherLang() + ":\n" + b);
    }
}
