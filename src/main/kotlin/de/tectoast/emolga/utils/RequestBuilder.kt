package de.tectoast.emolga.utils

import com.google.api.services.sheets.v4.model.*
import de.tectoast.emolga.commands.Command
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.regex.Pattern

@Suppress("unused")
class RequestBuilder
/**
 * Creates a RequestBuilder
 *
 * @param sid The ID of the sheet where the values should be written
 */(private val sid: String) {
    private val requests: MutableList<MyRequest> = ArrayList()
    private var executed = false
    private var runnable: Runnable? = null
    private var delay: Long = 0
    private var suppressMessages = false
    private var additionalSheets: Array<String>? = null
    private var onlyBatch = false
    fun withRunnable(r: Runnable?): RequestBuilder {
        runnable = r
        return this
    }

    fun withRunnable(delay: Long, r: Runnable?): RequestBuilder {
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
    fun addSingle(range: String?, body: Any, vararg raw: Boolean): RequestBuilder {
        return addRow(range, listOf(body), *raw)
    }

    /**
     * Adds a row of objects to the builder
     *
     * @param range The range, where the row should be written
     * @param body  The row that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    fun addRow(range: String?, body: List<Any>, vararg raw: Boolean): RequestBuilder {
        return addAll(range, listOf(body), *raw)
    }

    /**
     * Adds a matrix of objects to the builder
     *
     * @param range The range, where the objects should be written
     * @param body  The matrix that should be written
     * @param raw   optional argument, which makes the request raw (if true) or user entered (if false or null)
     * @return this RequestBuilder
     */
    fun addAll(range: String?, body: List<List<Any>?>?, vararg raw: Boolean): RequestBuilder {
        requests.add(
            MyRequest().setRange(range).setSend(body)
                .setValueInputOption(if (raw.isEmpty() || !raw[0]) ValueInputOption.USER_ENTERED else ValueInputOption.RAW)
        )
        return this
    }

    fun addColumn(range: String?, body: List<Any>, vararg raw: Boolean): RequestBuilder {
        return addAll(
            range, body.map { listOf(it) }, *raw
        )
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

    private fun addBGColorChange(sheetId: Int, range: String, c: Color?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.backgroundColor").setRows(
                        Command.getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setBackgroundColor(c)),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    fun addBGColorChange(sheetId: Int, x: Int, y: Int, c: Color?): RequestBuilder {
        return addBGColorChange(sheetId, Command.getAsXCoord(x) + y, c)
    }

    fun addNoteChange(sheetId: Int, range: String, note: String?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId)).setFields("note").setRows(
                    Command.getCellsAsRowData(
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
                        Command.getCellsAsRowData(
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
                        Command.getCellsAsRowData(
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
                        Command.getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setTextFormat(TextFormat().setFontFamily(font))),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    fun addStrikethroughChange(sheetId: Int, range: String, strikethrough: Boolean): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.textFormat.strikethrough").setRows(
                        Command.getCellsAsRowData(
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
        return addStrikethroughChange(sheetId, Command.getAsXCoord(x) + y, strikethrough)
    }

    private fun addFGColorChange(sheetId: Int, range: String, c: Color?): RequestBuilder {
        val split = range.split(":")
        val s1 = split[0]
        val s2 = if (split.size == 1) s1 else split[1]
        return addBatch(
            Request().setUpdateCells(
                UpdateCellsRequest().setRange(buildGridRange(range, sheetId))
                    .setFields("userEnteredFormat.textFormat.foregroundColor").setRows(
                        Command.getCellsAsRowData(
                            CellData().setUserEnteredFormat(CellFormat().setTextFormat(TextFormat().setForegroundColor(c))),
                            getColumnFromRange(s2) - getColumnFromRange(s1) + 1,
                            getRowFromRange(s2) - getRowFromRange(s1) + 1
                        )
                    )
            )
        )
    }

    fun addFGColorChange(sheetId: Int, x: Int, y: Int, c: Color?): RequestBuilder {
        return addFGColorChange(sheetId, Command.getAsXCoord(x) + y, c)
    }

    private val userEntered: List<ValueRange>
        get() = requests.filter { it.valueInputOption == ValueInputOption.USER_ENTERED }.map { it.build() }
    private val raw: List<ValueRange>
        get() = requests.filter { it.valueInputOption == ValueInputOption.RAW }.map { it.build() }
    private val batch: List<Request?>
        get() = requests.filter { it.valueInputOption == ValueInputOption.BATCH }.map { it.buildBatch() }

    /**
     * Executes the request to the specified google sheet
     */
    fun execute() {
        check(!executed) {
            """
     Already executed RequestBuilder with requests:
     sid = $sid
     $requests
     """.trimIndent()
        }
        executed = true
        val userentered = userEntered
        val raw = raw
        val batch = batch
        val service = Google.sheetsService
        val job = scope.launch {
            if (!onlyBatch) {
                launch {
                    if (userentered.isNotEmpty()) {
                        if (!suppressMessages) for (i in userentered.indices) {
                            val range = userentered[i]
                            logger.info("{}: {} -> {}", i, range.range, range.getValues())
                        }
                        try {
                            service.spreadsheets().values().batchUpdate(
                                sid, BatchUpdateValuesRequest().setData(userentered).setValueInputOption("USER_ENTERED")
                            ).execute()
                            additionalSheets?.forEach {
                                service.spreadsheets().values().batchUpdate(
                                    it,
                                    BatchUpdateValuesRequest().setData(userentered).setValueInputOption("USER_ENTERED")
                                ).execute()
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Command.sendStacktraceToMe(e)
                        }
                    }
                }
                launch {
                    if (raw.isNotEmpty()) {
                        try {
                            service.spreadsheets().values()
                                .batchUpdate(sid, BatchUpdateValuesRequest().setData(raw).setValueInputOption("RAW"))
                                .execute()
                            additionalSheets?.forEach {
                                service.spreadsheets().values().batchUpdate(
                                    it, BatchUpdateValuesRequest().setData(raw).setValueInputOption("RAW")
                                ).execute()
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Command.sendStacktraceToMe(e)
                        }
                    }
                }
            }
            launch {
                if (batch.isNotEmpty()) {
                    try {
                        service.spreadsheets().batchUpdate(sid, BatchUpdateSpreadsheetRequest().setRequests(batch))
                            .execute()
                        additionalSheets?.forEach {
                            service.spreadsheets().batchUpdate(it, BatchUpdateSpreadsheetRequest().setRequests(batch))
                                .execute()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Command.sendStacktraceToMe(e)
                    }
                }
            }
        }
        runnable?.let {
            scope.launch {
                job.join()
                delay(delay)
                it.run()
            }
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

        fun buildBatch(): Request? {
            return request
        }

        override fun toString(): String {
            return "Request{range='$range', send=$send, valueInputOption='$valueInputOption'}"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RequestBuilder::class.java)
        private val EVERYTHING_BUT_NUMBER = Pattern.compile("\\D")
        private val EVERYTHING_BUT_CHARS = Pattern.compile("[^a-zA-Z]")
        private val scope = CoroutineScope(Dispatchers.IO + CoroutineName("RequestBuilder"))
        fun updateSingle(sid: String?, range: String?, value: Any, vararg raw: Boolean) {
            updateRow(sid, range, listOf(value), *raw)
        }

        private fun updateRow(sid: String?, range: String?, values: List<Any>, vararg raw: Boolean) {
            updateAll(sid, range, listOf(values), *raw)
        }


        fun updateAll(sid: String?, range: String?, values: List<List<Any>?>?, vararg raw: Boolean) {
            scope.launch {
                try {
                    Google.sheetsService.spreadsheets().values().update(sid, range, ValueRange().setValues(values))
                        .setValueInputOption(if (raw.isEmpty() || !raw[0]) "USER_ENTERED" else "RAW").execute()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        fun batchUpdate(sid: String?, vararg requests: Request?) {
            if (requests.isEmpty()) return
            try {
                Google.sheetsService.spreadsheets().batchUpdate(
                    sid, BatchUpdateSpreadsheetRequest().setRequests(listOf(*requests))
                ).execute()
            } catch (e: IOException) {
                e.printStackTrace()
            }
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
            try {
                logger.info("r = {}", r.toPrettyString())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return r
        }
    }
}