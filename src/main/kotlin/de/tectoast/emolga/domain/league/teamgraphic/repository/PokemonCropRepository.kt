package de.tectoast.emolga.domain.league.teamgraphic.repository

import de.tectoast.emolga.domain.league.teamgraphic.model.DrawData
import de.tectoast.emolga.domain.league.teamgraphic.model.PokemonCropData
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistEntryTable
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistMetaTable
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.model.showdownIDColumn
import de.tectoast.emolga.domain.pokemon.repository.referencesPokedex
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@Single
@OptIn(ExperimentalTime::class)
class PokemonCropRepository(
    private val db: R2dbcDatabase,
) {
    private val tierlistJoinedTable = TierlistEntryTable.innerJoin(TierlistMetaTable, { this.tierlistId }, { this.id })

    suspend fun getNewPokemonToCrop(guild: Long, now: Instant) = suspendTransaction(db) {
        tierlistJoinedTable.leftJoin(PokemonCropTable, additionalConstraint = {
            (TierlistMetaTable.guild eq PokemonCropTable.guild) and (TierlistEntryTable.showdownId eq PokemonCropTable.showdownId)
        })
            .select(
                TierlistEntryTable.showdownId,
                PokemonCropTable.guild,
                PokemonCropTable.wipSince,
            )
            .where {
                TierlistMetaTable.guild eq guild and (PokemonCropTable.guild.isNull() or (PokemonCropTable.wipSince less now
                    .minus(1.minutes)))
            }
            .orderBy(Random())
            .limit(1)
            .firstOrNull()?.get(TierlistEntryTable.showdownId)
    }

    suspend fun getDoneCount(guild: Long) = suspendTransaction(db) {
        PokemonCropTable.selectAll().where { PokemonCropTable.guild eq guild }.count()
    }

    suspend fun getTotalCount(guild: Long) = suspendTransaction(db) {
        tierlistJoinedTable.selectAll().where { TierlistMetaTable.guild eq guild }.count()
    }

    suspend fun getFinished(guild: Long): List<PokemonCropData> = suspendTransaction(db) {
        PokemonCropTable.selectAll()
            .where { PokemonCropTable.guild eq guild and PokemonCropTable.wipSince.isNull() }
            .map {
                PokemonCropData(
                    it[PokemonCropTable.showdownId],
                    it[PokemonCropTable.x],
                    it[PokemonCropTable.y],
                    it[PokemonCropTable.size],
                    it[PokemonCropTable.flipped]
                )
            }
            .toList()
    }

    suspend fun setWIP(guild: Long, showdownId: ShowdownID, now: Instant) = suspendTransaction(db) {
        PokemonCropTable.upsert {
            it[this.guild] = guild
            it[this.showdownId] = showdownId
            it[wipSince] = now
        }
    }


    suspend fun insertPokemonCropData(
        guild: Long,
        data: PokemonCropData,
        user: Long,
    ): Unit = suspendTransaction(db) {
        PokemonCropTable.update(where = {
            (PokemonCropTable.guild eq guild) and (PokemonCropTable.showdownId eq data.showdownId)
        }) {
            it[PokemonCropTable.x] = data.x
            it[PokemonCropTable.y] = data.y
            it[PokemonCropTable.size] = data.size
            it[PokemonCropTable.user] = user
            it[PokemonCropTable.flipped] = data.flipped
            it[PokemonCropTable.wipSince] = null
        }
    }


    suspend fun getDrawData(guild: Long, sdNames: List<ShowdownID>): Map<ShowdownID, DrawData> =
        suspendTransaction(db) {
            PokemonCropTable.selectAll()
                .where { PokemonCropTable.guild eq guild and (PokemonCropTable.showdownId.inList(sdNames)) }
                .associate {
                    it[PokemonCropTable.showdownId] to DrawData(
                        name = it[PokemonCropTable.showdownId],
                        x = it[PokemonCropTable.x],
                        y = it[PokemonCropTable.y],
                        size = it[PokemonCropTable.size],
                        flipped = it[PokemonCropTable.flipped]
                    )
                }
        }

}

@OptIn(ExperimentalTime::class)
object PokemonCropTable : Table("pokemon_crop") {
    val guild = long("guild")
    val showdownId = showdownIDColumn().referencesPokedex()
    val x = integer("x").default(0)
    val y = integer("y").default(0)
    val user = long("user").default(0)
    val size = integer("size").default(0)
    val flipped = bool("flipped").default(false)
    val wipSince = timestamp("wip_since").nullable()

    override val primaryKey = PrimaryKey(guild, showdownId)
}
