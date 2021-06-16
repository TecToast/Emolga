package de.tectoast.emolga.commands.flegmon;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.PepeCommand;
import net.dv8tion.jda.api.managers.AudioManager;

public class FlegmonGehBitteCommand extends PepeCommand {

    public FlegmonGehBitteCommand() {
        super("flegmongehbitte", "Sagt Flegmon, dass er bitte aus dem Voice gehen soll");
        setArgumentTemplate(ArgumentManagerTemplate.noArgs());
    }

    @Override
    public void process(GuildCommandEvent e) {
        AudioManager am = e.getGuild().getAudioManager();
        if(am.isConnected()) {
            am.closeAudioConnection();
            e.reply("Dann gehe ich halt :c");
        } else {
            e.reply("Ich bin doch gar nicht da :c");
        }
    }
}
