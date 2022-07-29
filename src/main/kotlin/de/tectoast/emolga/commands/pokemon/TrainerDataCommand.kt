package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.buttons.buttonsaves.TrainerData
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import java.awt.Color

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

    override fun process(e: GuildCommandEvent) {
        val trainer = e.arguments.getTranslation("trainer").translation
        if (trainer == "Tom") {
            e.reply("Kleinstein und Machollo auf Level 11 :) Machollo hat die beste Fähigkeit im Spiel :D")
            return
        }
        val dt = TrainerData(trainer)
        e.textChannel.sendMessageEmbeds(
            EmbedBuilder().setTitle("Welches Team möchtest du sehen? Und sollen die Moves etc auch angezeigt werden?")
                .setColor(
                    Color.CYAN
                ).build()
        ).setActionRows(getTrainerDataActionRow(dt, false)).queue { m: Message -> trainerDataButtons[m.idLong] = dt }
    }
}