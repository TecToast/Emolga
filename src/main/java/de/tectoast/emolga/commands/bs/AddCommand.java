package de.tectoast.emolga.commands.bs;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.RequestBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

public class AddCommand extends Command {
    public AddCommand() {
        super("add", "`!add` Fügt dich ins Doc hinzu, damit du dort pokemon eintragen kannst", CommandCategory.BS);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        Member member = e.getMember();
        JSONObject json = getEmolgaJSON();
        if (!json.has("tradedoc")) json.put("tradedoc", new JSONObject());
        JSONObject obj = json.getJSONObject("tradedoc");
        String id = null;
        for (String s : obj.keySet()) {
            if (obj.getString(s).equals(member.getId())) id = s;
        }
        if (id != null) {
            tco.sendMessage("Du bist bereits unter der ID " + id + " registriert!").queue();
            return;
        }
        int x = -1;
        for (int i = 1; i <= 100; i++) {
            if (obj.has(String.valueOf(i))) continue;
            x = i;
            break;
        }
        if (x == -1) {
            tco.sendMessage("Es sind zu viele Benutzer registriert!").queue();
            return;
        }
        obj.put(String.valueOf(x), member.getId());
        tco.sendMessage("Du wurdest erfolgreich mit der ID " + x + " registriert!").queue();
        saveEmolgaJSON();
        if (x <= 8)
            RequestBuilder.updateSingle(tradesid, "VFs und Ballmons!" + (char) (x * 3 + 65) + "1", member.getEffectiveName() + " (" + x + ")", false);

    }
}
