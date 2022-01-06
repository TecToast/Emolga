package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.draft.DraftPokemon;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class UpdatepicksCommand extends Command {
    public UpdatepicksCommand() {
        super("updatepicks", "Updatet die Picks in einem Channel", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("channel", "Text-Channel", "Der Channel, wo die Picks drin stehen", ArgumentManagerTemplate.DiscordType.CHANNEL)
                .add("name", "Draft-Name", "Der Name des Drafts", ArgumentManagerTemplate.draft())
                .setExample("!updatepicks #emolga-teamübersicht Emolga-Conference")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        ArgumentManager args = e.getArguments();
        TextChannel tc = args.getChannel("channel");
        Guild g = tc.getGuild();
        JSONObject json = getEmolgaJSON();
        if (!json.has("drafts")) {
            e.reply("Es wurde noch kein Draft erstellt!");
            return;
        }
        JSONObject drafts = json.getJSONObject("drafts");
        if (e.getGuild().getIdLong() == Constants.ASLID) drafts = drafts.getJSONObject("ASLS7");
        if (!drafts.has(name)) {
            e.reply("Es gibt keine Liga mit dem Namen " + name + "!");
            return;
        }
        JSONObject league = drafts.getJSONObject(name);
        ArrayList<Message> li = new ArrayList<>();
        for (Message message : tc.getIterableHistory()) {
            li.add(message);
        }
        Collections.reverse(li);
        int i = 0;
        for (String p : league.getJSONObject("order").getString("1").split(",")) {
            ArrayList<DraftPokemon> list = league.getJSONObject("picks").getJSONArray(p).toList().stream().map(o -> {
                HashMap<String, Object> obj = (HashMap<String, Object>) o;
                return new DraftPokemon((String) obj.get("name"), (String) obj.get("tier"));
            }).collect(Collectors.toCollection(ArrayList::new));
            StringBuilder mes = new StringBuilder("**" + g.retrieveMemberById(p).complete().getEffectiveName() + ":**\n");
            for (String o : Tierlist.getByGuild(g.getId()).order) {
                ArrayList<DraftPokemon> mons = list.stream().filter(s -> s.tier.equals(o)).sorted(Comparator.comparing(pkmn -> pkmn.name)).collect(Collectors.toCollection(ArrayList::new));
                for (DraftPokemon mon : mons) {
                    mes.append(o).append(": ").append(mon.name).append("\n");
                }
            }
            li.get(i).editMessage(mes.toString()).queue();
            //tco.sendMessage(mes.toString()).queue();
            i++;
        }
        e.done();
    }

}

