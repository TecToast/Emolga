package de.tectoast.emolga.other

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.defaultChannel
import de.tectoast.emolga.defaultGuild
import de.tectoast.emolga.keepAlive
import de.tectoast.emolga.selectmenus.RoleGetMenu
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.events.onStringSelect
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.interactions.components.StringSelectMenu
import dev.minn.jda.ktx.messages.reply_
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.nio.file.Files

class Other : FunSpec({
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

    test("SelectMenuTest") {
        val list = listOf("A", "B", "C")
        defaultChannel.sendMessage("Test")
            .addActionRow(StringSelectMenu("1test", valueRange = list.indices, options = list.map {
                SelectOption(
                    it,
                    it,
                    "Hier steht ne tolle Beschreibung",
                    emoji = Emoji.fromCustom("Happy", 967390966153609226, false)
                )
            })).queue()
        jda.onStringSelect("1test") {
            it.reply_(":)", ephemeral = true).queue()
        }
        keepAlive()
    }

    test("SendMenu") {
        defaultChannel.sendMessage(":)").addComponents(RoleGetMenu.getActionRows()).queue()
        jda.onStringSelect("roleget") {
            it.reply_(":)", ephemeral = true).queue()
        }
        keepAlive()
    }
})
