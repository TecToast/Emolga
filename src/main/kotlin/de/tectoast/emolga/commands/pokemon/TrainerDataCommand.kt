package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.buttons.buttonsaves.TrainerData
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.embedColor
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send

class TrainerDataCommand :
    Command("trainerdata", "Zeigt die Pokemon eines Arenaleiters/Top 4 Mitglieds an", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "trainer",
                "Arenaleiter/Top4",
                "Der Arenaleiter/Das Top 4 Mitglied, von dem du das Team wissen möchtest",
                Translation.Type.TRAINER.or("Tom")
            ).setExample("!trainerdata Skyla").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val trainer = e.arguments.getTranslation("trainer").translation
        if (trainer == "Tom") {
            e.reply("Kleinstein und Machollo auf Level 11 :) Machollo hat die beste Fähigkeit im Spiel :D")
            return
        }
        val dt = TrainerData(trainer)
        e.textChannel.send(
            embeds = Embed(
                title = "Welches Team möchtest du sehen? Und sollen die Moves etc auch angezeigt werden?",
                color = embedColor
            ).into(), components = getTrainerDataActionRow(dt, false)
        ).queue { trainerDataButtons[it.idLong] = dt }
    }
}