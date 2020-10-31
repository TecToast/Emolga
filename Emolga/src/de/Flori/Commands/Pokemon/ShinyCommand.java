package de.Flori.Commands.Pokemon;

import de.Flori.Commands.Command;
import de.Flori.Commands.CommandCategory;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class ShinyCommand extends Command {
    public ShinyCommand() {
        super("shiny", "`!shiny <Pokemon>` Zeigt das Shiny des Pokemons an", CommandCategory.Pokemon);
    }

    @Override
    public void process(GuildMessageReceivedEvent e) {
        TextChannel tco = e.getChannel();
        String msg = e.getMessage().getContentDisplay();
        String mon = getGerName(msg.substring(7));
        if (!mon.startsWith("pkmn;")) {
            tco.sendMessage("Das ist kein Pokemon!").queue();
            return;
        }
        tco.sendMessage(getShinySpriteJSON().getString(String.valueOf(getDataJSON().getJSONObject(mon.substring(5).toLowerCase()).getInt("num")))).queue();
    }
}
