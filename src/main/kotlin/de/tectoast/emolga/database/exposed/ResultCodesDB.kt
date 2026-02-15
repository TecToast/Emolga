package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.draft.isEnglish
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.toSDName
import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object ResultCodesDB : Table("resultcodes") {
    val CODE = uuid("code")
    val LEAGUENAME = varchar("leaguename", 32)
    val GAMEDAY = integer("gameday")
    val P1 = integer("p1")
    val P2 = integer("p2")

    override val primaryKey = PrimaryKey(CODE)

    suspend fun getEntryByCode(resultid: String) = dbTransaction {
        val uuid = Uuid.parseHexDashOrNull(resultid) ?: return@dbTransaction null
        selectAll().where { CODE eq uuid }.singleOrNull()
    }

    suspend fun getResultDataForUser(resultid: String) = dbTransaction {
        val uuid = Uuid.parseHexDashOrNull(resultid) ?: return@dbTransaction null
        val entry = selectAll().where { CODE eq uuid }.singleOrNull() ?: return@dbTransaction null
        val league = de.tectoast.emolga.utils.json.db.league(entry[LEAGUENAME])
        val gid = league.guild
        val guild = jda.getGuildById(gid) ?: return@dbTransaction null
        val idxes = listOf(entry[P1], entry[P2])
        val memberData = guild.retrieveMembersByIds(idxes.map { league[it] }).await().associateBy { it.idLong }
        val tlEnglish = league.tierlist.isEnglish
        val allMonsTranslations =
            NameConventionsDB.getAllData(
                idxes.flatMap { league.picks(it) }.map { it.name }, NameConventionsDB.GERMAN, gid
            ).associateBy { it.official }
        ResultCodeResponse(
            guildName = guild.name,
            logoUrl = guild.iconUrl,
            bo3 = league.config.triggers.bo3,
            gameday = entry[GAMEDAY],
            data = idxes.map { idx ->
                val picks = league.picks(idx)
                val uid = league[idx]
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
                                de.tectoast.emolga.utils.json.db.pokedex.get(englishOfficial.toSDName())!!
                                    .calcSpriteName()
                            } else englishOfficial.toSDName()
                        )
                    })
            })
    }

    suspend fun add(leaguename: String, gameday: Int, p1: Int, p2: Int): Uuid {
        var code: Uuid
        while (true) {
            code = Uuid.random()
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

    suspend fun delete(code: Uuid) = dbTransaction {
        deleteWhere { CODE eq code }
    }

    suspend fun deleteFromLeague(league: String) = dbTransaction {
        deleteWhere { LEAGUENAME eq league }
    }

}

@Serializable
data class ResultCodeResponse(
    val guildName: String, val logoUrl: String?, val bo3: Boolean, val gameday: Int, val data: List<ResultUserData>
)

@Serializable
data class ResultUserData(val name: String, val avatarUrl: String, val picks: List<ResultCodePokemon>)

@Serializable
data class ResultCodePokemon(val tlName: String, val spriteName: String)
