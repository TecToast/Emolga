package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONObject;

public class DexNumberCommand extends Command {

    public DexNumberCommand() {
        super("dexnumber", "Zeigt das Pokemon, dass zur Dex-Nummer gehört", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("num", "Dex-Nummer", "Die Dex-Nummer", ArgumentManagerTemplate.Number.range(1, 898))
                .setExample("!dexnumber 730")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject data = getDataJSON();
        int num = e.getArguments().getInt("num");
        for (String s : data.keySet()) {
            JSONObject o = data.getJSONObject(s);
            if(o.getInt("num") == num) {
                e.reply(getGerNameNoCheck(o.getString("name")));
                return;
            }
        }
    }
}
