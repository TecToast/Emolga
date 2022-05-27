package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.database.Database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class NatureCommand extends Command {

    final HashMap<String, String> statnames;

    public NatureCommand() {
        super("nature", "Zeigt an, welche Werte dieses Wesen beeinflusst", CommandCategory.Pokemon);
        statnames = new HashMap<>();
        statnames.put("atk", "Atk");
        statnames.put("def", "Def");
        statnames.put("spa", "SpAtk");
        statnames.put("spd", "SpDef");
        statnames.put("spe", "Init");
        setArgumentTemplate(ArgumentManagerTemplate.builder().addEngl("nature", "Wesen", "Das Wesen", Translation.Type.NATURE)
                .setExample("!nature Adamant")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) throws SQLException {
        Translation t = e.getArguments().getTranslation("nature");
        ResultSet set = Database.select("SELECT * FROM natures WHERE name = \"" + t.getTranslation() + "\"");
        set.next();
        StringBuilder b = new StringBuilder(t.getOtherLang() + "/" + t.getTranslation() + ":\n");
        String plus = set.getString("plus");
        String minus = set.getString("minus");
        set.close();
        if (plus != null) {
            b.append(statnames.get(plus)).append("+\n").append(statnames.get(minus)).append("-");
        } else {
            b.append("Neutral");
        }
        e.reply(b.toString());
    }
}
