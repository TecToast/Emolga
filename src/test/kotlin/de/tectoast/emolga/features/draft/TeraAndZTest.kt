package de.tectoast.emolga.features.draft

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.TestInteractionData
import de.tectoast.emolga.features.draft.during.TeraAndZ
import de.tectoast.emolga.utils.Constants
import io.kotest.core.spec.style.FunSpec

class TeraAndZTest : FunSpec({
    xtest("TeraAndZ") {
        with(TestInteractionData(user = 386164594713952266, gid = Constants.G.NDS, sendReplyInTc = true)) {
            TeraAndZ.Modal.exec {
                tera = DraftName("Milotic", "Milotic")
                type = "Grass"
                z = DraftName("H-Viscogon", "Viscogon-Hisui")
            }
        }
    }
})
