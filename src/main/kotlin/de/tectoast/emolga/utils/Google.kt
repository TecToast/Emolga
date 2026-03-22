package de.tectoast.emolga.utils

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.api.services.youtube.YouTube
import de.tectoast.emolga.utils.json.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.koin.core.annotation.Single
import java.net.SocketTimeoutException
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@Single
class Google(val credentials: Tokens.Google) {
    private val logger = KotlinLogging.logger {}
    private val googleContext = createCoroutineContext("Google", Dispatchers.IO)

    @OptIn(ExperimentalTime::class)
    private val accesstoken: TimedCache<String> = TimedCache(45.minutes) { generateAccessToken() }
    private val sheetsService = MappedCache(accesstoken) {
        Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(it)
        ).setApplicationName("emolga").build()
    }
    private val youtubeService = MappedCache(accesstoken) {
        YouTube.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(it)
        ).setApplicationName("emolga").build()
    }

    /**
     * Gets the specified range of data from the specified spreadsheet
     * @param spreadsheetId The ID of the spreadsheet
     * @param range The range of the data
     * @param formula Whether to get the formula or the formatted value
     * @param majorDimension The major dimension of the data (default: "ROWS")
     * @return The data, as a list of rows, each row being a list of cells
     */
    suspend fun get(
        spreadsheetId: String, range: String, formula: Boolean, majorDimension: String = "ROWS"
    ): List<List<Any>> = withContext(googleContext) {
        sheetsService().spreadsheets().values()[spreadsheetId, range].setMajorDimension(majorDimension)
            .setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE").execute().getValues()
    }

    /**
     * Gets the specified ranges of data from the specified spreadsheet
     * @param sid The ID of the spreadsheet
     * @param ranges The ranges of the data
     * @param formula Whether to get the formula or the formatted value
     * @param majorDimension The major dimension of the data (default: "ROWS")
     * @return The data, as a list of ranges, each list being a list of rows, each row being a list of cells
     */
    suspend fun batchGet(
        sid: String, ranges: List<String>, formula: Boolean, majorDimension: String = "ROWS"
    ): List<List<List<Any?>>>? = withContext(googleContext) {
        sheetsService().spreadsheets().values().batchGet(sid).setRanges(ranges).setMajorDimension(majorDimension)
            .setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
            .execute()?.valueRanges?.map { it.getValues() }
    }

    /**
     * Gets the specified ranges of data from the specified spreadsheet
     * @param sid The ID of the spreadsheet
     * @param ranges The ranges of the data
     * @param formula Whether to get the formula or the formatted value
     * @param majorDimension The major dimension of the data (default: "ROWS")
     * @return The data, as a list of ranges, each list being a list of rows, each row being a list of cells
     */
    suspend fun batchGetStrings(
        sid: String, ranges: List<String>, formula: Boolean, majorDimension: String = "ROWS"
    ): List<List<List<String>>> = withContext(googleContext) {
        sheetsService().spreadsheets().values().batchGet(sid).setRanges(ranges).setMajorDimension(majorDimension)
            .setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
            .execute().valueRanges.map { it.getValues().map { row -> row.map { o -> o.toString() } } }
    }


    suspend fun getSheetData(spreadsheetId: String?, vararg range: String): Spreadsheet = withContext(googleContext) {
        sheetsService().spreadsheets()[spreadsheetId].setIncludeGridData(true).setRanges(range.toList()).execute()
    }

    /**
     * Batch updates the specified data in the specified spreadsheet
     * @param sid The ID of the spreadsheet
     * @param data The data to update
     * @param mode The mode of the update ("RAW" for unformatted, "USER_ENTERED" for formatted)
     */
    suspend fun batchUpdate(sid: String, data: List<ValueRange>, mode: String) {
        withContext(googleContext) {
            for (i in 0..5) {
                try {
                    sheetsService().spreadsheets().values()
                        .batchUpdate(sid, BatchUpdateValuesRequest().setData(data).setValueInputOption(mode)).execute()
                    break
                } catch (e: SocketTimeoutException) {
                    logger.warn("Google API request timed out, retrying...", e)
                }
            }
        }
    }

    /**
     * Batch updates the specified data in the specified spreadsheet
     * @param sid The ID of the spreadsheet
     * @param data The data to update
     */
    suspend fun batchUpdate(sid: String, data: List<Request>) {
        withContext(googleContext) {
            sheetsService().spreadsheets().batchUpdate(sid, BatchUpdateSpreadsheetRequest().setRequests(data)).execute()
        }
    }

    private val channelHandleIdCache = SizeLimitedMap<String, String?>(100)
    suspend fun fetchChannelId(channelHandle: String) = channelHandleIdCache.getOrPut(channelHandle) {
        withContext(googleContext) {
            youtubeService().channels().list("snippet".l).apply {
                forHandle = channelHandle
            }.execute()?.items?.firstOrNull()?.id
        }
    }

    private val validatedChannelIdsCache = SizeLimitedMap<String, Boolean>(100)
    suspend fun validateChannelIdExists(channelId: String) = validatedChannelIdsCache.getOrPut(
        channelId
    ) {
        withContext(googleContext) {
            youtubeService().channels().list("snippet".l).apply {
                id = channelId.l
            }.execute()?.items?.isNotEmpty() == true
        }
    }


    /**
     * Generates an access token with the stored credentials
     * @return The access token
     */
    private suspend fun generateAccessToken(): String = withContext(googleContext) {
        GoogleRefreshTokenRequest(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            credentials.refreshtoken,
            credentials.clientid,
            credentials.clientsecret
        ).execute().also { universalLogger.info("GENERATEACCESSTOKEN ${it.expiresInSeconds} ${it.scope}") }.accessToken
    }
}
