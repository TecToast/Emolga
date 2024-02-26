package de.tectoast.emolga.bot

import de.tectoast.emolga.defaultChannel
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import io.kotest.core.spec.style.FunSpec

class EmolgaMainTest : FunSpec({
    test("EmolgaMain") {
        defaultChannel.send(
            embeds = Embed(
                title = "<:Happy:967390966153609226>",
                description = "<:Happy:967390966153609226>"
            ).into()
        ).queue()
    }
})
