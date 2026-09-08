package de.tectoast.emolga.domain.league.signup.repository


import de.tectoast.emolga.domain.league.signup.model.*
import de.tectoast.emolga.domain.league.signup.model.data.ParticipantDataSetData
import de.tectoast.emolga.utils.database.arrayAgg
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.referencesCascade
import de.tectoast.emolga.utils.suspendTransaction
import kotlinx.coroutines.flow.*
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.inTopLevelSuspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single


@Single
class SignupRepository(private val db: R2dbcDatabase) {

    suspend fun createNewSignup(guild: Long, identifier: String, config: LeagueSignupConfig, announceMessageId: Long) =
        suspendTransaction(
            db,
            SignupCoreTable
        ) {
            insertIgnore {
                it[this.guild] = guild
                it[this.identifier] = identifier
                it[this.config] = config
                it[this.announceMessageId] = announceMessageId
            }
        }

    suspend fun hasRunningSignup(guild: Long) = suspendTransaction(db) {
        SignupCoreTable.selectAll().where { SignupCoreTable.guild eq guild }.count() > 0L
    }

    suspend fun getConfig(guildId: Long, identifier: String) = suspendTransaction(db, SignupCoreTable) {
        select(config).where { (guild eq guildId) and (SignupCoreTable.identifier eq identifier) }.firstOrNull()
            ?.get(config)
    }

    suspend fun getConfig(signupId: Int) = suspendTransaction(db, SignupCoreTable) {
        select(config).where { SignupCoreTable.id eq signupId }.firstOrNull()?.get(config)
    }

    suspend fun getDirtySignups() = suspendTransaction(db) {
        val countExpr = wrapAsExpression<Long>(
            SignupEntryTable.select(SignupEntryTable.id.count())
                .where { SignupEntryTable.signupId eq SignupCoreTable.id })
        val countExprLabeled = countExpr.alias("user_count")
        SignupCoreTable
            .select(
                SignupCoreTable.id,
                SignupCoreTable.config,
                SignupCoreTable.guild,
                SignupCoreTable.announceMessageId,
                countExprLabeled
            )
            .where { SignupCoreTable.lastDocumentedEntryCount neq countExpr }
            .map {
                DirtySignup(
                    id = it[SignupCoreTable.id],
                    config = it[SignupCoreTable.config],
                    guild = it[SignupCoreTable.guild],
                    announceMessageId = it[SignupCoreTable.announceMessageId],
                    userCount = it[countExprLabeled] ?: 0
                )
            }.toList()
    }

    suspend fun setNewDocumentedCount(data: Map<Int, Long>) = suspendTransaction(db) {
        for ((signupId, documentedCount) in data) {
            SignupCoreTable.update({ SignupCoreTable.id eq signupId }) {
                it[SignupCoreTable.lastDocumentedEntryCount] = documentedCount
            }
        }
    }

    suspend fun editSignupEntry(entryId: Int, entry: SignupEntry) = suspendTransaction(db) {
        SignupEntryTable.update({ SignupEntryTable.id eq entryId }) {
            it[SignupEntryTable.data] = entry.data
            it[SignupEntryTable.signupMessageId] = entry.signupMessageId
            it[SignupEntryTable.logoMessageId] = entry.logoMessageId
            it[SignupEntryTable.logoIdentifier] = entry.logoIdentifier
            it[SignupEntryTable.conference] = entry.conference
        }
        editSignupEntryUsers(entryId, entry.users)
    }

    suspend fun editSignupEntryUsers(entryId: Int, users: Set<Long>) = suspendTransaction(db) {
        SignupUserTable.deleteWhere { SignupUserTable.entryId eq entryId }
        SignupUserTable.batchInsert(users) { userId ->
            this[SignupUserTable.entryId] = entryId
            this[SignupUserTable.userId] = userId
        }
    }

    suspend fun saveNewSignupEntry(signupId: Int, entry: SignupEntry): Int = suspendTransaction(db) {
        val entryId = SignupEntryTable.insertReturning {
            it[SignupEntryTable.signupId] = signupId
            it[SignupEntryTable.data] = entry.data
            it[SignupEntryTable.signupMessageId] = entry.signupMessageId
            it[SignupEntryTable.logoMessageId] = entry.logoMessageId
            it[SignupEntryTable.logoIdentifier] = entry.logoIdentifier
            it[SignupEntryTable.conference] = entry.conference
        }.first()[SignupEntryTable.id]
        SignupUserTable.deleteWhere { SignupUserTable.entryId eq entryId }
        SignupUserTable.batchInsert(entry.users) { userId ->
            this[SignupUserTable.entryId] = entryId
            this[SignupUserTable.userId] = userId
        }
        entryId
    }

    suspend fun getSignupEntryById(entryId: Int) =
        suspendTransaction(db, SignupEntryTable) {
            selectAll().where { id eq entryId }.firstOrNull()?.entryRowToSignupEntry()?.second
        }

    suspend fun getSignupEntryByUserId(signupId: Int, userId: Long) =
        suspendTransaction(db) {
            SignupUserTable.innerJoin(SignupEntryTable, { entryId }, { id }).selectAll()
                .where { (SignupEntryTable.signupId eq signupId) and (SignupUserTable.userId eq userId) }.firstOrNull()
                ?.entryRowToSignupEntry()
        }

    private suspend fun ResultRow.entryRowToSignupEntry(alreadyFetchedUsers: ExpressionWithColumnType<List<Long>>? = null): Pair<Int, SignupEntry> {
        val entryId = this[SignupEntryTable.id]
        val users =
            alreadyFetchedUsers?.let { this[it].toMutableSet() } ?: inTopLevelSuspendTransaction(db) {
                SignupUserTable.select(SignupUserTable.userId)
                    .where { SignupUserTable.entryId eq entryId }
                    .orderBy(SignupUserTable.userId)
                    .map { row -> row[SignupUserTable.userId] }.toCollection(mutableSetOf())
            }
        return entryId to SignupEntry(
            users,
            this[SignupEntryTable.data].toMutableMap(),
            this[SignupEntryTable.signupMessageId],
            this[SignupEntryTable.logoMessageId],
            this[SignupEntryTable.logoIdentifier],
            this[SignupEntryTable.conference]
        )
    }

    suspend fun getSignupEntryIdsOfUsers(signupId: Int, users: Iterable<Long>) = suspendTransaction(db) {
        SignupUserTable.innerJoin(SignupEntryTable, { entryId }, { id }).select(
            SignupUserTable.entryId, SignupUserTable.userId
        ).where { (SignupEntryTable.signupId eq signupId) and (SignupUserTable.userId inList users) }
            .associate { it[SignupUserTable.userId] to it[SignupUserTable.entryId] }
    }


    suspend fun getCurrentSignupCount(signupId: Int) = suspendTransaction(db) {
        SignupEntryTable.selectAll().where { SignupEntryTable.signupId eq signupId }.count()
    }

    suspend fun exists(guild: Long, identifier: String) = suspendTransaction(db) {
        SignupCoreTable.selectAll()
            .where { (SignupCoreTable.guild eq guild) and (SignupCoreTable.identifier eq identifier) }.count() > 0L
    }

    suspend fun getLeagueSignup(guild: Long, identifier: String, locking: Boolean = false) = getLeagueSignup(
        { (SignupCoreTable.guild eq guild) and (SignupCoreTable.identifier eq identifier) },
        locking
    )

    suspend fun getLeagueSignup(signupId: Int, locking: Boolean = false) =
        getLeagueSignup({ SignupCoreTable.id eq signupId }, locking)

    suspend fun getLeagueSignupOfUser(guild: Long, user: Long, locking: Boolean = false) =
        getLeagueSignupFromQuery(locking) {
            SignupUserTable.innerJoin(SignupEntryTable, { entryId }, { id })
                .innerJoin(SignupCoreTable, { SignupEntryTable.signupId }, { id })
                .selectAll()
                .where { (SignupCoreTable.guild eq guild) and (SignupUserTable.userId eq user) }
        }

    private suspend fun getLeagueSignup(predicate: () -> Op<Boolean>, locking: Boolean) =
        getLeagueSignupFromQuery(locking) { SignupCoreTable.selectAll().where(predicate) }

    private suspend inline fun getLeagueSignupFromQuery(locking: Boolean, crossinline queryProvider: () -> Query) =
        suspendTransaction(db) {
            queryProvider()
                .apply {
                    if (locking) forUpdate()
                }
                .map {
                    LeagueSignup(
                        id = it[SignupCoreTable.id],
                        guild = it[SignupCoreTable.guild],
                        identifier = it[SignupCoreTable.identifier],
                        config = it[SignupCoreTable.config],
                        announceMessageId = it[SignupCoreTable.announceMessageId],
                        conferences = it[SignupCoreTable.conferences],
                        conferenceRoleIds = it[SignupCoreTable.conferenceRoleIds]
                    )
                }.firstOrNull()
        }

    suspend fun removeUser(guild: Long, userId: Long): SignupRemoveUserResult = suspendTransaction(db) {
        val resultRow = SignupUserTable.innerJoin(SignupEntryTable, { entryId }, { id })
            .innerJoin(SignupCoreTable, { SignupEntryTable.signupId }, { id })
            .select(SignupUserTable.id, SignupUserTable.entryId, SignupCoreTable.id)
            .where { (SignupCoreTable.guild eq guild) and (SignupUserTable.userId eq userId) }.firstOrNull()
            ?: return@suspendTransaction SignupRemoveUserResult.NotFound
        val entryId = resultRow[SignupUserTable.entryId]
        val userIdentifier = resultRow[SignupUserTable.id]
        val signupId = resultRow[SignupCoreTable.id]
        SignupUserTable.deleteWhere { SignupUserTable.id eq userIdentifier }
        val entry = getSignupEntryById(entryId) ?: return@suspendTransaction SignupRemoveUserResult.NotFound
        val remainingUserCount = SignupUserTable.selectAll().where { SignupUserTable.entryId eq entryId }.count()
        if (remainingUserCount == 0L) {
            SignupEntryTable.deleteWhere { SignupEntryTable.id eq entryId }
            SignupRemoveUserResult.Removed(signupId, entry, deletedEntry = true)
        } else {
            SignupRemoveUserResult.Removed(signupId, entry, deletedEntry = false)
        }
    }

    suspend fun getAllEntries(signupId: Int) = suspendTransaction(db) {
        val users =
            SignupUserTable.userId.arrayAgg(SignupUserTable.userId, columnType = LongColumnType()).alias("users")
        val aggregated =
            SignupUserTable.select(SignupUserTable.entryId, users).groupBy(SignupUserTable.entryId).alias("aggregated")
        SignupEntryTable.innerJoin(aggregated, { id }, { aggregated[SignupUserTable.entryId] }).selectAll()
            .orderBy(SignupEntryTable.order to SortOrder.ASC, SignupEntryTable.id to SortOrder.ASC)
            .where { SignupEntryTable.signupId eq signupId }.associate {
                it.entryRowToSignupEntry(
                    aggregated[users]
                )
            }
    }

    suspend fun setConferencesForSignup(signupId: Int, conferences: List<String>) = suspendTransaction(db) {
        SignupCoreTable.update({ SignupCoreTable.id eq signupId }) {
            it[SignupCoreTable.conferences] = conferences
        }
    }

    suspend fun setConferencesForEntries(signupId: Int, data: Map<Int, ParticipantDataSetData>) =
        suspendTransaction(db) {
            for ((entryId, singleData) in data) {
                SignupEntryTable.update({ (SignupEntryTable.signupId eq signupId) and (SignupEntryTable.id eq entryId) }) {
                    it[SignupEntryTable.conference] = singleData.conf
                    it[SignupEntryTable.order] = singleData.order
                }
            }
        }

    suspend fun getSignupEntries(signupId: Int, conference: String?) = suspendTransaction(db) {
        SignupEntryTable.selectAll()
            .where { (SignupEntryTable.signupId eq signupId) and (SignupEntryTable.conference eq conference) }
            .orderBy(SignupEntryTable.order)
            .map {
                it.entryRowToSignupEntry().second
            }.toList()
    }

    suspend fun getAllGuilds() = suspendTransaction(db) {
        SignupCoreTable.selectAll().map { it[SignupCoreTable.guild] }.toSet()
    }


}


object SignupCoreTable : Table("signup_core") {
    val id = integer("id").autoIncrement()
    val guild = long("guild_id")
    val identifier = text("identifier").default("")
    val config = jsonb<LeagueSignupConfig>("config")
    val announceMessageId = long("announce_message_id")
    val conferences = array<String>("conferences").default(emptyList())
    val conferenceRoleIds = jsonb<Map<String, Long>>("conference_role_ids").default(emptyMap())
    val lastDocumentedEntryCount = long("last_documented_entry_count").default(0)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(guild, identifier)
    }
}

object SignupEntryTable : Table("signup_entry") {
    val id = integer("id").autoIncrement()
    val signupId = integer("signup_id").referencesCascade(SignupCoreTable.id)
    val data = jsonb<Map<String, String>>("data")
    val signupMessageId = long("signup_message_id").nullable()
    val logoMessageId = long("logo_message_id").nullable()
    val logoIdentifier = text("logo_identifier").nullable()
    val conference = text("conference").nullable()
    val order = integer("order").default(0)

    override val primaryKey = PrimaryKey(id)
}

object SignupUserTable : Table("signup_user") {
    val id = integer("id").autoIncrement()
    val entryId = integer("entry_id").referencesCascade(SignupEntryTable.id)
    val userId = long("user_id")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(entryId, userId)
    }
}

