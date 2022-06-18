package de.tectoast.emolga.utils.automation.structure;

import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.tectoast.emolga.commands.Command.getEmolgaJSON;
import static de.tectoast.emolga.commands.Command.saveEmolgaJSON;

public class ModalConfigurator {

    private final List<TextInput> actionRows = new LinkedList<>();
    private final Map<String, Function<String, Object>> mapper = new HashMap<>();
    private String id;
    private String title;


    private ModalConfigurator() {
    }

    public static ModalConfigurator create() {
        return new ModalConfigurator();
    }

    public Modal buildModal() {
        return buildModal(0);
    }

    public Modal buildModal(int page) {
        return Modal.create("modalconfigurator;" + id, title)
                .addActionRows(actionRows.stream().skip(page * 5L).limit(5).map(ActionRow::of).toList())
                .build();
    }

    public void handle(ModalInteractionEvent e) {
        List<ModalMapping> values = e.getValues();
        JSONObject o = getEmolgaJSON().createOrGetJSON("configuration").createOrGetJSON(e.getGuild().getIdLong()).createOrGetJSON(id);
        Member member = e.getMember();
        for (ModalMapping mm : values) {
            String id = mm.getId();
            String value = mm.getAsString();
            if (value.isBlank()) continue;
            Object mappedValue = mapper.containsKey(id) ? mapper.get(id).apply(value) : value;
            if (mappedValue == null) {
                e.replyEmbeds(new EmbedBuilder()
                        .setTitle("Das Argument %s ist ungültig für \"%s\"!".formatted(value, actionRows.stream()
                                .filter(ti -> ti.getId().equals(id)).findFirst().orElseThrow().getLabel()))
                        .setColor(0xFF0000)
                        .setFooter("Aufgerufen von %s (%s)".formatted(member.getEffectiveName(), member.getUser().getAsTag()))
                        .build()).queue();
                return;
            }
            o.put(id, mappedValue);
        }
        e.replyEmbeds(new EmbedBuilder()
                .setTitle("Deine Konfiguration wurde erfolgreich gespeichert!")
                .setColor(0x00FF00)
                .setFooter("Aufgerufen von %s (%s)".formatted(member.getEffectiveName(), member.getUser().getAsTag()))
                .build()).queue();
        saveEmolgaJSON();
    }

    public void initialize(SlashCommandInteractionEvent e) {
        if (actionRows.size() <= 5) {
            e.replyModal(buildModal()).queue();
            return;
        }
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Welche Seite möchtest du sehen?")
                .setColor(Color.CYAN);
        SelectMenu.Builder sm = SelectMenu.create("modalconfigurator;" + id);
        for (int i = 0; i < (actionRows.size() / 5) + 1; i++) {
            int realSite = i + 1;
            embed.addField("Seite " + realSite,
                    actionRows.stream().skip(i * 5).limit(5)
                            .map(TextInput::getLabel).collect(Collectors.joining("\n")), false);
            sm.addOption("Seite " + realSite, String.valueOf(i));
        }
        e.replyEmbeds(embed.build()).addActionRow(sm.build()).queue();
    }

    public ModalConfigurator id(String id) {
        this.id = id;
        return this;
    }

    public ModalConfigurator title(String title) {
        this.title = title;
        return this;
    }

    public ModalConfigurator actionRows(TextInput... actionRows) {
        this.actionRows.addAll(Arrays.asList(actionRows));
        return this;
    }

    public ModalConfigurator mapper(Function<String, Object> mapper, String... ids) {
        for (String s : ids) {
            this.mapper.put(s, mapper);
        }
        return this;
    }

    @Override
    public String toString() {
        return "ModalConfigurator{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               '}';
    }
}
