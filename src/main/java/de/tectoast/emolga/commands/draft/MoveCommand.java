package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

public class MoveCommand extends Command {

    public MoveCommand() {
        super("move", "Verschiebt deinen Pick", CommandCategory.Draft, Constants.ASLID, Constants.CULTID, 821350264152784896L);
        aliases.add("verschieben");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Member memberr = e.getMember();
        long member = memberr.getIdLong();
        TextChannel tco = e.getChannel();
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            tco.sendMessage(memberr.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return;
        }
        if (!d.tc.getId().equals(tco.getId())) return;
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        long mem = d.current;
        Tierlist tierlist = d.getTierlist();
        /*if(d.round == tierlist.rounds) {
            e.reply("Der Draft befindet sich bereits in Runde " + d.round + ", somit kann der Pick nicht mehr verschoben werden!");
            return;
        }*/
        JSONObject league = d.getLeague();
        if (!league.has("skippedturns")) league.put("skippedturns", new JSONObject());
        JSONObject st = league.getJSONObject("skippedturns");
        st.put(mem, st.createOrGetArray(mem).put(d.round));
        d.nextPlayer(tco, tierlist, league);
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }
}
