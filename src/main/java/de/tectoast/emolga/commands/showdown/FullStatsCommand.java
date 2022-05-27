package de.tectoast.emolga.commands.showdown;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.records.UsageData;
import de.tectoast.emolga.utils.sql.DBManagers;
import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;

public class FullStatsCommand extends Command {

    public FullStatsCommand() {
        super("fullstats", "Zeigt die volle Statistik von einem Pokemon an (Kills/Uses/etc)", CommandCategory.Showdown);
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("mon", "Pokemon", "Das Pokemon",
                        ArgumentManagerTemplate.withPredicate("Pokemon", s -> getDraftGerName(s).isFromType(Translation.Type.POKEMON), false, draftnamemapper), false, "Das ist kein Pokemon!")
                .setExample("!fullstats Primarina")
                .build());
        slash();
    }

    @Override
    public void process(GuildCommandEvent e) {
        String mon = e.getArguments().getText("mon");
        UsageData data = DBManagers.FULL_STATS.getData(mon);
        if (data == null) {
            e.reply(new EmbedBuilder().setTitle("Dieses Pokemon befindet sich nicht in der Gesamtstatistik!").setColor(Color.RED).build());
            return;
        }
        String kpu = String.valueOf((double) data.kills() / (double) data.uses());
        kpu = kpu.substring(0, kpu.indexOf('.') + Math.min(kpu.length() - 1, 6));
        String wpu = String.valueOf((double) data.wins() / (double) data.uses());
        wpu = wpu.substring(0, wpu.indexOf('.') + Math.min(wpu.length() - 1, 6));
        e.reply(new EmbedBuilder().setColor(Color.CYAN)
                .setTitle("Gesamtstatistik von " + mon + " in " + (replayCount.get() - 6212) + " Replays")
                .addField("Kills", String.valueOf(data.kills()), false)
                .addField("Deaths", String.valueOf(data.deaths()), false)
                .addField("Uses", String.valueOf(data.uses()), false)
                .addField("Wins", String.valueOf(data.wins()), false)
                .addField("Looses", String.valueOf(data.looses()), false)
                .addField("Kills/Uses", kpu, false)
                .addField("Wins/Uses", wpu, false)
                .build()
        );
    }
}
