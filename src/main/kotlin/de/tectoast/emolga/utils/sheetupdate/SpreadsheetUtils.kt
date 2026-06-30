package de.tectoast.emolga.utils.sheetupdate

import com.google.api.services.sheets.v4.model.GridRange
import de.tectoast.emolga.domain.league.doc.model.SheetTemplateData
import de.tectoast.emolga.utils.dsl.AstEnvironment
import java.util.regex.Pattern

private val EVERYTHING_BUT_NUMBER = Pattern.compile("\\D")
private val EVERYTHING_BUT_CHARS = Pattern.compile("[^a-zA-Z]")

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
    return r
}

fun SheetUpdateContext.applySheetTemplate(templateData: SheetTemplateData, astEnvironment: AstEnvironment) {
    templateData.forEach {
        val coord = it.coord.eval(astEnvironment)
        val value = it.value.evalForSheet(astEnvironment)
        addSingle(coord, value)
    }
}