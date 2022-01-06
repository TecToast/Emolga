package de.tectoast.emolga.commands.various;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.commands.CommandCategory;
import de.tectoast.emolga.commands.GuildCommandEvent;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.ByteBuffer;

public class ClipCommand extends Command {

    public ClipCommand() {
        super("clip", "Clippt. lol.", CommandCategory.Admin, 919639507740020846L);
    }

    @Override
    public void process(GuildCommandEvent e) throws Exception {
        CircularFifoQueue<byte[]> bytes = clips.get(e.getGuild().getIdLong());
        System.out.println("bytes.size() = " + bytes.size());
        System.out.println("bytes.get(0).length = " + bytes.get(0).length);
        System.out.println("bytes.get(1).length = " + bytes.get(1).length);
        ByteBuffer buffer = ByteBuffer.allocate(1500 * 3840);
        bytes.forEach(buffer::put);
        byte[] array = buffer.array();
        /*AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(array));
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File("testclip.wav"));*/
        FileOutputStream fos = new FileOutputStream("testfileee.raw");
        fos.write(array);
        fos.close();
    }
}
