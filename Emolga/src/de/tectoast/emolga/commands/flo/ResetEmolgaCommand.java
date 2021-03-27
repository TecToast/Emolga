package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import org.json.JSONObject;

public class ResetEmolgaCommand extends Command {
    public ResetEmolgaCommand() {
        super("resetemolga", "`!resetemolga` Setzt die Diktatur zurück", CommandCategory.Flo);
    }

    @Override
    public void process(GuildCommandEvent e) {
        Guild g = e.getGuild();
        e.getChannel().sendMessage("Die **Emolga-Diktatur** ist zu Ende D:").queue();
        JSONObject o = getEmolgaJSON().getJSONObject("emolgareset").getJSONObject(g.getId());
        for (String s : o.keySet()) {
            g.retrieveMemberById(s).submit().thenCompose(mem -> mem.modifyNickname(o.getString(s)).submit());
        }
        for (GuildChannel gc : g.getChannels()) {
            gc.getManager().setName(gc.getName().replaceFirst("emolga-", "")).queue();
        }
        g.getSelfMember().modifyNickname("Emolga").queue();
    }
}
