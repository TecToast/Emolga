package de.tectoast.emolga.buttons

import com.mongodb.client.model.UpdateOptions
import de.tectoast.emolga.commands.flegmon.EventShinyCommand
import de.tectoast.emolga.utils.json.ShinyEvent
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.messages.send
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload
import org.litote.kmongo.combine
import org.litote.kmongo.eq
import org.litote.kmongo.inc
import org.litote.kmongo.push
import java.net.URI

object ShinyEventButton : ButtonListener("shinyevent") {
    override suspend fun process(e: ButtonInteractionEvent, name: String) {
        if (name.startsWith("approve")) {
            val (_, uid, game, method, points) = name.split(";")
            e.jda.getTextChannelById(EventShinyCommand.FINALCHANNEL)!!.send(
                "Neues Shiny von <@$uid>!\n" +
                        "Spiel: $game\n" +
                        "Methode: $method\n" +
                        "Punkte: $points"
            ).addFiles(FileUpload.fromData(withContext(Dispatchers.IO) {
                URI(e.message.contentRaw.substringAfterLast(": ")).toURL().openStream()
            }, "shiny.png")).queue()
            db.shinyEvent.updateOne(
                filter = ShinyEvent::user eq uid.toLong(),
                update = combine(
                    push(ShinyEvent::shinies, ShinyEvent.ShinyData(game, method)),
                    inc(ShinyEvent::points, points.toInt())
                ),
                options = UpdateOptions()
                    .upsert(true)
            )
        }
    }
}
