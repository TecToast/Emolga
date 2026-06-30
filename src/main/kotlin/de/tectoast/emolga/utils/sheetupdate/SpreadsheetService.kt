package de.tectoast.emolga.utils.sheetupdate

interface SpreadsheetService {
    suspend fun <T> updateSheet(spreadsheetId: String, wait: Boolean, block: suspend SheetUpdateContext.() -> T): T
    suspend fun batchGet(
        spreadsheetId: String,
        ranges: List<String>,
        formula: Boolean,
        majorDimension: String = "ROWS"
    ): List<List<List<Any?>?>?>?
}
