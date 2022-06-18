package de.tectoast.emolga.buttons;

import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.GPIOManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.IOException;

public class FlorixButton extends ButtonListener {

    public FlorixButton() {
        super("florix");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) throws IOException {
        if (e.getUser().getIdLong() != Constants.FLOID || e.getGuild().getIdLong() != Constants.MYSERVER) return;
        long mid = e.getMessageIdLong();
        String[] split = name.split(":");
        GPIOManager.PC pc = GPIOManager.PC.byMessage(name.contains(":") ? Long.parseLong(split[1]) : mid);
        boolean on = GPIOManager.isOn(pc);
        switch (split[0]) {
            case "startserver" -> {
                if (on) {
                    e.reply("Der Server ist bereits an!").setEphemeral(true).queue();
                    break;
                }
                GPIOManager.startServer(pc);
                e.reply("Der Server wurde gestartet!").setEphemeral(true).queue();
            }
            case "stopserver" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                e.replyEmbeds(new EmbedBuilder().setTitle("Bist du dir sicher, dass du den Server herunterfahren möchtest?").setColor(Color.RED).build()).addActionRow(Button.danger("florix;stopserverreal:" + mid, "Ja"), Button.success("florix;no", "Nein")).queue();
            }
            case "poweroff" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                e.replyEmbeds(new EmbedBuilder().setTitle("Bist du dir sicher, dass POWEROFF aktiviert werden soll?").setColor(Color.RED).build()).addActionRow(Button.danger("florix;poweroffreal:" + mid, "Ja"), Button.success("florix;no", "Nein")).queue();
            }
            case "status" -> e.reply("Der Server ist %s!".formatted(on ? "an" : "aus")).setEphemeral(true).queue();
            case "stopserverreal" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                GPIOManager.stopServer(pc);
                e.reply("Der Server wurde heruntergefahren!").setEphemeral(true).queue(i -> i.deleteMessageById(e.getMessageId()).queue());
            }
            case "poweroffreal" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                GPIOManager.powerOff(pc);
                e.reply("Power-Off wurde aktiviert!").setEphemeral(true).queue(i -> i.deleteMessageById(e.getMessageId()).queue());
            }
        }
    }
}
