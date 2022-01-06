package de.tectoast.emolga.commands.flegmon;

import de.tectoast.emolga.commands.GuildCommandEvent;
import de.tectoast.emolga.commands.PepeCommand;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;

public class AddSoundCommand extends PepeCommand {


    public AddSoundCommand() {
        super("addsound", "Added einen Sound");
        setArgumentTemplate(ArgumentManagerTemplate.builder()
                .add("sound", "Sound", "Der Sound, der hinzugefügt werden soll", ArgumentManagerTemplate.DiscordFile.of("mp3"))
                .setExample("!addsound <Hier Sound-Datei einfügen>")
                .build());
        wip();
    }

    @Override
    public void process(GuildCommandEvent e) {
        Message.Attachment a = e.getArguments().getAttachment("sound");
        String fileName = a.getFileName();
        File f = new File("audio/clips/" + fileName);
        if(f.exists()) {
            e.reply("Ein Sound mit dem Namen " + fileName.substring(0, fileName.length() - 4) + " gibt es bereits!");
            return;
        }
        a.downloadToFile(f).thenAccept(file -> e.reply("Der Sound wurde hinzugefügt!"));
    }
}
