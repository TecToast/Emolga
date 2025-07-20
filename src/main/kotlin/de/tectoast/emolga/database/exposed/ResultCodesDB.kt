package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.invoke
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.util.*

object ResultCodesDB : Table("resultcodes") {
    val CODE = uuid("code")
    val LEAGUENAME = varchar("leaguename", 32)
    val GAMEDAY = integer("gameday")
    val P1 = integer("p1")
    val P2 = integer("p2")

    override val primaryKey = PrimaryKey(CODE)

    suspend fun getEntryByCode(resultid: String) = dbTransaction {
        val uuid = runCatching { UUID.fromString(resultid) }.getOrNull() ?: return@dbTransaction null
        selectAll().where { CODE eq uuid }.singleOrNull()
    }

    suspend fun getResultDataForUser(resultid: String) = dbTransaction {
        val uuid = runCatching { UUID.fromString(resultid) }.getOrNull() ?: return@dbTransaction null
        val entry = selectAll().where { CODE eq uuid }.singleOrNull() ?: return@dbTransaction null
        val league = de.tectoast.emolga.utils.json.db.league(entry[LEAGUENAME])
        val allPicks = league.picks
        val gid = league.guild
        val guild = jda.getGuildById(gid) ?: return@dbTransaction null
        val idxes = listOf(entry[P1], entry[P2])
        val memberData = guild.retrieveMembersByIds(idxes.map { league.table[it] }).await().associateBy { it.idLong }
        val allMonsTranslations = NameConventionsDB.getAllData(
            idxes.flatMap { allPicks(it) }.map { it.name },
            NameConventionsDB.GERMAN, gid
        ).associateBy { it.official }
        ResultCodeResponse(
            guildName = guild.name,
            logoUrl = guild.iconUrl,
            gameday = entry[GAMEDAY],
            data = idxes.map { idx ->
                val picks = allPicks(idx)
                val uid = league.table[idx]
                val member = memberData[uid]!!
                val avatarUrl = member.effectiveAvatarUrl
                ResultUserData(
                    name = member.user.effectiveName,
                    avatarUrl = avatarUrl,
                    picks = picks.sortedWith(league.tierorderingComparator).map {
                        val nameData = allMonsTranslations[it.name]!!
                        ResultCodePokemon(nameData.tlName, nameData.otherOfficial!!)
                    }
                )
            }
        )
    }

    suspend fun add(leaguename: String, gameday: Int, p1: Int, p2: Int): UUID {
        var code: UUID
        while (true) {
            code = UUID.randomUUID()
            dbTransaction {
                insert {
                    it[CODE] = code
                    it[LEAGUENAME] = leaguename
                    it[GAMEDAY] = gameday
                    it[P1] = p1
                    it[P2] = p2
                }
            }
            break
        }
        return code
    }

    suspend fun delete(code: UUID) {
        dbTransaction {
            deleteWhere { CODE eq code }
        }
    }
}

@Serializable
data class ResultCodeResponse(
    val guildName: String,
    val logoUrl: String?,
    val gameday: Int,
    val data: List<ResultUserData>
)

@Serializable
data class ResultUserData(val name: String, val avatarUrl: String, val picks: List<ResultCodePokemon>)

@Serializable
data class ResultCodePokemon(val german: String, val english: String)
