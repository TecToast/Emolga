package de.tectoast.emolga.utils.music

import net.dv8tion.jda.api.audio.AudioSendHandler
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.*

class SoundSendHandler : AudioSendHandler {
    private val bytes = LinkedList<ByteArray>()
    override fun canProvide(): Boolean {
        return bytes.size > 0
    }

    override fun provide20MsAudio(): ByteBuffer? {
        val array = bytes.removeFirst()
        logger.info("array.length = " + array.size)
        return ByteBuffer.wrap(array)
    }

    override fun isOpus(): Boolean {
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SoundSendHandler::class.java)
    }
}