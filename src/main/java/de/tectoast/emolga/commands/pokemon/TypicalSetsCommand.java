package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.records.TypicalSets;

public class TypicalSetsCommand extends Command {

    public TypicalSetsCommand() {
        super("typicalsets", "Zeigt typische Moves/Items/Fähigkeiten für ein Pokemon an", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("mon", "Pokemon", "Das Pokemon",
                        ArgumentManagerTemplate.draftPokemon(), false, "Das ist kein Pokemon!")
                .setExample("!typicalsets Primarina")
                .build());
        slash();
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.reply(TypicalSets.getInstance().buildPokemon(e.getArguments().getText("mon")));
    }
}
