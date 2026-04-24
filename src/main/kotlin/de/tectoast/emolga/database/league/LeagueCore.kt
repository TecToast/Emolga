package de.tectoast.emolga.database.league

import de.tectoast.emolga.league.DraftState
import de.tectoast.emolga.league.config.LeagueConfigOverride
import de.tectoast.emolga.league.config.ResettableLeagueData
import de.tectoast.emolga.utils.jsonb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.json.extract
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update

object LeagueCoreTable : Table("league_core") {
    val leagueName = varchar("name", 255)
    val num = integer("num").default(0)
    val guild = long("guild")
    val prettyName = varchar("pretty_name", 255).nullable()
    val sheetId = varchar("sheet_id", 255).default("")
    val afterTimerSkipMode = jsonb<AfterTimerSkipMode>("after_timer_skip_mode").default(AfterTimerSkipMode.AfterDraftUnordered)
    val duringTimerSkipMode = jsonb<DuringTimerSkipMode>("during_timer_skip_mode").nullable()
    val draftOrder = jsonb<Map<Int, List<Int>>>("draft_order").default(emptyMap())
    val draftChannel = long("draft_channel").nullable()
    val isSwitchDraft = bool("is_switch_draft").default(false)
    val configOverride = jsonb<LeagueConfigOverride>("config_override").nullable()
    val draftData = jsonb<ResettableLeagueData>("draft_data").default(ResettableLeagueData())
    val resultChannel = long("result_channel").nullable()



    override val primaryKey = PrimaryKey(leagueName)

    init {
        index(false, guild)
    }
}

class LeagueCoreRepository(val db: R2dbcDatabase) {
    suspend fun getDraftRelevantData(channelId: Long) = getDraftRelevantData { LeagueCoreTable.draftChannel eq channelId }
    suspend fun getDraftRelevantData(leagueName: String) = getDraftRelevantData { LeagueCoreTable.leagueName eq leagueName }

    suspend fun getDraftStateLocking(leagueName: String) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.draftData).forUpdate().where { LeagueCoreTable.leagueName eq leagueName }.first()[LeagueCoreTable.draftData]
    }

    suspend fun getLeagueNamesByGuild(guildId: Long) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.leagueName).where { LeagueCoreTable.guild eq guildId }.map { it[LeagueCoreTable.leagueName] }.toList()
    }

    private suspend fun getDraftRelevantData(check: () -> Op<Boolean>) = suspendTransaction(db) {
        with(LeagueCoreTable) {
            val draftState = draftData.extract<DraftState>(".draftState")
            select(leagueName, prettyName, guild, sheetId, draftChannel, afterTimerSkipMode, duringTimerSkipMode, draftOrder, isSwitchDraft, draftData)
                .forUpdate()
                .where { check() and (draftState neq DraftState.OFF) }
                .firstOrNull()
                ?.let {
                    DraftRelevantLeagueData(
                        leagueName = it[leagueName],
                        displayName = it[prettyName] ?: it[leagueName],
                        guild = it[guild],
                        sheetId = it[sheetId],
                        draftChannel = it[draftChannel]!!,
                        afterTimerSkipMode = it[afterTimerSkipMode],
                        duringTimerSkipMode = it[duringTimerSkipMode],
                        draftOrder = it[draftOrder],
                        isSwitchDraft = it[isSwitchDraft],
                        draftData = it[draftData]
                    )
                }
        }
    }

    suspend fun updateDraftData(leagueName: String, draftData: ResettableLeagueData) = suspendTransaction(db) {
        LeagueCoreTable.update({ LeagueCoreTable.leagueName eq leagueName }) {
            it[LeagueCoreTable.draftData] = draftData
        }
    }
}

data class DraftRelevantLeagueData(
    val leagueName: String,
    val displayName: String,
    val guild: Long,
    val sheetId: String,
    val draftChannel: Long,
    val afterTimerSkipMode: AfterTimerSkipMode,
    val duringTimerSkipMode: DuringTimerSkipMode?,
    val draftOrder: Map<Int, List<Int>>,
    val isSwitchDraft: Boolean,
    val draftData: ResettableLeagueData
) {
    val pseudoEnd get() = draftData.draftState == DraftState.PSEUDOEND
    val round get() = draftData.round
    val totalRounds get() = draftOrder.size
    val isLastRound get() = round == totalRounds
    val draftWouldEnd get() = isLastRound && indexInRound == draftOrder[round]!!.lastIndex
    val indexInRound get() = draftData.indexInRound
    val currentIdx get() = draftOrder[round]!![indexInRound]
    val potentialBetweenPick get() = !pseudoEnd && duringTimerSkipMode == DuringTimerSkipMode.Always
    val alreadyBannedMonsThisRound get() = draftData.draftBan.bannedMons[round].orEmpty()

    fun hasMovedTurns(idx: Int) = movedTurns(idx).isNotEmpty()
    fun movedTurns(idx: Int) = draftData.moved[idx] ?: mutableListOf()
    fun addToMoved(idx: Int) {
        if (!isSwitchDraft) draftData.moved.getOrPut(idx) { mutableListOf() }.let { if (round !in it) it += round }
    }
}


