package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;

public class AnalyseCommand extends Command {
    public AnalyseCommand() {
        super("analyse", "Schickt das Ergebnis des angegebenen Kampfes in den Channel", CommandCategory.Showdown);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("url", "Replay-Link", "Der Replay-Link", ArgumentManagerTemplate.Text.any())
                .setExample("!analyse https://replay.pokemonshowdown.com/oumonotype-82345404").build());
        slash();
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        analyseReplay(e.getArguments().getText("url"), null, tco, null, e.deferReply());
    }

}
