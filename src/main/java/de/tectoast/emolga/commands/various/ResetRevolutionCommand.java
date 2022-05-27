package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;

import java.util.regex.Pattern;

public class ResetRevolutionCommand extends Command {
    private static final Pattern REVOLUTION_PATTERN = Pattern.compile("(.*)-");

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
            gc.getManager().setName(REVOLUTION_PATTERN.matcher(gc.getName()).replaceFirst("")).queue();
        }
        g.getSelfMember().modifyNickname("Emolga").queue();
        JSONArray arr = getEmolgaJSON().getJSONArray("activerevolutions");
        arr.remove(arr.toList().indexOf(g.getIdLong()));
        saveEmolgaJSON();
    }
}
