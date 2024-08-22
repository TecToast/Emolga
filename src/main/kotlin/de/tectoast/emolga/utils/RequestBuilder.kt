package de.tectoast.emolga.utils

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.services.sheets.v4.model.*
import de.tectoast.emolga.utils.records.Coord
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

@Suppress("unused")
class RequestBuilder
/**
 * Creates a RequestBuilder
 *
 * @param sid The ID of the sheet where the values should be written
 */(val sid: String) {
    private val requests: MutableList<MyRequest> = ArrayList()
    private var executed = false
    private var runnable: (suspend () -> Unit)? = null
    private var delay: Long = 0
    private var suppressMessages = false
    private var additionalSheets: Array<String>? = null
    private var onlyBatch = false
    private var realExecute = true
    fun withRunnable(r: suspend () -> Unit): RequestBuilder {
        runnable = r
        return this
    }

    fun withRunnable(delay: Long, r: suspend () -> Unit): RequestBuilder {
        runnable = r
        this.delay = delay
        return this
    }

    fun suppressMessages(): RequestBuilder {
        suppressMessages = true
        return this
    }

    fun withAdditionalSheets(vararg additionalSheets: String): RequestBuilder {
        this.additionalSheets = arrayOf(*additionalSheets)
        return this
    }

    fun onlyBatch(): RequestBuilder {
        onlyBatch = true
        return this
    }

    /**
     * Adds a single object to the builder
     *
     * @param range The range, where the object should be written
     * @param body  The single object that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    fun addSingle(range: String?, body: Any, raw: Boolean = false): RequestBuilder {
        return addRow(range, listOf(body), raw)
    }

    fun addSingle(range: Coord, body: Any, raw: Boolean = false): RequestBuilder {
        return addSingle(range.toString(), body, raw)
    }

    /**
     * Adds a row of objects to the builder
     *
     * @param range The range, where the row should be written
     * @param body  The row that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    fun addRow(range: String?, body: List<Any>, raw: Boolean = false): RequestBuilder {
        return addAll(range, listOf(body), raw)
    }

    fun addRow(range: Coord, body: List<Any>, raw: Boolean = false): RequestBuilder {
        return addRow(range.toString(), body, raw)
    }

    /**
     * Adds a matrix of objects to the builder
     *
     * @param range The range, where the objects should be written
     * @param body  The matrix that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    fun addAll(range: String?, body: List<List<Any>?>?, raw: Boolean = false): RequestBuilder {
        requests.add(
            MyRequest().setRange(range).setSend(body)
                .setValueInputOption(if (raw) ValueInputOption.RAW else ValueInputOption.USER_ENTERED)
        )
        return this
    }

    fun addAll(range: Coord, body: List<List<Any>?>?, raw: Boolean = false): RequestBuilder {
        return addAll(range.toString(), body, raw)
    }

    fun addColumn(range: String?, body: List<Any>, raw: Boolean = false): RequestBuilder {
        return addAll(
            range, body.map { listOf(it) }, raw
        )
    }

    fun addColumn(range: Coord, body: List<Any>, raw: Boolean = false): RequestBuilder {
        return addColumn(range.toString(), body, raw)
    }

    /**
     * Adds one or multiple batch request(s) to the builder
     *
     * @param requests The request(s) that should be sent
     * @return this RequestBuilder
     */
    fun addBatch(vararg requests: Request): RequestBuilder {
        requests.map { MyRequest().setRequest(it) }.forEach { this.requests.add(it) }
        return this
    }

    private fun getCellsAsRowData(cellData: CellData, x: Int, y: Int) =
        List(y) { RowData().setValues(List(x) { cellData }) }

    private fun addBGColorChange(sheetId: Int, range: String, c: Color?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.backgroundColor").setRows(
                        getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setBackgroundColor(c)),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    fun addCellFormatChange(sheetId: Int, range: String, format: CellFormat, vararg fields: String): RequestBuilder {
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

    fun addBGColorChange(sheetId: Int, x: Int, y: Int, c: Color?): RequestBuilder {
        return addBGColorChange(sheetId, x.xc() + y, c)
    }

    fun addNoteChange(sheetId: Int, range: String, note: String?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId)).setFields("note").setRows(
                    getCellsAsRowData(
                        CellData().setNote(note),
                        getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                        getRowFromRange(s2) - getRowFromRange(s1) + 1
                    )
                )
            )
        )
    }

    fun addHorizontalAlignmentChange(sheetId: Int, range: String, alignment: String?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.horizontalAlignment").setRows(
                        getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setHorizontalAlignment(alignment)),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    fun addVerticalAlignmentChange(sheetId: Int, range: String, alignment: String?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.verticalAlignment").setRows(
                        getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setVerticalAlignment(alignment)),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    fun addFontChange(sheetId: Int, range: String, font: String?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.textFormat.fontFamily").setRows(
                        getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setTextFormat(TextFormat().setFontFamily(font))),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    fun addCopyPasteChange(sheetId: Int, range: String, target: String, pasteType: String): RequestBuilder {
        return addBatch(
            Request().setCopyPaste(
                CopyPasteRequest().setSource(buildGridRange(range, sheetId))
                    .setDestination(buildGridRange(target, sheetId)).setPasteType(pasteType)
                    .setPasteOrientation("NORMAL")
            )
        )
    }

    fun addCutPasteChange(sheetId: Int, range: String, target: String, pasteType: String): RequestBuilder {
        return addBatch(
            Request().setCutPaste(
                CutPasteRequest().setSource(buildGridRange(range, sheetId))
                    .setDestination(buildGridCoordinate(target, sheetId)).setPasteType(pasteType)
            )
        )
    }

    class ConditionalFormat(val value: String, val format: CellFormat)

    fun addConditionalFormatCustomFormula(format: ConditionalFormat, range: String, id: Int): RequestBuilder {
        addBatch(
            Request().setAddConditionalFormatRule(
                AddConditionalFormatRuleRequest().setIndex(0).setRule(
                    ConditionalFormatRule().setRanges(listOf(buildGridRange(range, id))).setBooleanRule(
                        BooleanRule().setCondition(
                            BooleanCondition().setType("CUSTOM_FORMULA")
                                .setValues(listOf(ConditionValue().setUserEnteredValue(format.value)))
                        ).setFormat(format.format)
                    )
                )
            )
        )
        return this
    }

    fun addStrikethroughChange(sheetId: Int, range: String, strikethrough: Boolean): RequestBuilder {
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

    fun addStrikethroughChange(sheetId: Int, x: Int, y: Int, strikethrough: Boolean): RequestBuilder {
        return addStrikethroughChange(sheetId, x.xc() + y, strikethrough)
    }

    fun addFGColorChange(sheetId: Int, range: String, c: Color?): RequestBuilder {
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

    fun addFGColorChange(sheetId: Int, x: Int, y: Int, c: Color?): RequestBuilder {
        return addFGColorChange(sheetId, x.xc() + y, c)
    }

    private val userEntered: List<ValueRange>
        get() = requests.filter { it.valueInputOption == ValueInputOption.USER_ENTERED }.map { it.build() }
    private val raw: List<ValueRange>
        get() = requests.filter { it.valueInputOption == ValueInputOption.RAW }.map { it.build() }
    private val batch: List<Request>
        get() = requests.filter { it.valueInputOption == ValueInputOption.BATCH }.map { it.buildBatch() }

    fun clear() {
        requests.clear()
        executed = false
    }

    fun execute(realExecute: Boolean = true) {
        this.realExecute = realExecute
        scheduleForExecution(this)
    }

    /**
     * Executes the request to the specified google sheet
     */
    private fun internalExecute(tries: Int = 0) {
        check(!executed) {
            """
     Already executed RequestBuilder with requests:
     sid = $sid
     $requests
     """.trimIndent()
        }
        executed = true
        val userentered = userEntered
        if (!realExecute) {
            printUserEntered(userentered)
            return
        }
        val raw = raw
        val batch = batch
        val job = scope.async {
            launch {
                if (batch.isNotEmpty()) {
                    Google.batchUpdate(sid, batch)
                    additionalSheets?.forEach {
                        Google.batchUpdate(it, batch)
                    }
                }
            }
            if (!onlyBatch) {
                launch {
                    if (userentered.isNotEmpty()) {
                        if (!suppressMessages) printUserEntered()
                        Google.batchUpdate(sid, userentered, "USER_ENTERED")
                        additionalSheets?.forEach {
                            Google.batchUpdate(it, userentered, "USER_ENTERED")
                        }
                    }
                }
                launch {
                    if (raw.isNotEmpty()) {
                        Google.batchUpdate(sid, raw, "RAW")
                        additionalSheets?.forEach {
                            Google.batchUpdate(it, raw, "RAW")
                        }
                    }
                }
            }
        }
        scope.launch {
            try {
                job.await()
            } catch (ex: GoogleJsonResponseException) {
                if (ex.details.code == 401) {
                    val newTries = tries + 1
                    val timeMillis = newTries * 3000L
                    logger.info("Got 401, trying again in {}ms", timeMillis)
                    delay(timeMillis)
                    if (newTries < 3) {
                        internalExecute(newTries)
                    } else {
                        throw ex
                    }
                } else {
                    throw ex
                }
            }
        }
        runnable?.let {
            scope.launch {
                job.join()
                delay(delay)
                it()
            }
        }
    }

    fun executeAndReset(realExecute: Boolean = true) {
        execute(realExecute)
        clear()
    }

    fun printUserEntered(userentered: List<ValueRange> = userEntered) {
        logger.info("RequestBuilder with requests:")
        logger.info("sid = $sid")
        for (i in userentered.indices) {
            val range = userentered[i]
            logger.info("{}: {} -> {}", i, range.range, range.values)
        }
    }

    private enum class ValueInputOption {
        RAW, USER_ENTERED, BATCH
    }

    private class MyRequest {
        private var range: String? = null
        private var send: List<List<Any>?>? = null
        var valueInputOption: ValueInputOption? = null
        private var request: Request? = null
        fun setRange(range: String?): MyRequest {
            this.range = range
            return this
        }

        fun setSend(send: List<List<Any>?>?): MyRequest {
            this.send = send
            return this
        }

        fun setRequest(request: Request?): MyRequest {
            this.request = request
            valueInputOption = ValueInputOption.BATCH
            return this
        }

        fun setValueInputOption(valueInputOption: ValueInputOption?): MyRequest {
            this.valueInputOption = valueInputOption
            return this
        }

        fun build(): ValueRange {
            return ValueRange().setValues(send).setRange(range)
        }

        fun buildBatch(): Request {
            return request!!
        }

        override fun toString(): String {
            return "Request{range='$range', send=$send, valueInputOption='$valueInputOption'}"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RequestBuilder::class.java)
        private val EVERYTHING_BUT_NUMBER = Pattern.compile("\\D")
        private val EVERYTHING_BUT_CHARS = Pattern.compile("[^a-zA-Z]")
        private val scope = createCoroutineScope("RequestBuilder")
        private val channelCollectScope = createCoroutineScope("RequestBuilderChannelCollect")
        private val channel = Channel<RequestBuilder>(Channel.UNLIMITED)

        init {
            channelCollectScope.launch {
                while (true) {
                    channel.receive().internalExecute()
                    delay(2000)
                }
            }
        }

        private fun scheduleForExecution(b: RequestBuilder) {
            defaultScope.launch(start = CoroutineStart.UNDISPATCHED) { channel.send(b) }
        }

        fun getColumnFromRange(range: String): Int {
            val chars = EVERYTHING_BUT_CHARS.matcher(range).replaceAll("").toCharArray()
            return if (chars.size == 1) chars[0].code - 65 else (chars[0].code - 64) * 26 + (chars[1].code - 65)
        }

        fun getRowFromRange(range: String): Int {
            return EVERYTHING_BUT_NUMBER.matcher(range).replaceAll("").toInt() - 1
        }

        fun buildGridRange(expr: String, sheetId: Int): GridRange {
            val split = expr.split(":")
            val s1 = split[0]
            val s2 = if (split.size == 1) s1 else split[1]
            val r = GridRange()
            r.sheetId = sheetId
            r.setStartColumnIndex(getColumnFromRange(s1)).startRowIndex = getRowFromRange(s1)
            r.setEndColumnIndex(getColumnFromRange(s2) + 1).endRowIndex = getRowFromRange(s2) + 1
            logger.info("r = {}", r.toPrettyString())
            return r
        }

        fun buildGridCoordinate(expr: String, sheetId: Int): GridCoordinate {
            val split = expr.split(":")
            val s1 = split[0]
            val r = GridCoordinate().apply {
                this.sheetId = sheetId
                columnIndex = getColumnFromRange(s1)
                rowIndex = getRowFromRange(s1)
            }
            logger.info("r = {}", r.toPrettyString())
            return r
        }
    }
}
