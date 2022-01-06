package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class UpdatedatafromfileCommand extends Command {
    public UpdatedatafromfileCommand() {
        super("updatedatafromfile", "Aktualisiert die Daten auf Basis der Datei", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("name", "Draftname", "Der Name des Drafts", ArgumentManagerTemplate.draft())
                .setExample("!updatedatafromfile Emolga-Conference")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentDisplay();
        String name = e.getArguments().getText("name");
        Optional<Draft> op = Draft.drafts.stream().filter(d -> d.name.equals(name)).findFirst();
        if (op.isEmpty()) {
            tco.sendMessage("Dieser Draft existiert nicht!").queue();
            return;
        }
        Draft d = op.get();
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(name);
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
                d.picks.put(mem.getIdLong(), monlist);
                d.update(mem);
            } else {
                d.picks.put(mem.getIdLong(), new ArrayList<>());
            }
            if (d.isPointBased) {
                d.points.put(mem.getIdLong(), d.getTierlist().points);
                for (DraftPokemon mon : d.picks.get(mem.getIdLong())) {
                    d.points.put(mem.getIdLong(), d.points.get(mem.getIdLong()) - d.getTierlist().prices.get(mon.tier));
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
        e.done();
    }
}
