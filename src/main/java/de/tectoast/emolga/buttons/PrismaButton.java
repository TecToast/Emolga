package de.tectoast.emolga.buttons;

import de.tectoast.emolga.buttons.buttonsaves.PrismaTeam;
import de.tectoast.emolga.utils.Constants;
import de.tectoast.emolga.utils.RequestBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import static de.tectoast.emolga.commands.Command.getAsXCoord;
import static de.tectoast.emolga.commands.Command.prismaTeam;

public class PrismaButton extends ButtonListener {
    public PrismaButton() {
        super("prisma");
    }

    @Override
    public void process(ButtonInteractionEvent e, String name) {
        long id = e.getUser().getIdLong();
        if (id != 297010892678234114L && id != 213725720407441410L && id != Constants.FLOID) {
            e.reply("nö c:").setEphemeral(true).queue();
            return;
        }
        PrismaTeam pt = prismaTeam.get(e.getMessageIdLong());
        if (pt == null) {
            e.reply(":(").queue();
            return;
        }
        PrismaTeam.PokemonData pokemonData = pt.nextMon();
        RequestBuilder.updateSingle("1nCPIc-R5hAsoDXvTGSuGyk2c1K8DQqTBm1NGvLyYYm0", "Teamübersicht!%s%d"
                .formatted(getAsXCoord(pt.getIndex() * 3 + 2), pokemonData.ycoord()), pokemonData.pokemon());
        e.reply("+1").setEphemeral(true).queue();
    }
}
