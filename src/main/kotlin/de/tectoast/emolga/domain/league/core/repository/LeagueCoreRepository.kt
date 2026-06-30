package de.tectoast.emolga.domain.league.core.repository

import de.tectoast.emolga.domain.league.config.model.LeagueConfigOverride
import de.tectoast.emolga.domain.league.core.model.*
import de.tectoast.emolga.domain.league.draft.model.timer.TimerSkipMode
import de.tectoast.emolga.domain.league.member.repository.LeagueUserTable
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.referencesCascade
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.json.extract
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import org.koin.core.annotation.Single


@Single
class LeagueCoreRepository(private val db: R2dbcDatabase) {

    suspend fun getLeagueNameByPrefix(prefix: String) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.leagueName).where { LeagueCoreTable.leagueName like "$prefix%" }
            .map { it[LeagueCoreTable.leagueName] }.toList()
    }

    suspend fun getLeagueNameByDisplayName(guild: Long, displayName: String) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.leagueName)
            .where { LeagueCoreTable.guild eq guild and (LeagueCoreTable.prettyName eq displayName or (LeagueCoreTable.leagueName eq displayName)) }
            .map { it[LeagueCoreTable.leagueName] }.firstOrNull()
    }

    suspend fun getLeagueNameAndIdxByGuildUser(guild: Long, user: Long) = suspendTransaction(db) {
        LeagueCoreTable.innerJoin(LeagueUserTable, { leagueName }, { leagueName }).select(
            LeagueCoreTable.leagueName,
            LeagueUserTable.idx
        )
            .where { (LeagueCoreTable.guild eq guild) and (LeagueUserTable.userId eq user) }
            .map { it[LeagueCoreTable.leagueName] to it[LeagueUserTable.idx] }.firstOrNull()
    }

    suspend fun getGuildOfDraftChannel(channelId: Long) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.guild).where { LeagueCoreTable.draftChannel eq channelId }.firstOrNull()
            ?.get(LeagueCoreTable.guild)
    }

    suspend fun getDraftRelevantData(channelId: Long, locking: Boolean = true) =
        getDraftRelevantData(locking = locking) { LeagueCoreTable.draftChannel eq channelId }.firstOrNull()

    suspend fun getDraftRelevantData(leagueName: String, locking: Boolean = true) =
        getDraftRelevantData(locking = locking) { LeagueCoreTable.leagueName eq leagueName }.firstOrNull()

    suspend fun getDraftStateLocking(leagueName: String) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.draftData).forUpdate().where { LeagueCoreTable.leagueName eq leagueName }
            .first()[LeagueCoreTable.draftData]
    }

    suspend fun getLeagueNamesByGuild(guildId: Long) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.leagueName).where { LeagueCoreTable.guild eq guildId }
            .map { it[LeagueCoreTable.leagueName] }.toList()
    }

    suspend fun getAllScalarLeagueData(guild: Long) = getScalarLeagueData { LeagueCoreTable.guild eq guild }

    suspend fun getScalarLeagueData(leagueName: String) =
        getScalarLeagueData { LeagueCoreTable.leagueName eq leagueName }.first()

    suspend fun getScalarLeagueDataOrNull(leagueName: String) =
        getScalarLeagueData { LeagueCoreTable.leagueName eq leagueName }.firstOrNull()

    private suspend fun getScalarLeagueData(check: () -> Op<Boolean>) = suspendTransaction(db, LeagueCoreTable) {
        select(
            leagueName,
            prettyName,
            num,
            guild,
            sheetId,
            draftChannel
        ).where(check).orderBy(num).map {
            ScalarLeagueData(
                leagueName = it[leagueName],
                prettyName = it[prettyName],
                num = it[num],
                guild = it[guild],
                sheetId = it[sheetId],
                draftChannel = it[draftChannel]
            )
        }.toList()
    }

    suspend fun getLeagueFromDraftChannelOrUser(channel: Long, guild: Long, user: Long) = suspendTransaction(db) {
        LeagueCoreTable.innerJoin(LeagueUserTable, { leagueName }, { leagueName }).select(
            LeagueCoreTable.leagueName,
            LeagueUserTable.idx
        )
            .where {
                (LeagueCoreTable.draftChannel eq channel) or
                        ((LeagueUserTable.userId eq user) and (LeagueCoreTable.guild eq guild))
            }.orderBy(LeagueCoreTable.draftChannel eq channel, SortOrder.DESC)
            .map { it[LeagueCoreTable.leagueName] to it[LeagueUserTable.idx] }.firstOrNull()
    }

    suspend fun getLeagueWithParticipants(guild: Long, user: Long) =
        getLeagueWithParticipants { (LeagueCoreTable.guild eq guild) and (LeagueUserTable.userId eq user) }

    suspend fun getLeagueWithParticipants(leagueName: String) =
        getLeagueWithParticipants { LeagueCoreTable.leagueName eq leagueName }

    private suspend fun getLeagueWithParticipants(check: () -> Op<Boolean>) = suspendTransaction(db) {
        val (leagueName, guild) = LeagueCoreTable.innerJoin(LeagueUserTable, { leagueName }, { leagueName }).select(
            LeagueCoreTable.leagueName, LeagueCoreTable.guild
        )
            .where(check).map {
                it[LeagueCoreTable.leagueName] to it[LeagueCoreTable.guild]
            }.firstOrNull() ?: return@suspendTransaction null
        val users = LeagueUserTable.select(LeagueUserTable.userId)
            .where { (LeagueUserTable.leagueName eq leagueName) and (LeagueUserTable.userOrder eq 0) }
            .orderBy(LeagueUserTable.idx)
            .map { it[LeagueUserTable.userId] }.toList()
        LeagueWithParticipants(leagueName, guild, users)
    }

    private suspend fun getDraftRelevantData(locking: Boolean, check: (() -> Op<Boolean>)?) = suspendTransaction(db) {
        with(LeagueCoreTable) {
            select(
                leagueName,
                prettyName,
                guild,
                sheetId,
                draftChannel,
                afterTimerSkipMode,
                duringTimerSkipMode,
                draftOrder,
                isSwitchDraft,
                draftData
            ).apply {
                if (locking) forUpdate()
            }
                .where { isRunningCondition }
                .apply {
                    check?.let { andWhere(it) }
                }
                .map {
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
                .toList()
        }
    }

    suspend fun updateDraftData(leagueName: String, draftData: ResettableLeagueData) = suspendTransaction(db) {
        LeagueCoreTable.update({ LeagueCoreTable.leagueName eq leagueName }) {
            it[LeagueCoreTable.draftData] = draftData
        }
    }

    suspend fun setDraftStartData(leagueName: String, tcId: Long, isSwitchDraft: Boolean) = suspendTransaction(db) {
        LeagueCoreTable.update({ LeagueCoreTable.leagueName eq leagueName }) {
            it[LeagueCoreTable.draftChannel] = tcId
            it[LeagueCoreTable.isSwitchDraft] = isSwitchDraft
            it[LeagueCoreTable.draftData] = ResettableLeagueData()
        }
    }

    suspend fun getAllRunningDraftLeagueData() = getDraftRelevantData(check = null, locking = true)
    suspend fun getAllLeagueNames() = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.leagueName).map { it[LeagueCoreTable.leagueName] }.toSet()
    }

    suspend fun getLeagueDisplayNames(guild: Long) = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.prettyName, LeagueCoreTable.leagueName)
            .where { LeagueCoreTable.guild eq guild }
            .orderBy(LeagueCoreTable.num to SortOrder.ASC, LeagueCoreTable.leagueName to SortOrder.ASC)
            .map { it[LeagueCoreTable.prettyName] ?: it[LeagueCoreTable.leagueName] }.toList()
    }

    suspend fun getAllLeagueGuilds() = suspendTransaction(db) {
        LeagueCoreTable.select(LeagueCoreTable.guild).map { it[LeagueCoreTable.guild] }.toSet()
    }

    private val isRunningCondition by lazy { LeagueCoreTable.draftData.extract<String>("draftState") neq DraftState.OFF.name }
}

object LeagueCoreTable : Table("league_core") {
    val leagueName = text("name")
    val num = integer("num").default(0)
    val guild = long("guild")
    val prettyName = text("pretty_name").nullable()
    val sheetId = text("sheet_id").default("")
    val afterTimerSkipMode =
        jsonb<TimerSkipMode.After>("after_timer_skip_mode").default(TimerSkipMode.After.AfterDraftUnordered)
    val duringTimerSkipMode = jsonb<TimerSkipMode.During>("during_timer_skip_mode").nullable()
    val draftOrder = jsonb<Map<Int, List<Int>>>("draft_order").default(emptyMap())
    val draftChannel = long("draft_channel").nullable()
    val isSwitchDraft = bool("is_switch_draft").default(false)
    val configOverride = jsonb<LeagueConfigOverride>("config_override").nullable()
    val draftData = jsonb<ResettableLeagueData>("draft_data").default(ResettableLeagueData())


    override val primaryKey = PrimaryKey(leagueName)

    init {
        index(false, guild)
    }
}

context(t: Table)
fun <C : Column<String>> C.referencesLeagueName(): C = referencesCascade(LeagueCoreTable.leagueName)