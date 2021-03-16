package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.CommandEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

public class DeldraftCommand extends Command {
    public DeldraftCommand() {
        super("deldraft", "`!deldraft <Name>` Löscht den draft mit dem angegebenen Namen", CommandCategory.Flo);
    }

    @Override
    public void process(CommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        Member member = e.getMember();
        String name = msg.substring(10);
        JSONObject json = getEmolgaJSON();
        if (json.has("drafts")) {
            JSONObject drafts = json.getJSONObject("drafts");
            if (drafts.has(name)) {
                drafts.remove(name);
                saveEmolgaJSON();
                tco.sendMessage("Dieser draft wurde gelöscht!").queue();
            } else {
                tco.sendMessage("Dieser draft existiert nicht!").queue();
            }
        } else {
            tco.sendMessage("Es wurde noch kein draft erstellt!").queue();
        }
    }
}
