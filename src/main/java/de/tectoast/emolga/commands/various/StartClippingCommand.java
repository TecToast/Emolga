package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.managers.AudioManager;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.jetbrains.annotations.NotNull;

public class StartClippingCommand extends Command {

    public StartClippingCommand() {
        super("startclipping", "Startet die Clip-Funktion c:", CommandCategory.Flo, 919639507740020846L);
    }

    @Override
    public void process(GuildCommandEvent e) {
        AudioManager am = e.getGuild().getAudioManager();
        am.openAudioConnection(e.getMember().getVoiceState().getChannel());
        clips.put(e.getGuild().getIdLong(), new CircularFifoQueue<>(1500));
        am.setReceivingHandler(new AudioReceiveHandler() {
            @Override
            public boolean canReceiveCombined() {
                return true;
            }

            @Override
            public boolean canReceiveUser() {
                return false;
            }

            @Override
            public void handleCombinedAudio(@NotNull CombinedAudio audio) {
                clips.get(e.getGuild().getIdLong()).add(audio.getAudioData(1.0));
            }

            @Override
            public boolean includeUserInCombinedAudio(@NotNull User user) {
                return true;
            }
        });
        e.reply("Sehr interessant was ihr so redet \uD83D\uDC40");
    }
}
