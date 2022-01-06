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
        analyseReplay(e.getArguments().getText("url"), null, tco, null, e);
        /*Player[] game = Analysis.analyse(msg.substring(9), m);
        if (game == null) {
            tco.sendMessage("Da in einem der beiden Teams ein Zoroark ist, kann ich das Ergebnis nicht bestimmen!").queue();
            return;
        }
        int deadP1 = 0;
        int deadP2 = 0;
        StringBuilder t1 = new StringBuilder();
        StringBuilder t2 = new StringBuilder();
        System.out.println("Spieler 1");
        for (Pokemon p : game[0].getMons()) {
            if (p.isDead()) deadP1++;
            System.out.println(p.getPokemon() + " -> " + p.isDead());
        }
        System.out.println("Spieler 2");
        for (Pokemon p : game[1].getMons()) {
            if (p.isDead()) deadP2++;
            System.out.println(p.getPokemon() + " -> " + p.isDead());
        }
        long gid = tco.getGuild().getIdLong();
        String winloose = (game[0].getMons().size() - deadP1) + ":" + (game[1].getMons().size() - deadP2);
        boolean p1wins = game[0].isWinner();
        for (Pokemon p : game[0].getMons()) {
            t1.append(getMonName(p.getPokemon(), gid)).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && p1wins ? "X" : "").append("\n");
        }
        for (Pokemon p : game[1].getMons()) {
            t2.append(getMonName(p.getPokemon(), gid)).append(" ").append(p.getKills() > 0 ? p.getKills() + " " : "").append(p.isDead() && !p1wins ? "X" : "").append("\n");
        }
        Database.incrementStatistic("analysis");
        String str = game[0].getNickname() + " " + winloose + " " + game[1].getNickname() + "\n\n" + game[0].getNickname() + ": " + (!p1wins ? "(alle tot)" : "") + "\n" + t1
                + "\n" + game[1].getNickname() + ": " + (p1wins ? "(alle tot)" : "") + "\n" + t2;
        tco.sendMessage(str).queue();*/
    }

}
