package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.function.Consumer

class ClipCommand : Command("clip", "Clippt. lol.", CommandCategory.Flo, 919639507740020846L) {
    @Throws(Exception::class)
    override fun process(e: GuildCommandEvent) {
        val bytes = clips[e.guild.idLong]!!
        logger.info("bytes.size() = {}", bytes.size)
        logger.info("bytes.get(0).length = {}", bytes[0].size)
        logger.info("bytes.get(1).length = {}", bytes[1].size)
        val buffer = ByteBuffer.allocate(1500 * 3840)
        bytes.forEach(Consumer { src: ByteArray? -> buffer.put(src) })
        val array = buffer.array()
        /*AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(array));
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File("testclip.wav"));*/
        val fos = FileOutputStream("testfileee.raw")
        fos.write(array)
        fos.close()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClipCommand::class.java)
    }

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }
}