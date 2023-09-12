package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.TeamGraphics
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.getCount
import de.tectoast.emolga.utils.json.emolga.increment
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.utils.FileUpload
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


class RandomTeamGrafikCommand :
    Command("randomteamgrafik", "Du bekommst sensationelle und fancyge Teamgrafiken", CommandCategory.Pokemon) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, Constants.G.FPL)
        //setCustomPermissions(PermissionPreset.EMOLGAMOD)
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.deferReply()
        val lol = System.currentTimeMillis()
        val (img, randomTeamData) = TeamGraphics.fromDraftPokemon(RandomTeamCommand.generateTeam())
        logger.info("TeamGraphic took ${System.currentTimeMillis() - lol}ms")
        val os = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            ImageIO.write(img, "png", os)
            e.slashCommandEvent!!.hook.sendFiles(FileUpload.fromData(os.toByteArray(), "epischerrandomkader.png"))
                .await()
            randomTeamData.run {
                shinyCount.get().let {
                    if (it > 0)
                        e.textChannel.sendMessage("**Du hast einfach ${if (it == 1) "ein Shiny" else "$it Shinies"} bekommen :o**")
                            .queue()
                }
                if (hasDrampa) {
                    db.statistics.increment("drampacounter")
                    e.textChannel.sendMessage("**DRAMPA** (Nr. ${db.statistics.getCount("drampacounter")})").queue()
                    e.textChannel.sendMessage("<@446274734389198848>").queue()
                }
            }

        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }

}
