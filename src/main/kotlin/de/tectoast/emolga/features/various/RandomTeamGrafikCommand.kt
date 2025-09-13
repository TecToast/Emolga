package de.tectoast.emolga.features.various

import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.NoArgs
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.TeamGraphics
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.dv8tion.jda.api.utils.FileUpload
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.time.TimeSource

object RandomTeamGrafikCommand : CommandFeature<NoArgs>(
    NoArgs(), CommandSpec("randomteamgrafik", "Du bekommst sensationelle und fancyge Teamgrafiken")
) {
    private val logger = KotlinLogging.logger {}

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.deferReply()
        val timeMark = TimeSource.Monotonic.markNow()
        val (img, randomTeamData) = TeamGraphics.fromDraftPokemon(generateTeam())
        logger.info("TeamGraphic took ${timeMark.elapsedNow().inWholeMilliseconds}ms")
        val os = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            ImageIO.write(img, "png", os)
            iData.replyAwait(files = FileUpload.fromData(os.toByteArray(), "epischerrandomkader.png").into())
            randomTeamData.run {
                shinyCount.get().let {
                    if (it > 0) iData.reply("**Du hast einfach ${if (it == 1) "ein Shiny" else "$it Shinies"} bekommen :o**")
                }
                if (hasDrampa) {
                    iData.reply("**DRAMPA**")
                    iData.reply("<@446274734389198848>")
                }
            }

        }
    }

    private val sets = listOf(
        mapOf("S" to 4, "A" to 0, "B" to 2, "C" to 4, "D" to 2),
        mapOf("S" to 3, "A" to 2, "B" to 1, "C" to 4, "D" to 2),
        mapOf("S" to 3, "A" to 1, "B" to 3, "C" to 3, "D" to 2),
        mapOf("S" to 2, "A" to 3, "B" to 2, "C" to 3, "D" to 2),
        mapOf("S" to 2, "A" to 2, "B" to 4, "C" to 2, "D" to 2),
        mapOf("S" to 2, "A" to 3, "B" to 2, "C" to 3, "D" to 2),
    )

    private suspend fun generateTeam(guild: Long = Constants.G.ASL): List<DraftPokemon> {
        return Tierlist[guild]!!.retrieveTierlistMap(sets.random())
    }
}
