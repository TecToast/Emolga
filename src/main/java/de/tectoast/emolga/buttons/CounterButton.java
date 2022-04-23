package de.tectoast.emolga.buttons;

import de.tectoast.emolga.commands.Command;
import de.tectoast.jsolf.JSONObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import static de.tectoast.emolga.commands.Command.shinycountjson;

public class CounterButton extends ButtonListener {
    public CounterButton() {
        super("counter");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) {
        String[] split = name.split(":");
        String method = split[0];
        Member mem = e.getMember();
        JSONObject counter = shinycountjson.getJSONObject("counter");
        String id = mem.getId().equals("893773494578470922") ? "598199247124299776" : mem.getId();
        counter.getJSONObject(method).put(id, counter.getJSONObject(method).optInt(id, 0) + Integer.parseInt(split[1]));
        Command.updateShinyCounts(e);
    }

}
