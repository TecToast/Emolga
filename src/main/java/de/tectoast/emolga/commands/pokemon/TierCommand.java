package de.tectoast.emolga.commands.pokemon;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.draft.Tierlist;
import net.dv8tion.jda.api.entities.TextChannel;

public class TierCommand extends Command {
    public TierCommand() {
        super("tier", "Zeigt das Tier des Pokemon in der Liga dieses Servers an.", CommandCategory.Pokemon);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("mon", "Pokemon", "Das Pokemon", ArgumentManagerTemplate.draftPokemon())
                .setExample("!tier M-Galagladi")
                .build());
    }

    @Override
    public void process(GuildCommandEvent e) {
        TextChannel tco = e.getChannel();
        String pkmn = getDraftGerName(e.getArguments().getText("mon")).getTranslation();
        Tierlist tierlist = Tierlist.getByGuild(tco.getGuild().getId());
        if (tierlist == null) {
            e.reply("Auf diesem Server ist keine Tierliste hinterlegt! Wenn du dies tun möchtest, melde dich bei Flo/TecToast.");
            return;
        }
        String tier = tierlist.getTierOf(pkmn);
        if (!tier.isEmpty()) {
            tco.sendMessage(pkmn + " ist im " + tier + "-Tier!").queue();
        } else {
            tco.sendMessage(pkmn + " befindet sich nicht in der Tierliste!").queue();
        }
    }
}
