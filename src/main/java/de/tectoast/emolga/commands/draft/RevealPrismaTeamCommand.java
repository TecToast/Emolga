package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.buttons.buttonsaves.PrismaTeam;
import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.Collections;
import java.util.List;

public class RevealPrismaTeamCommand extends Command {

    public RevealPrismaTeamCommand() {
        super("revealprismateam", "Revealt die Prisma Teams lol", CommandCategory.Draft, Constants.FLPID);
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114L));
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("user", "User", "Der User lol", ArgumentManagerTemplate.DiscordType.USER)
                .setExample("!revealprismateam @HennyHahn")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        Member user = e.getArguments().getMember("user");
        JSONObject prisma = getEmolgaJSON().getJSONObject("drafts").getJSONObject("Prisma");
        JSONObject picks = prisma.getJSONObject("picks");
        List<JSONObject> jsonList = picks.getJSONList(user.getId());
        Collections.reverse(jsonList);
        e.getChannel().sendMessage("**" + user.getEffectiveName() + "**").setActionRow(Button.primary("prisma;lol", "Nächstes Pokemon"))
                .queue(m -> prismaTeam.put(m.getIdLong(), new PrismaTeam(jsonList.stream()
                        .map(o -> o.getString("name")).toList(), prisma.getLongList("table").indexOf(user.getIdLong()))));
    }
}
