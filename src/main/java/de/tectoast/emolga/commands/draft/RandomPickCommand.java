package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Predicate;

public class RandomPickCommand extends Command {
    public RandomPickCommand() {
        super("randompick", "Well... nen Random-Pick halt", CommandCategory.Draft);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("tier", "Tier", "Das Tier, in dem gepickt werden soll", ArgumentManagerTemplate.Text.any())
                .addEngl("type", "Typ", "Der Typ, von dem random gepickt werden soll", Translation.Type.TYPE, true)
                .setExample("!randompick A")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Member memberr = e.getMember();
        long member = memberr.getIdLong();
        TextChannel tco = e.getChannel();
        Draft d = Draft.getDraftByMember(member, tco);
        if (d == null) {
            e.getChannel().sendMessage("Du Kek der Command funktioniert nur in einem Draft xD").queue();
            return;
        }
        String msg = e.getMessage().getContentDisplay();
        Tierlist tierlist = d.getTierlist();
        ArgumentManager args = e.getArguments();
        String tier = tierlist.order.stream().filter(s -> args.getText("tier").equalsIgnoreCase(s)).findFirst().orElse("");
        if (tier.equals("")) {
            tco.sendMessage("Das ist kein Tier!").queue();
            return;
        }
        if (!d.tc.getId().equals(tco.getId())) return;
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        long mem = d.current;
        JSONObject json = getEmolgaJSON();
        //JSONObject league = json.getJSONObject("drafts").getJSONObject(d.name);
        JSONObject league = getEmolgaJSON().getJSONObject("drafts").getJSONObject(d.name);
            /*if (asl.has("allowed")) {
                JSONObject allowed = asl.getJSONObject("allowed");
                if (allowed.has(member.getId())) {
                    mem = d.tc.getGuild().retrieveMemberById(allowed.getString(member.getId())).complete();
                } else mem = member;
            } else mem = member;*/
        ArrayList<String> list = new ArrayList<>(tierlist.tierlist.get(tier));
        Collections.shuffle(list);
        Predicate<String> typecheck;
        if (args.has("type")) {
            Translation type = args.getTranslation("type");
            typecheck = str -> getDataJSON().getJSONObject(getSDName(str)).getJSONArray("types").toList().contains(type.getTranslation());
        } else {
            typecheck = str -> true;
        }
        PickCommand.exec(tco, "!pick " + list.stream().filter(str -> !d.isPicked(str) && !d.hasInAnotherForm(mem, str) && (!d.hasMega(mem) || !str.startsWith("M-")) && typecheck.test(str)).map(String::trim).findFirst().orElse(""), memberr, true);
    }
}
