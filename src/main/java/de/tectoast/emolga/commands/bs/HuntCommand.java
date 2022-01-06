package de.tectoast.emolga.commands.bs;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.entities.TextChannel;
import org.jsolf.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

public class HuntCommand extends Command {

    final ArrayList<String> games = new ArrayList<>(Arrays.asList("Gold", "Silber", "Kristall", "Rubin", "Saphir", "Smaragd", "Feuerrot", "Blattgrün", "Diamant", "Perl", "Platin", "HeartGold", "SoulSilver", "Schwarz", "Weiß", "Schwarz2", "Weiß2", "X", "Y", "Omega Rubin", "Alpha Saphir ", "Sonne", "Mond", "UltraSonne", "UltraMond", "LGP", "LGE", "Schwert", "Schild"));
    final ArrayList<String> methods = new ArrayList<>(Arrays.asList("Random Encounter", "Fishing Encounter", "Soft Resets", "Pokeradar", "Breeden", "DexNav", "Horden", "Kontaktsafari", "SOS", "Inselscanner", "Ultrapforten"));

    public HuntCommand() {
        super("hunt", "`!hunt <help|Spiel> <Methode>` Generiert ein pokemon, welches in dem Spiel mit der Methode gehuntet werden kann oder zeigt die möglichen Spiele/Methoden an", CommandCategory.BS);
        wip();
    }

    @Override
    public void process(GuildCommandEvent e) {
        String msg = e.getMessage().getContentDisplay();
        String[] split = msg.split(" ");
        TextChannel tco = e.getChannel();
        if (split[1].equalsIgnoreCase("help")) {
            tco.sendMessage("Mögliche Spiele: " + String.join(", ", games) + "\n" +
                    "Mögliche Methoden: " + String.join(", ", methods)).queue();
            return;
        }
        Optional<String> opgame = games.stream().filter(s -> s.equalsIgnoreCase(split[1])).findFirst();
        if (opgame.isEmpty()) {
            tco.sendMessage("Das ist kein valides Spiel!").queue();
            return;
        }
        String game = opgame.get();
        Optional<String> opmethod = games.stream().filter(s -> s.equalsIgnoreCase(split[1])).findFirst();
        if (opmethod.isEmpty()) {
            tco.sendMessage("Das ist keine valide Methode!").queue();
            return;
        }
        String method = opmethod.get();
        JSONObject obj = huntjson.getJSONObject(game);
        if (!obj.has(method)) {
            tco.sendMessage("Diese Methode gibt es in " + game + " nicht!").queue();
            return;
        }
        ArrayList<String> list = new ArrayList<>(Arrays.asList(obj.getString(game).split(",")));
        String mon = list.get(new Random().nextInt(list.size()));
        tco.sendMessage("Das generierte pokemon ist " + mon + "!").queue();
    }
}
