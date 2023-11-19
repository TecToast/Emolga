package de.tectoast.emolga.other

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.defaultGuild
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.onCommand
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.nio.file.Files

class AttachmentTest : FunSpec({
    test("AttachmentTest") {
        defaultGuild.upsertCommand("testattachment", "test")
            .addOption(OptionType.ATTACHMENT, "attachment", "attachment", true).queue()
        val hold = CompletableDeferred<Unit>()
        jda.onCommand("testattachment") {
            val tempfile = Files.createTempFile("test", ".png").toFile()
            try {
                it.getOption("attachment")!!.asAttachment.proxy.downloadToFile(tempfile).await()
            } catch (ex: Exception) {
                ex.printStackTrace()
                return@onCommand
            }
            tempfile.exists() shouldBe true
            hold.complete(Unit)
        }
        hold.await()
    }
})
