package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.RequestBuilder;
import de.tectoast.emolga.utils.draft.Draft;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
        d.cooldown.cancel(false);
        int round = d.round;
        //fplDoc(league, d);

        if (d.order.get(d.round).size() == 0) {
            if (d.round == d.getTierlist().rounds) {
                tco.sendMessage("Der Draft ist vorbei!").queue();
                d.ended = true;
                //wooloodoc(tierlist, pokemon, d, mem, needed, null, num, round);
                if (d.afterDraft.size() > 0)
                    tco.sendMessage("Reihenfolge zum Nachdraften:\n" + d.afterDraft.stream().map(d::getMention).collect(Collectors.joining("\n"))).queue();
                saveEmolgaJSON();
                Draft.drafts.remove(d);
                return;
            }
            d.round++;
            d.tc.sendMessage("Runde " + d.round + "!").queue();
            league.put("round", d.round);
        }
        logger.info("d.order = " + d.order);
        logger.info("d.round = " + d.round);
        d.current = d.order.get(d.round).remove(0);
        league.put("current", d.current);
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts");
        tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points.get(d.current) + " mögliche Punkte)").queue();
        d.cooldown.cancel(false);
        long delay = calculateDraftTimer();
        league.put("cooldown", System.currentTimeMillis() + delay);
        d.cooldown = d.scheduler.schedule((Runnable) d::timer, delay, TimeUnit.MILLISECONDS);
        saveEmolgaJSON();
    }

    private void fplDoc(JSONObject league, Draft d) {
        RequestBuilder b = new RequestBuilder(league.getString("sid"));
        b.addStrikethroughChange(league.getInt("draftorder"), d.round + 1, (d.members.size() - d.order.get(d.round).size()) + 6, true);
        b.execute();
    }
}
