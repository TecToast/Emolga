package de.tectoast.emolga.buttons.buttonsaves;

import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.*;

public class Nominate {
    final List<JSONObject> mons;
    final List<JSONObject> nominated;
    final List<JSONObject> notNominated;

    public Nominate(List<JSONObject> mons) {
        this.mons = mons;
        this.nominated = new LinkedList<>(mons);
        this.notNominated = new LinkedList<>();
    }

    public void unnominate(String name) {
        JSONObject o = mons.stream().filter(obj -> obj.getString("name").equals(name)).findFirst().orElse(null);
        this.nominated.remove(o);
        this.notNominated.add(o);
    }

    public void nominate(String name) {
        JSONObject o = mons.stream().filter(obj -> obj.getString("name").equals(name)).findFirst().orElse(null);
        this.notNominated.remove(o);
        this.nominated.add(o);
    }

    private boolean isNominated(String s) {
        return nominated.stream().anyMatch(o -> o.getString("name").equals(s));
    }

    public String generateDescription() {
        List<String> tiers = Arrays.asList("S", "A", "B", "C", "D");
        StringBuilder msg = new StringBuilder("**Nominiert: (" + nominated.size() + ")**\n");
        for (String o : tiers) {
            nominated.stream().filter(s -> s.getString("tier").equals(o)).map(o2 -> o2.getString("name")).sorted().forEach(mon -> msg.append(o).append(": ").append(mon).append("\n"));
        }
        msg.append("\n**Nicht nominiert: (").append(notNominated.size()).append(")**\n");
        for (String o : tiers) {
            notNominated.stream().filter(s -> s.getString("tier").equals(o)).map(o2 -> o2.getString("name")).sorted().forEach(mon -> msg.append(o).append(": ").append(mon).append("\n"));
        }
        return msg.toString();
    }

    public void render(ButtonInteractionEvent e) {
        e.editMessageEmbeds(new EmbedBuilder().setTitle("Nominierungen").setColor(Color.CYAN).setDescription(generateDescription()).build())
                .setActionRows(addAndReturn(getActionRows(mons.stream().map(o -> o.getString("name")).collect(Collectors.toList()), s -> isNominated(s) ? Button.primary("nominate;" + s, s) : Button.secondary("nominate;" + s, s)), ActionRow.of(Button.success("nominate;FINISH", Emoji.fromUnicode("✅")))))
                .queue();
    }

    public String buildJSONString() {
        List<String> tiers = Arrays.asList("S", "A", "B", "C", "D");
        StringBuilder msg = new StringBuilder();
        for (String o : tiers) {
            nominated.stream().filter(s -> s.getString("tier").equals(o)).map(o2 -> o2.getString("name")).sorted().forEach(mon -> msg.append(mon).append(";"));
        }
        msg.setLength(msg.length() - 1);
        msg.append("###");
        for (String o : tiers) {
            notNominated.stream().filter(s -> s.getString("tier").equals(o)).map(o2 -> o2.getString("name")).sorted().forEach(mon -> msg.append(mon).append(";"));
        }
        msg.setLength(msg.length() - 1);
        return msg.toString();
        //return nominated.stream().map(o -> o.getString("name")).collect(Collectors.joining(",")) + "###" + notNominated.stream().map(o -> o.getString("name")).collect(Collectors.joining(","));
    }

    public void finish(ButtonInteractionEvent e, boolean now) {
        if (now) {
            JSONObject nom = getEmolgaJSON().getJSONObject("drafts").getJSONObject("NDS").getJSONObject("nominations");
            JSONObject day = nom.getJSONObject(String.valueOf(nom.getInt("currentDay")));
            if (day.has(e.getUser().getId())) {
                e.reply("Du hast dein Team bereits für diesen Spieltag nominiert!").queue();
                return;
            }
            day.put(e.getUser().getId(), buildJSONString());
            saveEmolgaJSON();
            e.reply("Deine Nominierung wurde gespeichert!").queue();
        } else {
            if (nominated.size() != 11) {
                e.reply("Du musst exakt 11 Pokemon nominieren!").setEphemeral(true).queue();
            } else {
                e.editMessageEmbeds(new EmbedBuilder().setTitle("Bist du dir wirklich sicher? Die Nominierung kann nicht rückgängig gemacht werden!").setColor(Color.CYAN).setDescription(generateDescription()).build())
                        .setActionRows(ActionRow.of(Button.success("nominate;FINISHNOW", "Ja"), Button.danger("nominate;CANCEL", "Nein")))
                        .queue();
            }
        }
    }
}
