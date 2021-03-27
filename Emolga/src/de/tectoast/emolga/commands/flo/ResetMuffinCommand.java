package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

public class ResetMuffinCommand extends Command {
    public ResetMuffinCommand() {
        super("resetmuffin", "`!resetmuffin` Setzt die Revolution zurück", CommandCategory.Flo);
    }

    @Override
    public void process(GuildCommandEvent e) {
        Guild g = e.getGuild();
        e.getChannel().sendMessage("Die **Muffin-Revolution** ist zu Ende D:").queue();
        JSONObject o = getEmolgaJSON().getJSONObject("muffinreset").getJSONObject(g.getId());
        for (String s : o.keySet()) {
            g.retrieveMemberById(s).submit().thenCompose(mem -> mem.modifyNickname(o.getString(s)).submit());
        }
        g.getSelfMember().modifyNickname("Emolga").queue();
    }
}
