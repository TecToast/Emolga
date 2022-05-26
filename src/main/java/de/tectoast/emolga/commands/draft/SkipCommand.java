package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkipCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(SkipCommand.class);

    public SkipCommand() {
        super("skip", "Überspringt deinen Zug", CommandCategory.Draft, Constants.ASLID, Constants.FPLID, Constants.NDSID);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String msg = e.getMsg();
        TextChannel tco = e.getChannel();
        Member memberr = e.getMember();
        long member = memberr.getIdLong();
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            //tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return;
        }
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
        if (!d.tc.getId().equals(tco.getId())) return;
        if (!d.isSwitchDraft) {
            e.reply("Dieser Draft ist kein Switch-Draft, daher wird !skip nicht unterstützt!");
            return;
        }
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        //fplDoc(league, d);
        d.nextPlayer(tco, d.getTierlist(), league);
    }

    private void fplDoc(JSONObject league, Draft d) {
        RequestBuilder b = new RequestBuilder(league.getString("sid"));
        b.addStrikethroughChange(league.getInt("draftorder"), d.round + 1, (d.members.size() - d.order.get(d.round).size()) + 6, true);
        b.execute();
    }
}
