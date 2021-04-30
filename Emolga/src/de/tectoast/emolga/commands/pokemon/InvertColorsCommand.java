package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.File;

public class InvertColorsCommand extends Command {
    public InvertColorsCommand() {
        super("invertcolors", "`!invertcolors <Mon>`", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        Translation mon;
        boolean shiny;
        if(e.getArgsLength() == 2) {
            mon = getEnglNameWithType(e.getArg(1));
            shiny = true;
        } else {
            mon = getEnglNameWithType(e.getArg(0));
            shiny = false;
        }
        System.out.println("mon = " + mon);
        if (!mon.isFromType(Translation.Type.POKEMON)) {
            tco.sendMessage("Das ist kein Pokemon!").queue();
            return;
        }
        System.out.println("mon = " + mon);
        File f = invertImage(mon.getTranslation(), shiny);
        tco.sendFile(f).queue(message -> {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        });
    }
}
