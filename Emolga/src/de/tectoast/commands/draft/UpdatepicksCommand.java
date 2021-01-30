package de.tectoast.commands.draft;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.Constants;
import de.tectoast.utils.draft.DraftPokemon;
import de.tectoast.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.stream.Collectors;

public class UpdatepicksCommand extends Command {
    public UpdatepicksCommand() {
        super("updatepicks", "`!updatepicks <Text-Channel> <Name>", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        Message m = e.getMessage();
        String msg = m.getContentRaw();
        String[] split = msg.split(" ");
        String name = split[2];
        TextChannel tc = m.getMentionedChannels().get(0);
        Guild g = tc.getGuild();
        JSONObject json = getEmolgaJSON();
        if (!json.has("drafts")) {
            tco.sendMessage("Es wurde noch kein draft erstellt!").queue();
            return;
        }
        JSONObject drafts = json.getJSONObject("drafts");
        if (e.getGuild().getId().equals(Constants.ASLID)) drafts = drafts.getJSONObject("ASLS7");
        if (!drafts.has(name)) {
            tco.sendMessage("Es gibt keine Liga mit dem Namen " + name + "!").queue();
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
            for (String o : Tierlist.getByGuild("747357029714231299").order) {
                ArrayList<DraftPokemon> mons = list.stream().filter(s -> s.tier.equals(o)).sorted(Comparator.comparing(pkmn -> pkmn.name)).collect(Collectors.toCollection(ArrayList::new));
                for (DraftPokemon mon : mons) {
                    mes.append(o).append(": ").append(mon.name).append("\n");
                }
            }
            li.get(i).editMessage(mes.toString()).queue();
            //tco.sendMessage(mes.toString()).queue();
            i++;
        }
        tco.sendMessage("Success!").queue();
    }

}

