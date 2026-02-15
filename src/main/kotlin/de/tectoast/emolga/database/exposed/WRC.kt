package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.features.wrc.WRCSignupButton
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.universalLogger
import dev.minn.jda.ktx.messages.Embed
import kotlinx.coroutines.flow.*
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
object WRCDataDB : Table("wrc_running") {
    val WRCNAME = varchar("wrcname", 100)
    val WARRIORROLE = long("warriorrole")
    val GUILD = long("guild")
    val TLIDENTIFIER = varchar("tlidentifier", 100).nullable()
    val SID = varchar("sid", 100)
    val SIGNUPCHANNEL = long("signupchannel")
    val LASTSIGNUP = timestamp("lastsignup")
    val SIGNUPDURATIONMINS = integer("signupdurationmins")
    val INTERVALMINS = integer("intervalmins")
    val GAMEDAYS = integer("gamedays")
    val MATCHUPCHANNEL = long("matchupchannel")
    val TEAMSUBMITMINS = integer("teamsubmitmins")
    val FOLLOWUPMINS = integer("followupmins")

    suspend fun getByName(name: String) = dbTransaction {
        selectAll().where { WRCNAME eq name }.firstOrNull()
    }

    suspend fun getTierlistOfWrcName(wrcName: String) = getByName(wrcName)?.let { wrc ->
        Tierlist[wrc[GUILD], wrc[TLIDENTIFIER]] ?: universalLogger.warn(
            "No tierlist found for wrc {} {}", wrcName, wrc[TLIDENTIFIER]
        ).let { null }
    }

    override val primaryKey = PrimaryKey(WRCNAME)
}

object WRCUserSignupDB : Table("wrc_usersignup") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val USERID = long("userid")
    val WARRIOR = bool("warrior")
    val REGISTERED = bool("registered").default(false)
    val SLEPT = bool("slept").default(false)
    val PRIORITIZED = bool("prioritized").default(false)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, USERID)

    suspend fun unsignupUser(wrcname: String, gameday: Int, userid: Long) = dbTransaction {
        deleteWhere { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (USERID eq userid) } > 0
    }

    suspend fun signupUser(name: String, gameday: Int, uid: Long, warrior: Boolean) = dbTransaction {
        insert {
            it[WRCNAME] = name
            it[GAMEDAY] = gameday
            it[USERID] = uid
            it[WARRIOR] = warrior
        }
    }

    suspend fun buildSignupEmbed(wrcname: String, gameday: Int) = dbTransaction {
        val (warriors, challengers) = getAllSignupsForGameday(wrcname, gameday)
        val allRegisteredUsers = getAllRegisteredUsers(wrcname)
        Embed {
            color = embedColor
            title = "$wrcname Anmeldung Spieltag $gameday"
            field("Warriors", warriors.toEmbedFieldValue(allRegisteredUsers))
            field("Challengers", challengers.toEmbedFieldValue(allRegisteredUsers))
        }
    }

    suspend fun getAllSignupsForGameday(
        wrcname: String, gameday: Int
    ): Pair<List<Long>, List<Long>> = dbTransaction {
        selectAll().where { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) }.map { it[USERID] to it[WARRIOR] }
            .partition { it.second }.let { it.first.map { i -> i.first } to it.second.map { i -> i.first } }
    }

    suspend fun getAllRegisteredUsers(wrcname: String): Set<Long> = dbTransaction {
        select(USERID).withDistinct().where { (WRCNAME eq wrcname) and (REGISTERED eq true) }.map { it[USERID] }.toSet()
    }

    suspend fun markSlept(wrcname: String, gameday: Int, userid: Long) = dbTransaction {
        update({ (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (USERID eq userid) }) {
            it[SLEPT] = true
            it[REGISTERED] = false
        }
    }

    suspend fun markPrioritized(wrcname: String, gameday: Int, userid: Long) = dbTransaction {
        update({ (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (USERID eq userid) }) {
            it[PRIORITIZED] = true
        }
    }

    suspend fun findNewContenderAfterSlept(
        wrcname: String,
        gameday: Int,
        warrior: Boolean,
        usersToIgnore: Collection<Long?> = emptyList()
    ) = dbTransaction {
        select(USERID).where { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (WARRIOR eq warrior) and (REGISTERED eq false) and (SLEPT eq false) }
            .apply {
                usersToIgnore.filterNotNull().forEach {
                    andWhere {
                        USERID neq it
                    }
                }
            }
            .orderBy(Random())
            .map { it[USERID] }.firstOrNull()
    }

    fun buildSignupButton(wrcName: String, gameday: Int, disabled: Boolean) = WRCSignupButton.withoutIData(
        label = (if (disabled) "Anmeldung geschlossen" else "An/abmelden").k18n,
        emoji = Emoji.fromUnicode("✅").takeUnless { disabled },
        disabled = disabled
    ) {
        this.wrcname = wrcName
        this.gameday = gameday
    }

    private fun List<Long>.toEmbedFieldValue(allRegisterdUsers: Set<Long>): String {
        val (new, alreadyRegistered) = partition { it !in allRegisterdUsers }
        return buildString {
            append(new.joinToString("\n") { "<@${it}>" })
            if (alreadyRegistered.isNotEmpty()) {
                appendLine()
                append("**Bereits teilgenommen:**")
                appendLine()
                append(alreadyRegistered.joinToString("\n") { "<@${it}>" })
            }
        }.ifEmpty { "_keine_" }
    }

}

object WRCSignupMessageDB : Table("wrc_signupmessage") {

    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val MESSAGEID = long("messageid")

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }

    suspend fun setMessageIdForGameday(wrcName: String, gameday: Int, mid: Long) = dbTransaction {
        upsert {
            it[WRCNAME] = wrcName
            it[GAMEDAY] = gameday
            it[MESSAGEID] = mid
        }
    }

    suspend fun getAndDeleteMessageIdForGameday(wrcName: String, gameday: Int) = dbTransaction {
        val result =
            select(MESSAGEID).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) }.firstOrNull()?.get(MESSAGEID)
        deleteWhere { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) }
        result
    }

}

object WRCTeraDB : Table("wrc_teramessage") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val USER = long("user")
    val MESSAGEID = long("messageid")
    val TERA = varchar("tera", 100).nullable()

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, USER)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }

    suspend fun setMessageIdForUser(wrcName: String, gameday: Int, user: Long, mid: Long) = dbTransaction {
        upsert {
            it[WRCNAME] = wrcName
            it[GAMEDAY] = gameday
            it[USER] = user
            it[MESSAGEID] = mid
        }
    }

    suspend fun setTeraForUser(wrcName: String, gameday: Int, user: Long, tera: String) = dbTransaction {
        update({ (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (USER eq user) }) {
            it[TERA] = tera
        }
    }

    suspend fun deleteDataForUser(wrcName: String, gameday: Int, user: Long) = dbTransaction {
        deleteWhere { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (USER eq user) }
    }

    suspend fun getDataForUser(wrcName: String, gameday: Int, user: Long) = dbTransaction {
        select(MESSAGEID, TERA).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (USER eq user) }.firstOrNull()
            ?.let { it[MESSAGEID] to it[TERA] }
    }

}

object WRCMatchupsDB : Table("wrc_matchups") {

    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val BATTLEINDEX = integer("battleindex")
    val USERINDEX = integer("userindex")
    val USER = long("user")
    val SUBMIT = bool("submit").default(false)
    val INACTIVE = bool("inactive").default(false)

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, BATTLEINDEX, USERINDEX)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }

    private fun ResultRow.toWRCUserBattleData() = WRCUserBattleData(
        user = this[USER],
        battleIndex = this[BATTLEINDEX],
        userIndex = this[USERINDEX]
    )

    suspend fun getSubmitDataForGameday(wrcName: String, gameday: Int) = dbTransaction {
        select(USER, BATTLEINDEX, USERINDEX, SUBMIT).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) }
            .orderBy(BATTLEINDEX to SortOrder.ASC, USERINDEX to SortOrder.ASC)
            .map { WRCUserSubmitData(userData = it.toWRCUserBattleData(), submitted = it[SUBMIT]) }
            .toList()
    }

    suspend fun findChallengersWithoutSubmit(wrcName: String, gameday: Int) = dbTransaction {
        select(
            USER,
            BATTLEINDEX,
            USERINDEX
        ).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and ((BATTLEINDEX eq 1) or (USERINDEX eq 1)) and (SUBMIT eq false) }
            .map { it.toWRCUserBattleData() }
    }

    suspend fun findWarriorWithoutSubmit(wrcName: String, gameday: Int) = dbTransaction {
        select(
            USER,
            BATTLEINDEX,
            USERINDEX
        ).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and ((BATTLEINDEX eq 0) and (USERINDEX eq 0)) and (SUBMIT eq false) }
            .map { it.toWRCUserBattleData() }.firstOrNull()
    }

    suspend fun changeUserInMatchup(
        wrcName: String, gameday: Int, battleIndex: Int, userIndex: Int, newUser: Long
    ) = dbTransaction {
        update({ (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (BATTLEINDEX eq battleIndex) and (USERINDEX eq userIndex) }) {
            it[USER] = newUser
            it[SUBMIT] = false
        }
    }

    suspend fun getOpponentOfUser(
        wrcName: String, gameday: Int, user: Long
    ) = dbTransaction {
        val matchup =
            select(BATTLEINDEX, USERINDEX).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (USER eq user) }
                .firstOrNull() ?: return@dbTransaction null
        val (battleIndex, userIndex) = matchup.let { it[BATTLEINDEX] to it[USERINDEX] }
        select(USER).where {
            (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (BATTLEINDEX eq battleIndex) and (USERINDEX neq userIndex)
        }.firstOrNull()?.get(USER)
    }

    suspend fun insertMatchup(wrcName: String, gameday: Int, index: Int, u1: Long, u2: Long) = dbTransaction {
        batchInsert(listOf(u1, u2).withIndex(), shouldReturnGeneratedValues = false) { (userIndex, user) ->
            this[WRCNAME] = wrcName
            this[GAMEDAY] = gameday
            this[BATTLEINDEX] = index
            this[USERINDEX] = userIndex
            this[USER] = user
        }
    }

    suspend fun hasSubmittedTeam(wrcname: String, gameday: Int, user: Long) = dbTransaction {
        selectAll().where { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (((USER eq user) and (SUBMIT eq true))) }
            .count() > 0
    }

    suspend fun getUsersIfSubmitted(wrcName: String, gameday: Int, user: Long) = dbTransaction {
        select(
            BATTLEINDEX,
            USERINDEX,
            USER,
        ).where { (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (USER eq user) and (SUBMIT eq true) }.toList()
            .takeIf { it.size == 2 }?.let { it.map { row -> row[USER] } to it.first()[BATTLEINDEX] }
    }

    suspend fun markSubmitted(wrcname: String, gameday: Int, user: Long) = dbTransaction {
        update({ (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (USER eq user) }) {
            it[SUBMIT] = true
        }
    }

    suspend fun markInactive(wrcName: String, gameday: Int, battleIndex: Int) = dbTransaction {
        update({ (WRCNAME eq wrcName) and (GAMEDAY eq gameday) and (BATTLEINDEX eq battleIndex) }) {
            it[INACTIVE] = true
        }
    }
}

data class WRCUserBattleData(val user: Long, val battleIndex: Int, val userIndex: Int) {
    fun isWarrior() = battleIndex == 0 && userIndex == 0
}

data class WRCUserSubmitData(val userData: WRCUserBattleData, val submitted: Boolean) {
    fun isNonSubmittedWarrior() = !submitted && userData.isWarrior()
    fun isNonSubmittedChallenger() = !submitted && !userData.isWarrior()
}

object WRCMonsOptionsDB : Table("wrc_monsoptions") {
    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val MON = varchar("mon", 100)

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, MON)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }
}

object WRCMonsPickedDB : Table("wrc_monspicked") {

    val WRCNAME = varchar("wrcname", 100)
    val GAMEDAY = integer("gameday")
    val USERID = long("userid")
    val MON = varchar("mon", 100)
    val TIER = varchar("tier", 10)

    override val primaryKey = PrimaryKey(WRCNAME, GAMEDAY, USERID, MON)

    init {
        foreignKey(WRCNAME to WRCDataDB.WRCNAME, onDelete = CASCADE, onUpdate = CASCADE)
    }

    suspend fun setPickedMonsForTier(
        wrcname: String, gameday: Int, userId: Long, tier: String, selectedMons: List<String>
    ) = dbTransaction {
        deleteWhere { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (USERID eq userId) and (MON notInList selectedMons) and (TIER eq tier) }
        batchInsert(selectedMons, ignore = true, shouldReturnGeneratedValues = false) {
            this[WRCNAME] = wrcname
            this[GAMEDAY] = gameday
            this[USERID] = userId
            this[MON] = it
            this[TIER] = tier
        }
    }

    suspend fun buildPickedMonsMessage(
        wrcName: String,
        gameday: Int,
        userId: Long,
        monsProvided: List<DraftPokemon>? = null
    ) = buildString {
        val mons = monsProvided ?: getOrderedPickedMons(wrcName, gameday, userId)
        val tera = WRCTeraDB.getDataForUser(wrcName, gameday, userId)?.second
        var currentTier: String? = null
        for (mon in mons) {
            if (mon.tier != currentTier) {
                currentTier = mon.tier
                append("\n**${mon.tier}:**\n")
            }
            append("${mon.name.condAppend(mon.name == tera, " (TERA)")}\n")
        }
    }.trim()

    suspend fun getUnorderedPickedMons(wrcname: String, gameday: Int, user: Long) = dbTransaction {
        select(MON, TIER).where { (WRCNAME eq wrcname) and (GAMEDAY eq gameday) and (USERID eq user) }.map {
            DraftPokemon(it[MON], it[TIER])
        }.toList()
    }

    suspend fun getOrderedPickedMons(wrcname: String, gameday: Int, user: Long) = dbTransaction {
        val tl = WRCDataDB.getTierlistOfWrcName(wrcname) ?: error("no tierlist found for wrc $wrcname $gameday $user")
        getUnorderedPickedMons(wrcname, gameday, user).sortedWith(tl.tierorderingComparator)
    }

}

suspend fun <T> Flow<T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val first = mutableListOf<T>()
    val second = mutableListOf<T>()
    collect { item ->
        if (predicate(item)) first.add(item)
        else second.add(item)
    }
    return first to second
}
