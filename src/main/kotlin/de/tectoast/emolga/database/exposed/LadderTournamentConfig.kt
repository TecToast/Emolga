package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.jsonb
import de.tectoast.emolga.utils.toSDName
import de.tectoast.emolga.utils.toUsername
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.reflect.KProperty1

object LadderTournamentConfigTable : Table("ladder_tournament_config") {
    val guild = long("league")
    val data = jsonb<LadderTournamentConfig>("data")
}

object LadderTournamentUserTable : Table("ladder_tournament_users") {
    val guild = long("guild")
    val userId = long("user")
    val sdName = varchar("sd_name", 100)
    val formats = array<String>("formats")
    val verified = bool("verified")

    override val primaryKey = PrimaryKey(guild, userId)
}

class LadderTournamentRepository(val db: R2dbcDatabase) {
    suspend fun getConfigByGuild(guild: Long) = suspendTransaction(db) {
        LadderTournamentConfigTable.select(LadderTournamentConfigTable.data)
            .where { LadderTournamentConfigTable.guild eq guild }
            .firstOrNull()?.get(LadderTournamentConfigTable.data)
    }

    suspend fun getVerifiedUsersByGuild(guild: Long) = suspendTransaction(db) {
        LadderTournamentUserTable.selectAll()
            .where { (LadderTournamentUserTable.guild eq guild) and (LadderTournamentUserTable.verified eq true) }
            .toMap {
                it[LadderTournamentUserTable.userId] to LadderTournamentUserData(
                    it[LadderTournamentUserTable.sdName],
                    it[LadderTournamentUserTable.formats],
                    it[LadderTournamentUserTable.verified]
                )
            }

    }
}

class SDLadderDataProvider(val httpClient: HttpClient) {
    suspend fun fetchDataForUser(sdName: String): SDUserResponse {
        delay(Random.nextLong(5000, 10000))
        repeat(5) { _ ->
            val response = httpClient.get("https://pokemonshowdown.com/users/${sdName.toUsername()}.json")
            if (response.status.isSuccess()) {
                return response.body<SDUserResponse>()
            }
            delay(Random.nextLong(5000, 15000))
        }
        error("Failed to fetch data for user $sdName")
    }
}

class LadderTournamentService(
    val repository: LadderTournamentRepository,
    val sdLadderDataProvider: SDLadderDataProvider
) {
    suspend fun executeForGuild(guild: Long) {
        val config = repository.getConfigByGuild(guild) ?: return
        val users = repository.getVerifiedUsersByGuild(guild)
        val usersPerFormat =
            users.flatMap { (uid, data) -> data.formats.map { it to uid } }
                .groupBy { it.first }
                .mapValues { it.value.map { v -> v.second } }
        val userData = fetchDataForUsers(users)
        val b = RequestBuilder(config.sid)
        for ((format, targetRange) in config.formats) {
            val formatId = format.toSDName()
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
            b.addAll(targetRange, tableData)
        }
        b.execute()
    }

    private suspend fun fetchDataForUsers(users: Map<Long, LadderTournamentUserData>): Map<Long, SDUserResponse> {
        return users.mapValues {
            sdLadderDataProvider.fetchDataForUser(it.value.sdName)
        }
    }
}


@Serializable
data class LadderTournamentConfig(
    val adminChannel: Long,
    val signupChannel: Long,
    val formats: Map<String, String>,
    val sid: String,
    val cols: List<LadderTournamentCol>,
    val sortCols: List<LadderTournamentCol>,
    val lastExecution: Long,
    val durationInHours: Int,
    val amount: Int,
    val sdNamePrefix: String,
)

@Serializable
enum class LadderTournamentCol(val property: KProperty1<SDRankData, Number>) {
    WINS(SDRankData::wins),
    LOSSES(SDRankData::losses),
    TIES(SDRankData::ties),
    GXE(SDRankData::gxe),
    ELO(SDRankData::elo);

    operator fun get(data: SDRankData?) = data?.let { property.get(it) } ?: 0
}


data class LadderTournamentUserData(val sdName: String, val formats: List<String>, var verified: Boolean = true)

@Serializable
data class SDUserResponse(
    val username: String,
    val ratings: Map<String, SDRankData>
)

@Serializable
data class SDRankData(
    @SerialName("w") val wins: Int = 0,
    @SerialName("l") val losses: Int = 0,
    @SerialName("t") val ties: Int = 0,
    val gxe: Double,
    val elo: Double
)
