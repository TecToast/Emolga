package de.tectoast.emolga.domain.league.teamgraphic.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.league.config.repository.LeagueConfigRepository
import de.tectoast.emolga.domain.league.core.repository.LeagueCoreRepository
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamData
import de.tectoast.emolga.domain.league.teamgraphic.model.TeamGraphicStyle
import de.tectoast.emolga.domain.league.teamgraphic.repository.TeamGraphicRepository
import de.tectoast.emolga.utils.joinToTeammates
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.MessageEdit
import dev.minn.jda.ktx.messages.into
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.core.annotation.Single
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.seconds

@Single
class TeamGraphicManager(
    private val teamDataCreationService: TeamDataCreationService,
    private val leagueCoreRepo: LeagueCoreRepository,
    private val leagueConfigRepo: LeagueConfigRepository,
    private val teamgraphicRepo: TeamGraphicRepository,
    private val channelInterface: ChannelInterface,
    private val generator: TeamGraphicGenerator
) {
    private val logger = KotlinLogging.logger {}


    private suspend fun generateForLeague(
        leagueName: String, style: TeamGraphicStyle? = null
    ): List<Pair<TeamData, BufferedImage>> {
        val teamDataList = teamDataCreationService.allFromLeague(leagueName)
        val style = style ?: getTeamGraphicStyleOfLeague(leagueName) ?: return emptyList()
        return teamDataList.map { teamData ->
            logger.info { "Generating team graphic for ${teamData.teamOwner ?: "Unknown Owner"}" }
            teamData to generator.generate(teamData, style)
        }
    }

    suspend fun generateAndSendInDraftChannels(guild: Long) {
        val leagues = leagueCoreRepo.getAllScalarLeagueData(guild)
        for (league in leagues) {
            val channel = league.draftChannel ?: continue
            val leagueName = league.leagueName
            logger.info { "Generating and sending team graphics for league $leagueName in channel $channel" }
            generateAndSendForLeague(leagueName, channel)
            delay(10.seconds)
        }
    }

    suspend fun generateAndSendForLeague(leagueName: String, channelId: Long, style: TeamGraphicStyle? = null) {
        teamgraphicRepo.setChannelId(leagueName, channelId)
        for ((idx, data) in generateForLeague(leagueName, style).withIndex()) {
            val (teamData, image) = data
            val id = channelInterface.sendMessage(
                channelId,
                MessageCreate(
                    teamData.users.joinToTeammates(),
                    files = image.toFileUpload().into()
                )
            )
            if (id != null)
                teamgraphicRepo.setMessageId(leagueName, idx, id)
        }
    }

    suspend fun generateAndStoreInFS(leagueName: String, style: TeamGraphicStyle?) {
        val targetDir = File("/teamgraphics/generated/${leagueName}")
        targetDir.mkdirs()
        withContext(Dispatchers.IO) {
            generateForLeague(leagueName, style).forEachIndexed { index, (_, image) ->
                ImageIO.write(image, "png", targetDir.resolve("$index.png"))
            }
        }
    }

    suspend fun updateSingleTeamGraphic(leagueName: String, idx: Int, style: TeamGraphicStyle? = null) {
        val teamData = teamDataCreationService.singleFromLeague(leagueName, idx)
        val tcid = teamgraphicRepo.getChannelId(leagueName) ?: return
        val msgid = teamgraphicRepo.getMessageId(leagueName, idx) ?: return
        val style = style ?: getTeamGraphicStyleOfLeague(leagueName) ?: return
        channelInterface.editMessage(
            channelId = tcid,
            messageId = msgid,
            MessageEdit(
                content = teamData.users.joinToTeammates(),
                files = generator.generate(teamData, style).toFileUpload().into()
            )
        )
    }

    private suspend fun getTeamGraphicStyleOfLeague(leagueName: String): TeamGraphicStyle? {
        return leagueConfigRepo.getConfig(leagueName).teamgraphics?.style
    }
}