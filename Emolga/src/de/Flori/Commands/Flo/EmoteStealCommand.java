package de.Flori.Commands.Flo;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class EmoteStealCommand extends Command {


    public EmoteStealCommand() {
        super("emotesteal", "`!emotesteal`", CommandCategory.Flo);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        if(emotesteal.remove(tco.getId())) {
            tco.sendMessage("Der EmoteSteal wurde deaktiviert!").queue();
        } else {
            emotesteal.add(tco.getId());
            tco.sendMessage("Der EmoteSteal wurde aktiviert!").queue();
        }
    }
}
