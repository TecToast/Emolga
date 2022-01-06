package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class GerCommand extends Command {
    public GerCommand() {
        super("ger", "Zeigt den deutschen Namen dieser Sache.", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("stuff", "Pokemon|Attacke|Fähigkeit|Item|Typ", "Die Sache, von der du den englischen Namen haben möchtest", Translation.Type.all())
                .setExample("!ger Primarina")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        String stuff = e.getArguments().getTranslation("stuff").getTranslation();
        if(stuff.equals("Psychokinese")) {
            e.reply("Psychokinese/Psycho");
            return;
        }
        e.reply(stuff);
    }
}
