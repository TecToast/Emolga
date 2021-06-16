package de.tectoast.emolga.utils.music;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class SoundSendHandler implements AudioSendHandler {

    public final LinkedList<byte[]> bytes = new LinkedList<>();

    public void loadSoundBytes(LinkedList<byte[]> l) {
        bytes.addAll(l);
    }

    @Override
    public boolean canProvide() {
        return bytes.size() > 0;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        byte[] array = bytes.removeFirst();
        System.out.println("array.length = " + array.length);
        return ByteBuffer.wrap(array);
    }

    @Override
    public boolean isOpus() {
        return false;
    }
}
