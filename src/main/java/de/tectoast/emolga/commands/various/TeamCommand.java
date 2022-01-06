package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.Member;

import java.util.ArrayList;
import java.util.Collections;

public class TeamCommand extends Command {

    public TeamCommand() {
        super("team", "lol", CommandCategory.Various);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
        setCustomPermissions(PermissionPreset.CULT);
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.getGuild().loadMembers().onSuccess(l -> {
            ArrayList<Member> list = new ArrayList<>();
            for (Member mem : l) {
                if (mem.getVoiceState().inVoiceChannel()) list.add(mem);
            }
            Collections.shuffle(list);
            e.reply(list.get(0).getEffectiveName() + " und " + list.get(1).getEffectiveName() + " **VS** " + list.get(2).getEffectiveName() + " und " + list.get(3).getEffectiveName());
        });
    }
}
