package de.tectoast.emolga.utils

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.api.services.youtube.YouTube
import de.tectoast.emolga.utils.cache.MappedCache
import de.tectoast.emolga.utils.cache.TimedCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@Single
class Google(private val credentials: BotConfig.Google, val clock: Clock) {
    private val googleContext = Dispatchers.IO

    @OptIn(ExperimentalTime::class)
    private val accesstoken: TimedCache<String> = TimedCache(45.minutes, clock) { generateAccessToken() }
    private val sheetsService = MappedCache(accesstoken, clock) {
        Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(it)
        ).setApplicationName("emolga").build()
    }
    private val youtubeService = MappedCache(accesstoken, clock) {
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
     * Batch updates the specified data in the specified spreadsheet
     * @param sid The ID of the spreadsheet
     * @param data The data to update
     */
    suspend fun batchUpdate(sid: String, data: List<Request>) {
        withContext(googleContext) {
            sheetsService().spreadsheets().batchUpdate(sid, BatchUpdateSpreadsheetRequest().setRequests(data)).execute()
        }
    }

    /**
     * Batch updates the specified data in the specified spreadsheet
     * @param sid The ID of the spreadsheet
     * @param data The data to update
     * @param mode The mode of the update ("RAW" for unformatted, "USER_ENTERED" for formatted)
     */
    suspend fun batchUpdate(sid: String, data: List<ValueRange>, mode: String) {
        withContext(googleContext) {
            sheetsService().spreadsheets().values()
                .batchUpdate(sid, BatchUpdateValuesRequest().setData(data).setValueInputOption(mode)).execute()
        }
    }

    private val channelHandleIdCache = newThreadSafeCache<String, String?>(100)
    suspend fun fetchChannelId(channelHandle: String) = channelHandleIdCache.getOrPut(channelHandle) {
        withContext(googleContext) {
            youtubeService().channels().list(listOf("snippet")).apply {
                forHandle = channelHandle
            }.execute()?.items?.firstOrNull()?.id
        }
    }

    private val validatedChannelIdsCache = newThreadSafeCache<String, Boolean>(100)
    suspend fun validateChannelIdExists(channelId: String) = validatedChannelIdsCache.getOrPut(
        channelId
    ) {
        withContext(googleContext) {
            youtubeService().channels().list(listOf("snippet")).apply {
                id = listOf(channelId)
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
        ).execute().accessToken
    }
}
