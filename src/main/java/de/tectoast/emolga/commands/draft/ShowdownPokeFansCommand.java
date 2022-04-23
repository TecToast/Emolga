package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.jsolf.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class ShowdownPokeFansCommand extends Command {
    private static final Logger logger = LoggerFactory.getLogger(ShowdownPokeFansCommand.class);

    public ShowdownPokeFansCommand() {
        super("showdownpokefans", "Nimmt ein Showdown-Paste und wandelt es in ein Pokefans-Paste um", CommandCategory.Draft, Constants.CULTID, 821350264152784896L);
        setArgumentTemplate(ArgumentManagerTemplate.builder().add("paste", "Paste", "Das Paste", ArgumentManagerTemplate.Text.any())
                .setExample("!showdownpokefans binzufaul")
                .build());
        wip();
        disable();
    }

    @Override
    public void process(GuildCommandEvent e) {
        JSONArray tosend = new JSONArray();
        //for (String id : ids) {
        String paste = e.getArguments().getText("paste");
        JSONArray oneUser = new JSONArray();
        oneUser.put("HierDenNamenÄndern");
        oneUser.put("HierDieLigaÄndern");
        List<String> pmons = new LinkedList<>();
        for (String s : paste.split("\n")) {
            if (s.trim().length() == 0) continue;
            if (s.contains(":") && !s.contains("Type: Null")) continue;
            logger.info("s = " + s);
            pmons.add(s.trim());
        }
        JSONArray mons = new JSONArray();
        logger.info("pmons = " + pmons);
        pmons.stream()
                .sorted(Comparator.comparing(str -> getDataJSON().getJSONObject(toSDName((String) str)).getJSONObject("baseStats").getInt("spe")).reversed())
                .map(s -> {
                    String[] split = s.split("-");
                    return getGerNameNoCheck(split[0]) + (split.length > 1 ? "-" + split[1] : "");
                })
                .map(str -> str
                        .replace("Boreos-T", "Boreos Tiergeistform")
                        .replace("Voltolos-T", "Voltolos Tiergeistform")
                        .replace("Demeteros-T", "Demeteros Tiergeistform")
                        .replace("Boreos-I", "Boreos Inkarnationsform")
                        .replace("Voltolos-I", "Voltolos Inkarnationsform")
                        .replace("Demeteros-I", "Demeteros Inkarnationsform")
                        .replace("Wolwerock-Tag", "Wolwerock Tagform")
                        .replace("Wolwerock-Nacht", "Wolwerock Nachtform")
                        .replace("Wolwerock-Zw", "Wolwerock Zwielichtform")
                        .replace("Shaymin", "Shaymin Landform")
                        .replace("Durengard", "Durengard Schildform")
                        .replace("Pumpdjinn", "Pumpdjinn XL")
                        .replace("M-", "Mega-")
                        .replace("A-", "Alola-")
                        .replace("G-", "Galar-")
                ).forEach(mons::put);
        oneUser.put(mons);
        tosend.put(oneUser);
        //}
        e.reply(tosend.toString());
    }
}
