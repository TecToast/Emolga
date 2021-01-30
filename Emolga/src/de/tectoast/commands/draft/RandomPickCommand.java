package de.tectoast.commands.draft;

import de.tectoast.commands.Command;
import de.tectoast.commands.CommandCategory;
import de.tectoast.utils.draft.Draft;
import de.tectoast.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

public class RandomPickCommand extends Command {
    public RandomPickCommand() {
        super("randompick", "`!randompick` Well...", CommandCategory.Draft);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        Member member = e.getMember();
        TextChannel tco = e.getChannel();
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            tco.sendMessage(member.getAsMention() + " Du bist in keinem Draft drin!").queue();
            return;
        }
        if (!d.tc.getId().equals(tco.getId())) return;
        if (d.isCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        Member mem;
        JSONObject json = getEmolgaJSON();
        //JSONObject league = json.getJSONObject("drafts").getJSONObject(d.name);
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
            /*if (asl.has("allowed")) {
                JSONObject allowed = asl.getJSONObject("allowed");
                if (allowed.has(member.getId())) {
                    mem = d.tc.getGuild().retrieveMemberById(allowed.getString(member.getId())).complete();
                } else mem = member;
            } else mem = member;*/
        if (d.current.getId().equals(member.getId())) mem = member;
        else {
            mem = tco.getGuild().retrieveMemberById(league.getJSONObject("allowed").getString(member.getId())).complete();
        }
        Tierlist tierlist = Tierlist.getByGuild("747357029714231299");
        for (String tier : tierlist.order) {
            if (d.getPossibleTiers(mem).get(tier) == 0) {
                continue;
            }
            ArrayList<String> list = new ArrayList<>(tierlist.tierlist.get(tier));
            Collections.shuffle(list);
            PickCommand.exec(tco, "!pick " + list.stream().filter(str -> !d.isPicked(str) && !d.hasInAnotherForm(mem, str)).findFirst().orElse(""), mem, true);
            break;
        }
    }
}
