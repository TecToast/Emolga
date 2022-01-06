package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.sql.DBManagers;
import org.jsolf.JSONObject;

import java.util.Arrays;
import java.util.List;

public class SinnohLoginCommand extends Command {

    public SinnohLoginCommand() {
        super("sinnohlogin", "Meldet dich beim Sinnoh Plateau an", CommandCategory.Draft, Constants.FLPID);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("sd", "Showdown-Name", "Dein Showdown-Name", ArgumentManagerTemplate.Text.any())
                .setExample("!sinnohlogin TecToast")
                .build());
        //addCustomChannel(Constants.FLPID);
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("SinnohPlateau");
        long uid = e.getMember().getIdLong();
        List<Long> table = league.getLongList("table");
        if(table.contains(uid)) {
            e.reply("Du bist bereits angemeldet!");
            return;
        }
        String sd = e.getArguments().getText("sd");
        DBManagers.SD_NAMES.addIfAbsend(sd, uid);
        int index = table.size() + 300;
        league.getJSONArray("table").put(uid);
        RequestBuilder.updateRow("18akCBATIaQ0DcMihj_DLX9QmxbZPP6Gpd6NlYqrMHUY", "Tabelle!O%d".formatted(index),
                Arrays.asList(e.getMember().getEffectiveName(), "", "=S%d + U%d".formatted(index, index), "", "0", "", "0", "", "=S%d * 3".formatted(index)));
        e.getMessage().addReaction("✅").queue();
        saveEmolgaJSON();
    }
}
