package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class CatchRateCommand extends Command {

    public CatchRateCommand() {
        super("catchrate", "Gibt die Catch-Rate des jeweiligen Pokemon aus", CommandCategory.Pokemon);
        aliases.add("cr");
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("mon", "Pokemon", "Das Mon lol", Translation.Type.POKEMON).setExample("!catchrate Primarene").build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String mon = e.getArguments().getTranslation("mon").getTranslation();
        e.reply("**" + mon + "** hat eine Catchrate von **" + catchrates.getString(mon) + "**!");
    }
}
