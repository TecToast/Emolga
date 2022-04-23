package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class TypeInfoCommand extends Command {

    public TypeInfoCommand() {
        super("typeinfo", "Zeigt dir Informationen über einen Typen an", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .addEngl("type", "Typ", "Der Typ", Translation.Type.TYPE)
                .setExample("!typeinfo Wasser")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Translation tt = e.getArguments().getTranslation("type");
        String type = tt.getTranslation();
        JSONObject json = getTypeJSON();
        List<String> effectiveAgainst = new LinkedList<>();
        List<String> resistedBy = new LinkedList<>();
        List<String> weakAgainst = new LinkedList<>();
        List<String> resisted = new LinkedList<>();
        json.keySet().forEach(str -> {
            int damageTaken = json.getJSONObject(str).getJSONObject("damageTaken").getInt(type);
            if (damageTaken > 0) {
                String t = ((Translation) Translation.Type.TYPE.validate(str, Translation.Language.GERMAN, "default")).getTranslation();
                if (damageTaken > 1) {
                    if (damageTaken == 3) resistedBy.add(t + " **(immun)**");
                    else resistedBy.add(t);
                } else {
                    effectiveAgainst.add(t);
                }
            }
        });

        JSONObject typejson = json.getJSONObject(type).getJSONObject("damageTaken");
        typejson.keySet().forEach(str -> {
            int damageTaken = typejson.getInt(str);
            if (damageTaken > 0) {
                Translation t = (Translation) Translation.Type.TYPE.validate(str, Translation.Language.GERMAN, "default");
                if (t != null) {
                    if (damageTaken > 1) {
                        if (damageTaken == 3) resisted.add(t.getTranslation() + " **(immun)**");
                        else resisted.add(t.getTranslation());
                    } else {
                        weakAgainst.add(t.getTranslation());
                    }
                }
            }
        });
        e.reply("**" + tt.getOtherLang() + "**\n\n" +
                "- effektiv gegen\n" + String.join("\n", effectiveAgainst) + "\n\n" +
                "- wird resistiert von\n" + String.join("\n", resistedBy) + "\n\n" +
                "- ist schwach gegen\n" + String.join("\n", weakAgainst) + "\n\n" +
                "- resistiert\n" + String.join("\n", resisted));
    }
}
