package de.tectoast.emolga.utils.sheetupdate

import com.google.api.services.sheets.v4.model.*
import kotlin.time.Duration


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

