package de.tectoast.emolga.utils.sheetupdate

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.ratelimiter.RateLimiter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Duration.Companion.seconds

@Single
class GoogleSpreadsheetService(
    private val googleApi: Google,
    @Named("GoogleRateLimiter") val rateLimiter: RateLimiter
) : SpreadsheetService {

    val scope = createCoroutineScope("GoogleSpreadsheetService")
    private val logger = KotlinLogging.logger {}

    override suspend fun <T> updateSheet(
        spreadsheetId: String,
        wait: Boolean,
        block: suspend SheetUpdateContext.() -> T
    ): T {
        val context = GoogleSheetUpdateContext()
        val result = context.block()
        executeContext(spreadsheetId, context, wait)
        return result
    }

    override suspend fun batchGet(
        spreadsheetId: String,
        ranges: List<String>,
        formula: Boolean,
        majorDimension: String
    ): List<List<List<Any?>?>?>? {
        return googleApi.batchGet(spreadsheetId, ranges, formula, majorDimension)
    }

    private suspend fun executeContext(spreadsheetId: String, context: GoogleSheetUpdateContext, wait: Boolean) {
        if (context.isEmpty()) return
        val task = suspend {
            executeWithRetry(spreadsheetId, context)
            context.runnable?.let {
                delay(context.delay)
                it()
            }
        }
        if (wait) {
            task()
        } else {
            scope.launch {
                try {
                    task()
                } catch (e: Exception) {
                    logger.error("Background Sheet Update failed for $spreadsheetId", e)
                }
            }
        }
    }

    private suspend fun executeWithRetry(
        sheetId: String,
        ctx: GoogleSheetUpdateContext,
        maxRetries: Int = 5
    ) {
        var currentDelay = 1.seconds
        for (valueUpdate in ctx.valueUpdates) {
            logger.info("${valueUpdate.range} -> ${valueUpdate.getValues()}")
        }
        repeat(maxRetries) { attempt ->
            try {
                if (ctx.valueUpdates.isNotEmpty()) {
                    rateLimiter.withPermit {
                        googleApi.batchUpdate(sheetId, ctx.valueUpdates, "USER_ENTERED")
                    }
                }
                if (ctx.batchRequests.isNotEmpty()) {
                    rateLimiter.withPermit {
                        googleApi.batchUpdate(sheetId, ctx.batchRequests)
                    }
                }
                return
            } catch (e: GoogleJsonResponseException) {
                if ((e.statusCode == 503 || e.statusCode == 429 || e.statusCode == 401) && attempt <= maxRetries) {
                    delay(currentDelay)
                    currentDelay *= 2
                } else {
                    throw e
                }
            }
        }
    }


}