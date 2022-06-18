package de.tectoast.emolga.utils.music;

import net.dv8tion.jda.api.audio.AudioSendHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.LinkedList;

public class SoundSendHandler implements AudioSendHandler {

    private static final Logger logger = LoggerFactory.getLogger(SoundSendHandler.class);

    public final LinkedList<byte[]> bytes = new LinkedList<>();

    @Override
    public boolean canProvide() {
        return bytes.size() > 0;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        byte[] array = bytes.removeFirst();
        logger.info("array.length = " + array.length);
        return ByteBuffer.wrap(array);
    }

    @Override
    public boolean isOpus() {
        return false;
    }
}
