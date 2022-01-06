package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

public class ResetRevolutionCommand extends Command {
    public ResetRevolutionCommand() {
        super("resetrevolution", "Setzt die Diktatur zurück", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        setCustomPermissions(PermissionPreset.CULT);
    }

    @Override
    public void process(GuildCommandEvent e) {
        Guild g = e.getGuild();
        e.getChannel().sendMessage("Die **Diktatur** ist zu Ende D:").queue();
        JSONObject o = getEmolgaJSON().getJSONObject("revolutionreset").getJSONObject(g.getId());
        for (String s : o.keySet()) {
            g.retrieveMemberById(s).submit().thenCompose(mem -> mem.modifyNickname(o.getString(s)).submit());
        }
        for (GuildChannel gc : g.getChannels()) {
            gc.getManager().setName(gc.getName().replaceFirst("(.*)-", "")).queue();
        }
        g.getSelfMember().modifyNickname("Emolga").queue();
        JSONArray arr = getEmolgaJSON().getJSONArray("activerevolutions");
        arr.remove(arr.toList().indexOf(g.getIdLong()));
        saveEmolgaJSON();
    }
}
