package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

public class RandomPickCommand extends Command {
    public RandomPickCommand() {
        super("randompick", "`!randompick` Well...", CommandCategory.Draft);
    }

    @Override
    public void process(GuildCommandEvent e) {
        Member member = e.getMember();
        TextChannel tco = e.getChannel();
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            e.getChannel().sendMessage("Du Kek der Command funktioniert nur in einem Draft xD").queue();
            return;
        }
        String msg = e.getMessage().getContentDisplay();
        Tierlist tierlist = Tierlist.getByGuild(d.guild);
        if (e.getArgsLength() == 0) {
            tco.sendMessage("Du musst ein Tier auswählen!").queue();
            return;
        }
        String tier = e.getArg(0).toUpperCase();
        if (!tierlist.order.contains(tier)) {
            tco.sendMessage("Das ist kein Tier!").queue();
            return;
        }
        if (!d.tc.getId().equals(tco.getId())) return;
        if (d.isNotCurrent(member)) {
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

        ArrayList<String> list = new ArrayList<>(tierlist.tierlist.get(tier));
        Collections.shuffle(list);
        Predicate<String> typecheck;
        if (e.hasArg(1)) {
            Translation type = getEnglNameWithType(e.getArg(1));
            if (!type.isFromType(Translation.Type.TYPE)) {
                tco.sendMessage("Das ist kein Typ!").queue();
                return;
            }
            typecheck = str -> {
                String sd;
                if (str.startsWith("M-")) sd = getSDName(str.substring(2)) + "mega";
                else if (str.startsWith("A-")) sd = getSDName(str.substring(2)) + "alola";
                else if (str.startsWith("G-")) sd = getSDName(str.substring(2)) + "galar";
                else sd = getSDName(str.split("-")[0]);
                return getDataJSON().getJSONObject(sd).getJSONArray("types").toList().contains(type.getTranslation());
            };
        } else {
            typecheck = str -> true;
        }
        PickCommand.exec(tco, "!pick " + list.stream().filter(str -> !d.isPicked(str) && !d.hasInAnotherForm(mem, str) && (!d.hasMega(mem) || !str.startsWith("M-")) && typecheck.test(str)).map(String::trim).findFirst().orElse(""), mem, true);
    }
}
