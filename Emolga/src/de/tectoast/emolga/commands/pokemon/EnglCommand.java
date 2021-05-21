package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class EnglCommand extends Command {
    public EnglCommand() {
        super("engl", "Zeigt den englischen Namen dieser Sache.", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .addEngl("stuff", "Pokemon|Attacke|Fähigkeit|Item", "Die Sache, von der du den englischen Namen haben möchtest", Translation.Type.all())
                .setExample("!engl Primarene")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.reply(e.getArguments().getTranslation("stuff").getTranslation());
    }
}
