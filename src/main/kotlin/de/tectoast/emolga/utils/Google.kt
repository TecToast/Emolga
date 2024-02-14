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
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes


object Google {
    private var REFRESHTOKEN: String? = null
    private var CLIENTID: String? = null
    private var CLIENTSECRET: String? = null
    private val googleContext = createCoroutineContext("Google", Dispatchers.IO)

    private val trustedTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val gsonFactory = GsonFactory.getDefaultInstance()
    private val authorizationHeaderAccessMethod = BearerToken.authorizationHeaderAccessMethod()

    private var accesstoken: TimedCache<String> = TimedCache(50.minutes) { generateAccessToken() }
    private val sheetsService = MappedCache(accesstoken) {
        Sheets.Builder(
            trustedTransport,
            gsonFactory,
            Credential(authorizationHeaderAccessMethod).setAccessToken(it)
        ).setApplicationName("emolga").build()
    }
    private val driveService = MappedCache(accesstoken) {
        Drive.Builder(
            trustedTransport,
            gsonFactory,
            Credential(authorizationHeaderAccessMethod).setAccessToken(it)
        ).setApplicationName("emolga").build()
    }

    fun setCredentials(refreshToken: String?, clientID: String?, clientSecret: String?) {
        REFRESHTOKEN = refreshToken
        CLIENTID = clientID
        CLIENTSECRET = clientSecret
    }

    suspend fun get(spreadsheetId: String, range: String, formula: Boolean): List<List<Any>> =
        withContext(googleContext) {
            sheetsService().spreadsheets()
                .values()[spreadsheetId, range].setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
                .execute().getValues()
        }

    suspend fun batchGet(sid: String, ranges: List<String>, formula: Boolean, majorDimension: String = "ROWS") =
        withContext(googleContext) {
            sheetsService().spreadsheets().values().batchGet(sid).setRanges(ranges).setMajorDimension(majorDimension)
                .setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
                .execute().valueRanges.map { it.getValues() }
        }

    suspend fun getSheetData(sid: String, ranges: List<String>) = withContext(googleContext) {
        sheetsService().spreadsheets()[sid].setIncludeGridData(true).setRanges(ranges).execute()
    }

    suspend fun batchUpdate(sid: String, data: List<ValueRange>, mode: String) = withContext(googleContext) {
        sheetsService().spreadsheets().values()
            .batchUpdate(sid, BatchUpdateValuesRequest().setData(data).setValueInputOption(mode)).execute()
    }

    suspend fun batchUpdate(sid: String, data: List<Request>) = withContext(googleContext) {
        sheetsService().spreadsheets().batchUpdate(sid, BatchUpdateSpreadsheetRequest().setRequests(data)).execute()
    }

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

    suspend fun generateAccessToken(): String = withContext(googleContext) {
        GoogleRefreshTokenRequest(
            trustedTransport,
            GsonFactory.getDefaultInstance(),
            REFRESHTOKEN,
            CLIENTID,
            CLIENTSECRET
        ).execute().accessToken
    }
}
