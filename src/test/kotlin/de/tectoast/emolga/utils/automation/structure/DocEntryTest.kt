package de.tectoast.emolga.utils.automation.structure

import de.tectoast.emolga.utils.json.db
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import org.bson.Document

class DocEntryTest : FunSpec({
    context("QueryWorks") {
        test("Not Match") {
            db.matchresults.find(
                Document(
                    "\$expr", Document(
                        "\$setIsSubset", listOf(
                            listOf(175910318608744448, 723829878755164202), "\$uids"
                        )
                    )
                )
            ).toList().shouldBeEmpty()
        }
        test("Should Match") {
            val filter = Document(
                "\$expr", Document(
                    "\$setIsSubset", listOf(
                        listOf(446274734389198848, 811234782805098587), "\$uids"
                    )
                )
            )
            println(filter.toJson())
            db.matchresults.find(
                filter
            ).toList().shouldNotBeEmpty()
        }


    }
})
