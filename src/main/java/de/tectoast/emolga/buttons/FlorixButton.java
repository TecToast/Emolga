package de.tectoast.emolga.buttons;

import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.GPIOManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FlorixButton extends ButtonListener {

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    public FlorixButton() {
        super("florix");
    }

    @Override
    public void process(ButtonClickEvent e, String name) throws IOException {
        if (e.getUser().getIdLong() != Constants.FLOID || e.getGuild().getIdLong() != Constants.MYSERVER) return;
        boolean on = GPIOManager.isOn();
        switch (name) {
            case "startserver" -> {
                if (on) {
                    e.reply("Der Server ist bereits an!").setEphemeral(true).queue();
                    break;
                }
                GPIOManager.startServer();
                e.reply("Der Server wurde gestartet!").setEphemeral(true).queue();
            }
            case "stopserver" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                e.replyEmbeds(new EmbedBuilder().setTitle("Bist du dir sicher, dass du den Server herunterfahren möchtest?").setColor(Color.RED).build()).addActionRow(Button.danger("florix;stopserverreal", "Ja"), Button.success("florix;no", "Nein")).queue();
            }
            case "poweroff" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                e.replyEmbeds(new EmbedBuilder().setTitle("Bist du dir sicher, dass POWEROFF aktiviert werden soll?").setColor(Color.RED).build()).addActionRow(Button.danger("florix;poweroffreal", "Ja"), Button.success("florix;no", "Nein")).queue();
            }
            case "status" -> e.reply("Der Server ist %s!".formatted(on ? "an" : "aus")).setEphemeral(true).queue();
            case "stopserverreal" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                GPIOManager.stopServer();
                e.reply("Der Server wurde heruntergefahren!").setEphemeral(true).queue(i -> i.deleteMessageById(e.getMessageId()).queue());
            }
            case "poweroffreal" -> {
                if (!on) {
                    e.reply("Der Server ist bereits aus!").setEphemeral(true).queue();
                    break;
                }
                GPIOManager.powerOff();
                e.reply("Power-Off wurde aktiviert!").setEphemeral(true).queue(i -> i.deleteMessageById(e.getMessageId()).queue());
            }
        }
    }
}
