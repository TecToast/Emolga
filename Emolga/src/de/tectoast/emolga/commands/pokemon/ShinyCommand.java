package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.File;

public class ShinyCommand extends Command {
    public ShinyCommand() {
        super("shiny", "`!shiny <Pokemon>` Zeigt das Shiny des Pokemons an", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        String mon = getEnglNameWithType(msg.substring(7));
        if (!mon.startsWith("pkmn;")) {
            tco.sendMessage("Das ist kein Pokemon!").queue();
            return;
        }
        File f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        //if(!f.exists()) f = new File("../Showdown/sspclient/sprites/gen5-shiny/" + mon.split(";")[1].toLowerCase() + ".png");
        System.out.println(f.getPath());
        System.out.println(f.exists());
        tco.sendFile(f).queue();
    }
}
