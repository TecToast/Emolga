package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;

public class Rule34Command extends Command {

    public Rule34Command() {
        super("rule34", "Schickt ein Rule34-Bild dieses Mons", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.noSpecifiedArgs("!rule34 <Pokemon>", "!rule34 Guardevoir"));
    }

    @Override
    public void process(GuildCommandEvent e) {
        e.reply("Du Schlingel :^)");
        e.reply("https://tenor.com/view/dance-moves-dancing-singer-groovy-gif-17029825");
        sendToMe("Haha " + e.getMember().getAsMention() + " hat in " + e.getChannel().getAsMention() + " dumme sachen gemacht lmao");
    }
}
