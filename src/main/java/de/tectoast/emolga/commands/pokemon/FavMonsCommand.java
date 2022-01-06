package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class FavMonsCommand extends Command {

    public FavMonsCommand() {
        super("favmons", "Erstelle eine Liste deiner Fav Mons", CommandCategory.Pokemon);
        wip();
    }

    @Override
    public void process(GuildCommandEvent e) {

    }
}
