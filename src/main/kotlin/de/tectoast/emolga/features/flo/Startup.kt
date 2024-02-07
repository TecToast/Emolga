package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.ListenerProvider
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.marker
import mu.KotlinLogging
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.litote.kmongo.eq

object Startup : ListenerProvider() {

    private val logger = KotlinLogging.logger {}

    init {
        registerListener<ReadyEvent> { e ->
            logger.info("important".marker, "Ready event received!")
            if (e.jda.selfUser.idLong == 723829878755164202) {
                logger.info("important".marker, "Emolga is now online!")
                db.drafts.find(League::isRunning eq true).toList().forEach {
                    if (it.noAutoStart) return@forEach
                    logger.info("important".marker, "Starting draft ${it.leaguename}...")
                    it.startDraft(null, true, null)
                }
            }
        }
    }
}
