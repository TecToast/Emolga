package de.tectoast.emolga.utils

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.Permission
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.*
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes


object Google {
    private var REFRESHTOKEN: String? = null
    private var CLIENTID: String? = null
    private var CLIENTSECRET: String? = null
    private val googleContext = createCoroutineContext("Google", Dispatchers.IO)

    private val accesstoken: TimedCache<String> = TimedCache(45.minutes) { generateAccessToken() }
    private val sheetsService = MappedCache(accesstoken) {
        Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(it)
        ).setApplicationName("emolga").build()
    }
    private val driveService = MappedCache(accesstoken) {
        Drive.Builder(
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
     * Set the credentials for the Google API
     * @param refreshToken The refresh token
     * @param clientID The client ID
     * @param clientSecret The client secret
     */
    fun setCredentials(refreshToken: String, clientID: String, clientSecret: String) {
        REFRESHTOKEN = refreshToken
        CLIENTID = clientID
        CLIENTSECRET = clientSecret
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
        spreadsheetId: String,
        range: String,
        formula: Boolean,
        majorDimension: String = "ROWS"
    ): List<List<Any>> =
        withContext(googleContext) {
            sheetsService().spreadsheets()
                .values()[spreadsheetId, range].setMajorDimension(majorDimension)
                .setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
                .execute().getValues()
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
        sid: String,
        ranges: List<String>,
        formula: Boolean,
        majorDimension: String = "ROWS"
    ): List<List<List<Any>>> =
        withContext(googleContext) {
            sheetsService().spreadsheets().values().batchGet(sid).setRanges(ranges).setMajorDimension(majorDimension)
                .setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
                .execute().valueRanges.map { it.getValues() }
        }

    /**
     * Gets the full data from the specified ranges of the specified spreadsheet
     * @param sid The ID of the spreadsheet
     * @param ranges The ranges of the data
     * @return The data as Spreadsheet
     */
    suspend fun getSheetData(sid: String, ranges: List<String>): Spreadsheet = withContext(googleContext) {
        sheetsService().spreadsheets()[sid].setIncludeGridData(true).setRanges(ranges).execute()
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
     * Uploads the specified file to the specified folder in Google Drive
     * @param parent The ID of the folder
     * @param name The name of the file
     * @param mimeType The MIME type of the file
     * @param data The data of the file
     * @return The ID of the file
     */
    suspend fun uploadFileToDrive(parent: String, name: String, mimeType: String, data: ByteArray): String =
        withContext(googleContext) {
            val fileId = driveService().files().create(
                File().setParents(listOf(parent)).setName(name),
                ByteArrayContent(
                    mimeType,
                    data
                )
            ).setUploadType("media").setUseContentAsIndexableText(false).execute().id
            driveService().permissions().create(fileId, Permission().setType("anyone").setRole("reader")).execute()
            fileId
        }

    /**
     * Fetches the last video from the specified channel
     * @param channelId The ID of the channel
     * @return The search result of last video from the channel or null if there wasn't any
     */
    suspend fun fetchLastVideoFromChannel(channelId: String): SearchResult? =
        fetchLatestVideosFromChannel(channelId).firstOrNull()

    suspend fun fetchLatestVideosFromChannel(channelId: String): List<SearchResult> = withContext(googleContext) {
        youtubeService().search().list("snippet".l).apply {
            this.channelId = channelId
            order = "date"
            type = listOf("video")
        }.execute().items
    }

    suspend fun fetchChannelId(channelHandle: String) = withContext(googleContext) {
        youtubeService().channels().list("snippet".l).apply {
            forHandle = channelHandle
        }.execute()?.items?.firstOrNull()?.id
    }

    /**
     * Generates an access token with the stored credentials
     * @return The access token
     * @see setCredentials
     */
    private suspend fun generateAccessToken(): String = withContext(googleContext) {
        val accessToken = GoogleRefreshTokenRequest(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            REFRESHTOKEN,
            CLIENTID,
            CLIENTSECRET
        ).execute().also { universalLogger.info("GENERATEACCESSTOKEN ${it.expiresInSeconds} ${it.scope}") }.accessToken
        delay(5000)
        accessToken
    }
}
