package de.tectoast.emolga.utils.sheetupdate

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.Request
import de.tectoast.emolga.utils.dsl.Coord
import de.tectoast.emolga.utils.xc
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

