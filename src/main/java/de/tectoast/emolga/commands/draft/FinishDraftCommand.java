package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.concurrent.TimeUnit;

public class FinishDraftCommand extends Command {

    public FinishDraftCommand() {
        super("finishdraft", "Beendet für dich den Draft", CommandCategory.Draft, Constants.NDSID, Constants.ASLID);
        aliases.add("finish");
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
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        long mem = d.current;
        int round = d.round;
        if (e.getGuild().getIdLong() == Constants.NDSID && d.picks.get(mem).size() < 15) {
            e.reply("Du hast noch keine 15 Pokemon!");
            return;
        }
        /*if (round < 12) {
            e.reply("Du hast noch nicht 11 Pokemon!");
            return;
        }*/
        e.reply("Du hast den Draft für dich beendet!");
        d.order.values().forEach(l -> l.removeIf(me -> me == mem));
        league.put("finished", league.optString("finished") + mem + ",");
        d.cooldown.cancel(false);
        if (d.order.get(d.round).size() == 0) {
            if (d.round == d.getTierlist().rounds) {
                tco.sendMessage("Der Draft ist vorbei!").queue();
                d.ended = true;
                //wooloodoc(tierlist, pokemon, d, mem, needed, null, num, round);
                saveEmolgaJSON();
                Draft.drafts.remove(d);
                return;
            }
            d.round++;
            if (d.order.get(d.round).size() == 0) {
                e.reply("Da alle bereits ihre Drafts beendet haben, ist der Draft vorbei!");
                saveEmolgaJSON();
                return;
            }
            d.tc.sendMessage("Runde " + d.round + "!").queue();
            league.put("round", d.round);
        }
        d.current = d.order.get(d.round).remove(0);
        league.put("current", d.current);
        d.cooldown.cancel(false);
        league.getJSONObject("picks").put(d.current, d.getTeamAsArray(d.current));
        if (d.isPointBased)
            //tco.sendMessage(getMention(current) + " (<@&" + asl.getLongList("roleids").get(getIndex(current.getIdLong())) + ">) ist dran! (" + points.get(current.getIdLong()) + " mögliche Punkte)").queue();
            tco.sendMessage(d.getMention(d.current) + " ist dran! (" + d.points.get(d.current) + " mögliche Punkte)").queue();
        else
            tco.sendMessage(d.getMention(d.current) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(d.current) + ")").queue();
        long delay = calculateDraftTimer();
        league.put("cooldown", System.currentTimeMillis() + delay);
        d.cooldown = d.scheduler.schedule((Runnable) d::timer, delay, TimeUnit.MILLISECONDS);
        saveEmolgaJSON();
    }
}
