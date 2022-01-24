package de.tectoast.emolga.commands.moderator;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EmolgaChannelCommand extends Command {

    public EmolgaChannelCommand() {
        super("emolgachannel", "Added/Removed einen Channel, in dem Emolga benutzt werden kann", CommandCategory.Moderator);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("action", "Aktion", "Die Aktion, die du durchführen möchtest",
                        ArgumentManagerTemplate.Text.of(SubCommand.of("add", "Fügt einen Channel hinzu"), SubCommand.of("remove", "Removed einen Channel")))
                .add("channel", "Channel", "Der Channel, der geaddet/removed werden soll", ArgumentManagerTemplate.DiscordType.CHANNEL)
                .setExample("!emolgachannel add #botchannel")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        String action = args.getText("action");
        TextChannel tc = args.getChannel("channel");
        JSONObject ec = getEmolgaJSON().getJSONObject("emolgachannel");
        String gid = e.getGuild().getId();
        long tid = tc.getIdLong();
        if (!ec.has(gid)) ec.put(gid, new JSONArray());
        JSONArray arr = ec.getJSONArray(gid);
        List<Long> l = arr.toLongList();
        long gidl = e.getGuild().getIdLong();
        if (action.equals("add")) {
            if (l.contains(tid)) {
                e.reply(tc.getAsMention() + " wurde bereits als Channel eingestellt!");
                return;
            }
            arr.put(tid);
            if (!emolgaChannel.containsKey(gidl)) emolgaChannel.put(gidl, new ArrayList<>());
            emolgaChannel.get(gidl).add(tid);
            saveEmolgaJSON();
            e.reply("Der Channel " + tc.getAsMention() + " wurde erfolgreich zu den erlaubten Channeln hinzugefügt!");
        } else {
            if (arr.length() == 0) {
                e.reply("Auf diesem Server wurden noch keine Channel für mich eingestellt!");
                return;
            }
            if (removeFromJSONArray(arr, tid)) {
                e.reply(tc.getAsMention() + " wurde erfolgreich aus den erlaubten Channeln gelöscht!");
                emolgaChannel.get(gidl).remove(tid);
                saveEmolgaJSON();
                return;
            }
            e.reply(tc.getAsMention() + " ist nicht in der Liste der erlaubten Channel!");
        }
    }
}
