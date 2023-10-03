package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.TeamGraphics
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
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


object RandomTeamGrafikCommand :
    Command("randomteamgrafik", "Du bekommst sensationelle und fancyge Teamgrafiken", CommandCategory.Pokemon) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        slash(true, Constants.G.FPL)
        //setCustomPermissions(PermissionPreset.EMOLGAMOD)
    }

    override suspend fun process(e: GuildCommandEvent) {
        e.deferReply()
        val lol = System.currentTimeMillis()
        val (img, randomTeamData) = TeamGraphics.fromDraftPokemon(generateTeam())
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


    private val logger = KotlinLogging.logger {}


        private val sets = listOf(
            mapOf("S" to 4, "A" to 0, "B" to 2, "C" to 4, "D" to 2),
            mapOf("S" to 3, "A" to 2, "B" to 1, "C" to 4, "D" to 2),
            mapOf("S" to 3, "A" to 1, "B" to 3, "C" to 3, "D" to 2),
            mapOf("S" to 2, "A" to 3, "B" to 2, "C" to 3, "D" to 2),
            mapOf("S" to 2, "A" to 2, "B" to 4, "C" to 2, "D" to 2),
            mapOf("S" to 2, "A" to 3, "B" to 2, "C" to 3, "D" to 2),
        )

        fun generateTeam(guild: Long = Constants.G.ASL): List<DraftPokemon> {
            return Tierlist[guild]!!.retrieveTierlistMap(sets.random())
        }


}
