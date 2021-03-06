package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class UpdatedatafromfileCommand extends Command {
    public UpdatedatafromfileCommand() {
        super("updatedatafromfile", "`!updatedatafromfile <Name>` Aktualisiert die Daten auf Basis der Datei", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        String name = msg.substring(20);
        Optional<Draft> op = Draft.drafts.stream().filter(d -> d.name.equals(name)).findFirst();
        if (!op.isPresent()) {
            tco.sendMessage("Dieser draft existiert nicht!").queue();
            return;
        }
        Draft d = op.get();
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject("ASLS7").getJSONObject(name);
        int lround = league.getInt("round");
        if (d.round != lround) {
            d.tc.sendMessage("Runde " + lround + "!").queue();
        }
        d.round = lround;
        d.current = d.tc.getGuild().retrieveMemberById(league.getString("current")).complete();
        int x = 0;
        for (Member mem : d.order.get(d.round)) {
            x++;
            if (d.current.getId().equals(mem.getId())) break;
        }
        if (x > 0) {
            d.order.get(d.round).subList(0, x).clear();
        }
        JSONObject pick = league.getJSONObject("picks");
        for (Member mem : d.members) {
            if (pick.has(mem.getId())) {
                JSONArray arr = pick.getJSONArray(mem.getId());
                ArrayList<DraftPokemon> monlist = new ArrayList<>();
                for (Object ob : arr) {
                    JSONObject obj = (JSONObject) ob;
                    monlist.add(new DraftPokemon(obj.getString("name"), obj.getString("tier")));
                }
                d.picks.put(mem, monlist);
                d.update(mem);
            } else {
                d.picks.put(mem, new ArrayList<>());
            }
            if (d.isPointBased) {
                d.points.put(mem, 1000);
                for (DraftPokemon mon : d.picks.get(mem)) {
                    d.points.put(mem, d.points.get(mem) - d.getTierlist().prices.get(mon.tier));
                }
            }
        }
        try {
            d.cooldown.cancel();
        } catch (Exception ignored) {
        }
        d.cooldown = new Timer();
        d.cooldown.schedule(new TimerTask() {
            @Override
            public void run() {
                d.timer();
            }
        }, calculateASLTimer());
        tco.sendMessage("Done!").queue();
    }
}
