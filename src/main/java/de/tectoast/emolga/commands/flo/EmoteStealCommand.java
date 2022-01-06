package de.tectoast.emolga.commands.flo;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;

public class EmoteStealCommand extends Command {


    public EmoteStealCommand() {
        super("emotesteal", "Stealt Emotes... lol", CommandCategory.Flo);
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        if (emoteSteal.remove(tco.getIdLong())) {
            tco.sendMessage("Der EmoteSteal wurde deaktiviert!").queue();
        } else {
            emoteSteal.add(tco.getIdLong());
            tco.sendMessage("Der EmoteSteal wurde aktiviert!").queue();
        }
    }
}
