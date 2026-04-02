package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.toSDName
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.JDA
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ResultCodeEntry(
    val code: Uuid,
    val leagueName: String,
    val gameday: Int,
    val p1: Int,
    val p2: Int
)

@OptIn(ExperimentalUuidApi::class)
interface ResultCodesRepository {
    suspend fun getEntryByCode(resultid: String): ResultCodeEntry?
    suspend fun getResultDataForUser(resultid: String): ResultCodeResponse?
    suspend fun add(leaguename: String, gameday: Int, p1: Int, p2: Int): Uuid
    suspend fun delete(code: Uuid)
    suspend fun deleteFromLeague(league: String)
}

@OptIn(ExperimentalUuidApi::class)
@Single(binds = [ResultCodesRepository::class])
class PostgresResultCodesRepository(val db: R2dbcDatabase, val resultCodes: ResultCodesDB, val jda: JDA) :
    ResultCodesRepository {
    override suspend fun getEntryByCode(resultid: String): ResultCodeEntry? = suspendTransaction(db) {
        val uuid = Uuid.parseHexDashOrNull(resultid) ?: return@suspendTransaction null
        resultCodes.selectAll().where { resultCodes.CODE eq uuid }.singleOrNull()?.let {
            ResultCodeEntry(
                code = it[resultCodes.CODE],
                leagueName = it[resultCodes.LEAGUENAME],
                gameday = it[resultCodes.GAMEDAY],
                p1 = it[resultCodes.P1],
                p2 = it[resultCodes.P2]
            )
        }
    }

    override suspend fun getResultDataForUser(resultid: String) = suspendTransaction(db) {
        val uuid = Uuid.parseHexDashOrNull(resultid) ?: return@suspendTransaction null
        val entry =
            resultCodes.selectAll().where { resultCodes.CODE eq uuid }.singleOrNull() ?: return@suspendTransaction null
        val league = mdb.league(entry[resultCodes.LEAGUENAME])
        val gid = league.guild
        val guild = jda.getGuildById(gid) ?: return@suspendTransaction null
        val idxes = listOf(entry[resultCodes.P1], entry[resultCodes.P2])
        val memberData = guild.retrieveMembersByIds(idxes.mapNotNull { league[it] }).await().associateBy { it.idLong }
        val tlEnglish = league.tierlist.isEnglish
        val allMonsTranslations =
            NameConventionsDB.getAllData(
                idxes.flatMap { league.picks(it) }.map { it.name }, NameConventionsDB.GERMAN, gid
            )
        ResultCodeResponse(
            guildName = guild.name,
            logoUrl = guild.iconUrl,
            bo3 = league.config.triggers.bo3,
            gameday = entry[resultCodes.GAMEDAY],
            data = idxes.map { idx ->
                val picks = league.picks(idx)
                val uid = league[idx] ?: 0L
                val member = memberData[uid]!!
                val avatarUrl = member.effectiveAvatarUrl
                ResultUserData(
                    name = member.user.effectiveName,
                    avatarUrl = avatarUrl,
                    picks = picks.sortedWith(league.tierorderingComparator).map {
                        val nameData = allMonsTranslations[it.name]!!
                        val englishOfficial = nameData.otherOfficial!!
                        ResultCodePokemon(
                            if (tlEnglish) nameData.otherTl!! else nameData.tlName, if ("-" in englishOfficial) {
                                mdb.pokedex.get(englishOfficial.toSDName())!!
                                    .calcSpriteName()
                            } else englishOfficial.toSDName()
                        )
                    })
            })
    }

    override suspend fun add(leaguename: String, gameday: Int, p1: Int, p2: Int): Uuid {
        val code: Uuid = Uuid.generateV7()
        suspendTransaction(db) {
            resultCodes.insert {
                it[resultCodes.CODE] = code
                it[resultCodes.LEAGUENAME] = leaguename
                it[resultCodes.GAMEDAY] = gameday
                it[resultCodes.P1] = p1
                it[resultCodes.P2] = p2
            }
        }
        return code
    }

    override suspend fun delete(code: Uuid) {
        suspendTransaction(db) {
            resultCodes.deleteWhere { resultCodes.CODE eq code }
        }
    }

    override suspend fun deleteFromLeague(league: String) {
        suspendTransaction(db) {
            resultCodes.deleteWhere { resultCodes.LEAGUENAME eq league }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Single
class ResultCodesDB : Table("resultcodes") {
    val CODE = uuid("code")
    val LEAGUENAME = varchar("leaguename", 32)
    val GAMEDAY = integer("gameday")
    val P1 = integer("p1")
    val P2 = integer("p2")

    override val primaryKey = PrimaryKey(CODE)
}

@Serializable
data class ResultCodeResponse(
    val guildName: String, val logoUrl: String?, val bo3: Boolean, val gameday: Int, val data: List<ResultUserData>
)

@Serializable
data class ResultUserData(val name: String, val avatarUrl: String, val picks: List<ResultCodePokemon>)

@Serializable
data class ResultCodePokemon(val tlName: String, val spriteName: String)
