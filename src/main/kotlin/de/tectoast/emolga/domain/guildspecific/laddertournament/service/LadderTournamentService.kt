package de.tectoast.emolga.domain.guildspecific.laddertournament.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.sendMessage
import de.tectoast.emolga.domain.guildspecific.laddertournament.model.LadderTournamentUserData
import de.tectoast.emolga.domain.guildspecific.laddertournament.model.LadderUserResponse
import de.tectoast.emolga.domain.guildspecific.laddertournament.repository.LadderTournamentRepository
import de.tectoast.emolga.domain.guildspecific.laddertournament.service.provider.LadderDataProvider
import de.tectoast.emolga.domain.statestore.repository.StateStoreTable.uid
import de.tectoast.emolga.features.league.laddertournament.LadderTournamentApproveButton
import de.tectoast.emolga.utils.*
import de.tectoast.emolga.utils.sheetupdate.SpreadsheetService
import de.tectoast.generic.K18n_AlreadySignedUp
import de.tectoast.k18n.generated.K18N_DEFAULT_LANGUAGE
import dev.minn.jda.ktx.messages.MessageCreate
import dev.minn.jda.ktx.messages.into
import org.koin.core.annotation.Single
import kotlin.math.roundToInt

@Single
class LadderTournamentService(
    private val repository: LadderTournamentRepository,
    private val ladderDataProvider: LadderDataProvider,
    private val channelInterface: ChannelInterface,
    private val spreadsheetService: SpreadsheetService,
) {
    suspend fun executeForGuild(guild: Long) {
        val config = repository.getConfigByGuild(guild) ?: return
        val users = repository.getVerifiedUsersByGuild(guild)
        val usersPerFormat = users.flatMap { (uid, data) -> data.formats.map { it to uid } }.groupBy { it.first }
            .mapValues { it.value.map { v -> v.second } }
        val userData = fetchDataForUsers(users)
        spreadsheetService.updateSheet(config.sid, wait = false) {
            for ((format, targetRange) in config.formats) {
                val formatId = format.toShowdownID()
                val usersInFormat = usersPerFormat[format] ?: continue
                val tableData = usersInFormat.map { userData[it]!! }.sortedWith { a, b ->
                    val dataA = a.ratings[formatId] ?: return@sortedWith 1
                    val dataB = b.ratings[formatId] ?: return@sortedWith -1
                    for (sortCol in config.sortCols) {
                        val numA = sortCol[dataA].toDouble().roundToInt()
                        val numB = sortCol[dataB].toDouble().roundToInt()
                        if (numA != numB) return@sortedWith numB - numA
                    }
                    0
                }.map {
                    val rankData = it.ratings[formatId]
                    buildList {
                        add(it.username.removePrefix(config.sdNamePrefix))
                        config.cols.forEach { col ->
                            add(col[rankData].toDouble().roundToInt().toString())
                        }
                    }
                }
                addAll(targetRange, tableData)
            }
        }
    }

    private suspend fun fetchDataForUsers(users: Map<Long, LadderTournamentUserData>): Map<Long, LadderUserResponse> {
        return users.mapValues {
            ladderDataProvider.fetchDataForUser(it.value.sdName)
        }
    }

    suspend fun handleSignupRequest(
        guild: Long, userId: Long, sdName: String, formats: List<String>, btn: LadderTournamentApproveButton
    ): K18nMessageOrError {
        val lt = repository.getConfigByGuild(guild) ?: return "NoConfig".k18n.error()
        if (repository.isSignedUp(guild, userId)) return K18n_AlreadySignedUp.error()
        if (!sdName.startsWith(lt.sdNamePrefix)) return "Dein Showdown-Name muss mit `${lt.sdNamePrefix}` beginnen!".k18n.error()
        repository.signupUnverified(guild = guild, userId = userId, sdName = sdName, formats = formats)
        channelInterface.sendMessage(
            lt.adminChannel, MessageCreate(
                "Anmeldungsanfrage:\nUser: <@${uid}>\nSD-Name: `${sdName}`",
                components = btn.withoutIData(K18N_DEFAULT_LANGUAGE) {
                    this.user = userId
                }.into()
            )
        )
        return "Deine Anmeldung ist angekommen und wird nun vom Komitee verifiziert!".k18n.success()
    }

    suspend fun handleApproveRequest(guild: Long, userId: Long): CalcResult<Unit> {
        val lt = repository.getConfigByGuild(guild)
            ?: return "Auf diesem Server gibt es kein laufendes Ladder-Turnier!".k18n.error()
        val formats = repository.verify(guild, userId)
            ?: return "Dieser User hat keine Anmeldung zum Ladder-Turnier!".k18n.error()
        channelInterface.sendMessage(lt.signupChannel, "<@${userId}>: ${formats.joinToString()}")
        return Unit.success()
    }
}