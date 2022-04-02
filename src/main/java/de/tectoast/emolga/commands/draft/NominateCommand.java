package de.tectoast.emolga.commands.draft;

import de.tectoast.emolga.buttons.buttonsaves.Nominate;
import de.tectoast.emolga.commands.PrivateCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;

public class NominateCommand extends PrivateCommand {

    final Comparator<JSONObject> tiercomparator;

    public NominateCommand() {
        super("nominate");
        setIsAllowed(u -> getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS").getJSONObject("picks").has(u.getId()));
        List<String> tiers = Arrays.asList("S", "A", "B", "C", "D");
        tiercomparator = Comparator.comparing(o -> tiers.indexOf(o.getString("tier")));
    }

    @Override
    public void process(PrivateMessageReceivedEvent e) {
        JSONObject nds = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS");
        JSONObject nom = nds.getJSONObject("nominations");
        int currentDay = nom.getInt("currentDay");
        if (!nom.has(currentDay)) nom.put(currentDay, new JSONObject());
        if (nom.getJSONObject(String.valueOf(currentDay)).has(e.getAuthor().getId())) {
            e.getChannel().sendMessage("Du hast für diesen Spieltag dein Team bereits nominiert!").queue();
            return;
        }
        JSONArray arr = nds.getJSONObject("picks").getJSONArray(e.getAuthor().getId());
        List<JSONObject> list = arr.toJSONList();
        list.sort(tiercomparator);
        List<String> b = list.stream().map(o -> o.getString("name")).collect(Collectors.toList());
        Nominate n = new Nominate(list);
        e.getChannel().sendMessageEmbeds(new EmbedBuilder().setTitle("Nominierungen").setColor(Color.CYAN).setDescription(n.generateDescription()).build())
                .setActionRows(addAndReturn(getActionRows(b, s -> Button.primary("nominate;" + s, s)), ActionRow.of(Button.success("nominate;FINISH", Emoji.fromUnicode("✅")))))
                .queue(m -> nominateButtons.put(m.getIdLong(), n));

    }
}
