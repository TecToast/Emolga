package de.tectoast.emolga.database.league

import de.tectoast.emolga.league.DraftState
import de.tectoast.emolga.league.config.LeagueConfig
import de.tectoast.emolga.league.config.LeagueConfigOverride
import de.tectoast.emolga.league.config.ResettableLeagueData
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.referencesCascade
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.json.extract
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

object LeagueCoreTable : Table("league_core") {
    val leagueName = varchar("name", 255)
    val num = integer("num").default(0)
    val guild = long("guild")
    val teamSize = integer("team_size")
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

    suspend fun getDraftRelevantData(channelId: Long) = suspendTransaction(db) {
        with(LeagueCoreTable) {
            val draftState = draftData.extract<DraftState>(".draftState")
            select(leagueName, guild, teamSize, sheetId, afterTimerSkipMode, duringTimerSkipMode, draftOrder, isSwitchDraft, draftData)
                .forUpdate(ForUpdateOption.ForUpdate)
                .where { (draftChannel eq channelId) and (draftState neq DraftState.OFF) }
                .firstOrNull()
                ?.let {
                    DraftRelevantLeagueData(
                        leagueName = it[leagueName],
                        guild = it[guild],
                        teamSize = it[teamSize],
                        sheetId = it[sheetId],
                        afterTimerSkipMode = it[afterTimerSkipMode],
                        duringTimerSkipMode = it[duringTimerSkipMode],
                        draftOrder = it[draftOrder],
                        isSwitchDraft = it[isSwitchDraft],
                        draftData = it[draftData]
                    )
                }
        }
    }
}

data class DraftRelevantLeagueData(
    val leagueName: String,
    val guild: Long,
    val teamSize: Int,
    val sheetId: String,
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

object LeagueScheduleTable : Table("league_schedule") {
    val id = integer("id").autoIncrement()
    val leagueName = varchar("league_name", 255).referencesCascade(LeagueCoreTable.leagueName)
    val week = integer("week")
    val battleIndex = integer("battle_index")

    val p1 = integer("p1")
    val p2 = integer("p2")

    override val primaryKey = PrimaryKey(id)

    init {
        index(true, leagueName, week, battleIndex)
        index(false, leagueName, p1, p2)
    }
}



object GuildDefaultConfigTable : Table("guild_default_config") {
    val guildId = long("guild_id")
    val config = jsonb<LeagueConfigOverride>("config").nullable()

    override val primaryKey = PrimaryKey(guildId)
}

class LeagueConfigRepository(val db: R2dbcDatabase) {
    suspend fun getConfig(leagueName: String) = suspendTransaction(db) {
        val row = LeagueCoreTable.select(LeagueCoreTable.guild, LeagueCoreTable.configOverride).where {
            LeagueCoreTable.leagueName eq leagueName
        }.first()
        val guildId = row[LeagueCoreTable.guild]
        val leagueConfig = row[LeagueCoreTable.configOverride]
        val guildConfig = GuildDefaultConfigTable.select(GuildDefaultConfigTable.config).where {
            GuildDefaultConfigTable.guildId eq guildId
        }.firstOrNull()?.get(GuildDefaultConfigTable.config)
        LeagueConfig() + guildConfig + leagueConfig
    }
}