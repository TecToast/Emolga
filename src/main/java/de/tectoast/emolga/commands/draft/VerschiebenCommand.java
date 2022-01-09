package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static de.tectoast.emolga.utils.draft.Draft.getIndex;

public class VerschiebenCommand extends Command {

    public VerschiebenCommand() {
        super("verschieben", "Verschiebt deinen Pick", CommandCategory.Draft, Constants.ASLID, Constants.CULTID, 821350264152784896L);
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
        if(d.round == d.getTierlist().rounds) {
            e.reply("Der Draft befindet sich bereits in Runde " + d.round + ", somit kann der Pick nicht mehr verschoben werden!");
            return;
        }
        try {
            d.cooldown.cancel();
        } catch (Exception ignored) {

        }
        JSONObject asl = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS9");
        JSONObject league = asl.getJSONObject(d.name);
        if (d.order.get(d.round).size() == 0) {
            d.round++;
            d.tc.sendMessage("Runde " + d.round + "!").queue();
            league.put("round", d.round);
        }
        d.current = d.order.get(d.round).remove(0);
        league.put("current", d.current);
        DraftPokemon toremove = null;
        if (d.isPointBased && d.points.get(d.current) < 20) {
            List<DraftPokemon> picks = d.picks.get(d.current);
            int price = 0;
            for (DraftPokemon pick : picks) {
                int pr = d.getTierlist().getPointsNeeded(pick.name);
                if (pr > price) {
                    price = pr;
                    toremove = pick;
                }
            }
            tco.sendMessage(d.getMention(d.current) + " Du hast nicht mehr genug Punkte um ein weiteres Pokemon zu draften! Deshalb verlierst du " + toremove.name + " und erhältst dafür " + price / 2 + " Punkte!").queue();
            d.points.put(d.current, d.points.get(d.current) + price / 2);
            d.picks.get(d.current).remove(toremove);
            d.afterDraft.add(d.current);
        }
        league.getJSONObject("picks").put(d.current, d.getTeamAsArray(d.current));
        if (d.isPointBased)
            tco.sendMessage(d.getMention(d.current) + " (<@&" + asl.getLongList("roleids").get(getIndex(d.current)) + ">) ist dran! (" + d.points.get(d.current) + " mögliche Punkte)").queue();
        else
            tco.sendMessage(d.getMention(d.current) + " ist dran! (Mögliche Tiers: " + d.getPossibleTiersAsString(d.current) + ")").queue();
        d.cooldown = new Timer();
        long delay = calculateASLTimer();
        league.put("cooldown", System.currentTimeMillis() + delay);
        d.cooldown.schedule(new TimerTask() {
            @Override
            public void run() {
                d.timer();
            }
        }, delay);
        saveEmolgaJSON();
        //ndsdoc(tierlist, pokemon, d, mem, tier, round);
    }
}
