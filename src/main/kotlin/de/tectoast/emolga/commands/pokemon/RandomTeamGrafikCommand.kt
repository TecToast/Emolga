package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.saveEmolgaJSON
import de.tectoast.emolga.utils.TeamGraphics
import de.tectoast.emolga.utils.json.Emolga
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


class RandomTeamGrafikCommand :
    Command("randomteamgrafik", "Du bekommst sensationelle und fancyge Teamgrafiken", CommandCategory.Pokemon) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        //setCustomPermissions(PermissionPreset.EMOLGAMOD)
    }

    override suspend fun process(e: GuildCommandEvent) {
        val (img, randomTeamData) = TeamGraphics.fromDraftPokemon(RandomTeamCommand.generateTeam(), e)
        val os = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            ImageIO.write(img, "png", os)
            e.textChannel.sendFile(os.toByteArray(), "epischerrandomkader.png").await()
            randomTeamData.run {
                shinyCount.get().let {
                    if (it > 0)
                        e.textChannel.sendMessage("**Du hast einfach ${if (it == 1) "ein Shiny" else "$it Shinies"} bekommen :o**")
                            .queue()
                }
                if (hasDrampa) {
                    e.textChannel.sendMessage("**DRAMPA** (Nr. ${++Emolga.get.statistics.drampaCounter})").queue()
                    e.textChannel.sendMessage("<@446274734389198848>").queue()
                    saveEmolgaJSON()
                }
            }

        }
    }

}