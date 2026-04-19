package de.tectoast.emolga.utils

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.*
import de.tectoast.emolga.database.coord.AstEnvironment
import de.tectoast.emolga.database.exposed.SheetTemplateData
import de.tectoast.emolga.utils.RequestBuilder.Companion.buildGridRange
import de.tectoast.emolga.utils.RequestBuilder.Companion.getColumnFromRange
import de.tectoast.emolga.utils.RequestBuilder.Companion.getRowFromRange
import de.tectoast.emolga.utils.records.Coord
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.time.Duration

interface SheetUpdateContext {
    fun withRunnable(delay: Duration, block: suspend () -> Unit): SheetUpdateContext
    fun addBatch(vararg requests: Request): SheetUpdateContext
    fun addAll(range: String, body: List<List<Any>>): SheetUpdateContext
    fun addCellFormatChange(sheetId: Int, range: String, format: CellFormat, vararg fields: String): SheetUpdateContext
    fun addStrikethroughChange(sheetId: Int, range: String, strikethrough: Boolean): SheetUpdateContext
    fun addFGColorChange(sheetId: Int, range: String, c: Color?): SheetUpdateContext

    fun addSingle(range: String, body: Any): SheetUpdateContext {
        return addRow(range, listOf(body))
    }

    fun addSingle(range: Coord, body: Any): SheetUpdateContext {
        return addSingle(range.toString(), body)
    }

    fun addRow(range: String, body: List<Any>): SheetUpdateContext {
        return addAll(range, listOf(body))
    }

    fun addRow(range: Coord, body: List<Any>): SheetUpdateContext {
        return addRow(range.toString(), body)
    }

    fun addAll(range: Coord, body: List<List<Any>>): SheetUpdateContext {
        return addAll(range.toString(), body)
    }

    fun addColumn(range: String, body: List<Any>): SheetUpdateContext {
        return addAll(
            range, body.map { listOf(it) }
        )
    }

    fun addColumn(range: Coord, body: List<Any>): SheetUpdateContext {
        return addColumn(range.toString(), body)
    }

    fun addStrikethroughChange(sheetId: Int, x: Int, y: Int, strikethrough: Boolean): SheetUpdateContext {
        return addStrikethroughChange(sheetId, x.xc() + y, strikethrough)
    }

}

fun SheetUpdateContext.applySheetTemplate(templateData: SheetTemplateData, astEnvironment: AstEnvironment) {
    templateData.forEach {
        val coord = it.coord.eval(astEnvironment)
        val value = it.value.evalForSheet(astEnvironment)
        addSingle(coord, value)
    }
}

interface SpreadsheetService {
    suspend fun <T> updateSheet(spreadsheetId: String, wait: Boolean, block: suspend SheetUpdateContext.() -> T): T
}

class GoogleSheetUpdateContext : SheetUpdateContext {
    val batchRequests = mutableListOf<Request>()
    val valueUpdates = mutableListOf<ValueRange>()
    var runnable: (suspend () -> Unit)? = null
    var delay: Duration = Duration.ZERO

    fun isEmpty() = batchRequests.isEmpty() && valueUpdates.isEmpty() && runnable == null

    override fun withRunnable(
        delay: Duration,
        block: suspend () -> Unit
    ): SheetUpdateContext = apply {
        this.runnable = block
        this.delay = delay
    }

    override fun addBatch(vararg requests: Request) = apply {
        batchRequests.addAll(requests)
    }

    override fun addAll(
        range: String,
        body: List<List<Any>>
    ): SheetUpdateContext = apply {
        valueUpdates.add(ValueRange().setRange(range).setValues(body))
    }

    override fun addCellFormatChange(
        sheetId: Int,
        range: String,
        format: CellFormat,
        vararg fields: String
    ): SheetUpdateContext {
        val split = range.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat(${fields.joinToString(",")})").setRows(
                        getCellsAsRowData(
                            CellData().setUserEnteredFormat(format),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    override fun addStrikethroughChange(
        sheetId: Int,
        range: String,
        strikethrough: Boolean
    ): SheetUpdateContext {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.textFormat.strikethrough").setRows(
                        getCellsAsRowData(
                            CellData().setUserEnteredFormat(
                                CellFormat().setTextFormat(
                                    TextFormat().setStrikethrough(
                                        strikethrough
                                    )
                                )
                            ),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    override fun addFGColorChange(
        sheetId: Int,
        range: String,
        c: Color?
    ): SheetUpdateContext {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.textFormat.foregroundColor").setRows(
                        getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setTextFormat(TextFormat().setForegroundColor(c))),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    private fun getCellsAsRowData(cellData: CellData, x: Int, y: Int) =
        List(y) { RowData().setValues(List(x) { cellData }) }
}

@Single
class GoogleSpreadsheetService(
    val googleApi: Google,
    @Named("GoogleRateLimiter") val rateLimiter: RateLimiter
) : SpreadsheetService {

    val scope = createCoroutineScope("GoogleSpreadsheetService")
    private val logger = KotlinLogging.logger {}

    override suspend fun <T> updateSheet(
        spreadsheetId: String,
        wait: Boolean,
        block: suspend SheetUpdateContext.() -> T
    ) : T {
        val context = GoogleSheetUpdateContext()
        val result = context.block()
        executeContext(spreadsheetId, context, wait)
        return result
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
        var currentDelay = 1000L

        repeat(maxRetries) { attempt ->
            try {
                rateLimiter.withPermit {
                    googleApi.batchUpdate(sheetId, ctx.batchRequests)
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