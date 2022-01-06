package de.tectoast.emolga.buttons;

import de.tectoast.emolga.commands.Command;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import org.jsolf.JSONObject;

import static de.tectoast.emolga.commands.Command.shinycountjson;

public class CounterButton extends ButtonListener {
    public CounterButton() {
        super("counter");
    }

    @Override
    public void process(ButtonClickEvent e, String name) {
        String[] split = name.split(":");
        String method = split[0];
        Member mem = e.getMember();
        JSONObject counter = shinycountjson.getJSONObject("counter");
        counter.getJSONObject(method).put(mem.getId(), counter.getJSONObject(method).optInt(mem.getId(), 0) + Integer.parseInt(split[1]));
        Command.updateShinyCounts(e);
    }

}
