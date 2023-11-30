package de.tectoast.emolga.buttons

import com.mongodb.client.model.UpdateOptions
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.flegmon.ShinyCommand
import de.tectoast.emolga.utils.json.ShinyEvent
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.Modal
import dev.minn.jda.ktx.messages.reply_
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.*
import java.net.URI

object ShinyButton : ButtonListener("shinyevent") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        val split = name.split(";")
        if (name.startsWith("approve")) {
            val (_, user, game, method, points) = split
            e.reply_("Shiny wurde approved!").setEphemeral(true).queue()
            e.jda.getTextChannelById(ShinyCommand.FINALCHANNEL)!!.send(
                "Neues Shiny von <@$user>!\nSpiel: $game\nMethode: $method\nPunkte: $points"
            ).addFiles(FileUpload.fromData(withContext(Dispatchers.IO) {
                URI(e.message.contentRaw.substringAfterLast(": ")).toURL().openStream()
            }, "shiny.png")).queue()
            val uid = user.toLong()
            db.shinyEvent.updateOne(
                filter = ShinyEvent::user eq uid, update = combine(
                    push(ShinyEvent::shinies, ShinyEvent.ShinyData(game, method)),
                    inc(ShinyEvent::points, points.toInt())
                ), options = UpdateOptions().upsert(true)
            )
            e.message.delete().queue()
            updateUser(uid)
        } else if (name.startsWith("deny")) {
            e.replyModal(Modal("shiny;${split[1]}", "Grund eingeben") {
                short("reason", "Grund", required = false)
            }).queue()
        }
    }

    private suspend fun updateUser(uid: Long) {
        val filter = ShinyEvent::user eq uid
        db.shinyEvent.findOne(filter)?.let {
            val channel = EmolgaMain.flegmonjda.getTextChannelById(ShinyCommand.POINTCHANNEL)!!
            if (it.messageId == null) {
                db.shinyEvent.updateOne(
                    filter, set(ShinyEvent::messageId setTo channel.sendMessage("<@$uid>: ${it.points}").await().idLong)
                )
            } else {
                channel.editMessageById(it.messageId, "<@$uid>: ${it.points}").await()
            }
        }
    }
}
