package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;

public class AllowCommand extends Command {

    public AllowCommand() {
        super("allow", "Erlaubt einem anderen User, für dich zu picken", CommandCategory.Draft, Constants.ASLID);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User, der für dich picken darf", ArgumentManagerTemplate.DiscordType.USER)
                .setExample("!allow @Flo")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONObject drafts = getEmolgaJSON().getJSONObject("drafts");
        Member member = e.getMember();
        Optional<JSONObject> op = drafts.keySet().stream().map(drafts::getJSONObject).filter(o -> o.has("guild")).filter(o -> o.getString("guild").equals(e.getGuild().getId()))
                .filter(o -> o.has("table")).filter(o -> o.getLongList("table").contains(member.getIdLong())).findFirst();
        if (op.isPresent()) {
            Member mem = e.getArguments().getMember("user");
            long user = mem.getIdLong();
            JSONObject league = op.get();
            if (!league.has("allowed")) league.put("allowed", new JSONObject());
            JSONObject allowed = league.getJSONObject("allowed");
            if (allowed.has(user)) {
                e.reply("%s pickt bereits für `%s`!".formatted(mem.getEffectiveName(), e.getGuild().retrieveMemberById(allowed.getLong(user)).complete().getEffectiveName()));
                return;
            }
            allowed.put(user, (Object) member.getIdLong());
            e.reply("Du hast %s erlaubt, für dich zu picken!".formatted(mem.getEffectiveName()));
            saveEmolgaJSON();
        } else {
            e.reply("Du nimmst nicht an einer Liga auf diesem Server teil!");
        }
    }
}
