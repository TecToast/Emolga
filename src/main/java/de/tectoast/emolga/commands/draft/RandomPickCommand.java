package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Draft;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        Tierlist tierlist = d.getTierlist();
        ArgumentManager args = e.getArguments();
        String tier = tierlist.order.stream().filter(s -> args.getText("tier").equalsIgnoreCase(s)).findFirst().orElse("");
        if (tier.isEmpty()) {
            tco.sendMessage("Das ist kein Tier!").queue();
            return;
        }
        if (!d.tc.getId().equals(tco.getId())) return;
        if (d.isNotCurrent(member)) {
            tco.sendMessage(d.getMention(member) + " Du bist nicht dran!").queue();
            return;
        }
        long mem = d.current;
        List<String> list = new ArrayList<>(tierlist.tierlist.get(tier));
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
