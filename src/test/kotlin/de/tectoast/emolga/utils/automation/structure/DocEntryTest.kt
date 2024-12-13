package de.tectoast.emolga.utils.automation.structure

import de.tectoast.emolga.utils.json.db
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import org.bson.Document

class DocEntryTest : FunSpec({
    context("QueryWorks") {
        xtest("Not Match") {
            db.matchresults.find(
                Document(
                    "\$expr", Document(
                        "\$setIsSubset", listOf(
                            "\$uids", listOf(175910318608744448, 723829878755164202)
                        )
                    )
                )
            ).toList().shouldBeEmpty()
        }
    }
})
