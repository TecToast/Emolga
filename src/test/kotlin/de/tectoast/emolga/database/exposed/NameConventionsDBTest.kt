package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.showdown.Pokemon
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.litote.kmongo.gt

class NameConventionsDBTest : FunSpec({
    test("AllSDMonsAreInDB") {
        val allMons = db.pokedex.find(Pokemon::num gt 0).toList().map { it.name }
        newSuspendedTransaction {
            val notExistent = allMons - NameConventionsDB.select(NameConventionsDB.ENGLISH)
                .where(NameConventionsDB.ENGLISH inList allMons)
                .map { it[NameConventionsDB.ENGLISH] }
            notExistent.shouldBeEmpty()
        }
    }
})
