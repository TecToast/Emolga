package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.showdown.Pokemon
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.inList
import org.jetbrains.exposed.v1.r2dbc.select
import org.litote.kmongo.gt

class NameConventionsDBTest : FunSpec({
    test("AllSDMonsAreInDB") {
        val allMons = db.pokedex.find(Pokemon::num gt 0).toList().map { it.name }
        dbTransaction {
            val notExistent = allMons - NameConventionsDB.select(NameConventionsDB.ENGLISH)
                .where(NameConventionsDB.ENGLISH inList allMons)
                .map { it[NameConventionsDB.ENGLISH] }
            notExistent.shouldBeEmpty()
        }
    }
})
