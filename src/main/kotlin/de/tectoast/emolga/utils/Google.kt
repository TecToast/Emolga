package de.tectoast.emolga.utils

import com.google.api.client.auth.oauth2.BearerToken
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest
import com.google.api.services.sheets.v4.model.Request
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.Playlist
import com.google.api.services.youtube.model.SearchResult
import com.google.api.services.youtube.model.Video
import net.dv8tion.jda.annotations.ReplaceWith
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException

object Google {
    private val logger = LoggerFactory.getLogger(Google::class.java)
    private var REFRESHTOKEN: String? = null
    private var CLIENTID: String? = null
    private var CLIENTSECRET: String? = null
    private var accesstoken: String? = null
    private var lastUpdate: Long = -1
    fun setCredentials(refreshToken: String?, clientID: String?, clientSecret: String?) {
        REFRESHTOKEN = refreshToken
        CLIENTID = clientID
        CLIENTSECRET = clientSecret
    }

    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun getVidByQuery(vid: String?): SearchResult? {
        try {
            return youTubeService.search().list(listOf("snippet")).setQ(vid).setMaxResults(1L).execute().items[0]
        } catch (ex: GeneralSecurityException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        logger.info("NULL")
        return null
    }

    @JvmStatic
    fun getVidByURL(url: String): Video? {
        try {
            val id =
                (if (url.contains("youtu.be")) url.substring("https://youtu.be/".length) else url.substring("https://www.youtube.com/watch?v=".length)).split(
                    "&".toRegex()
                ).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0]
            return youTubeService.videos().list(listOf("snippet")).setId(listOf(id)).setMaxResults(1L)
                .execute().items[0]
        } catch (ex: GeneralSecurityException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        logger.info("NULL")
        return null
    }

    @JvmStatic
    fun getPlaylistByURL(url: String): Playlist? {
        try {
            val id = url.substring("https://www.youtube.com/playlist?list=".length).split("&".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]
            return youTubeService.playlists().list(listOf("snippet")).setId(listOf(id)).setMaxResults(1L)
                .execute().items[0]
        } catch (ex: GeneralSecurityException) {
            ex.printStackTrace()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        logger.info("NULL")
        return null
    }

    @Throws(IllegalArgumentException::class)
    operator fun get(spreadsheetId: String?, range: String?, formula: Boolean): List<List<Any>>? {
        try {
            return sheetsService!!.spreadsheets()
                .values()[spreadsheetId, range].setValueRenderOption(if (formula) "FORMULA" else "FORMATTED_VALUE")
                .execute().getValues()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        logger.info("NULL")
        return null
    }

    @Deprecated("")
    @ReplaceWith("RequestBuilder.updateAll")
    @Throws(
        IllegalArgumentException::class
    )
    fun updateRequest(spreadsheetId: String?, range: String?, values: List<List<Any?>?>?, raw: Boolean) {
        try {
            sheetsService!!.spreadsheets().values().update(spreadsheetId, range, ValueRange().setValues(values))
                .setValueInputOption(if (raw) "RAW" else "USER_ENTERED").execute()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    @Deprecated("")
    @Throws(IllegalArgumentException::class)
    fun batchUpdateRequest(spreadsheetId: String?, request: Request) {
        try {
            sheetsService!!.spreadsheets().batchUpdate(
                spreadsheetId, BatchUpdateSpreadsheetRequest()
                    .setRequests(listOf(request))
            ).execute()
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
    }

    @JvmStatic
    val sheetsService: Sheets?
        get() {
            refreshTokenIfNotPresent()
            try {
                return Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(
                        accesstoken
                    )
                )
                    .setApplicationName("emolga")
                    .build()
            } catch (e: GeneralSecurityException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

    @get:Throws(GeneralSecurityException::class, IOException::class)
    private val youTubeService: YouTube
        get() {
            refreshTokenIfNotPresent()
            return YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(
                    accesstoken
                )
            )
                .setApplicationName("emolga")
                .build()
        }

    private fun refreshTokenIfNotPresent() {
        if (accesstoken == null || System.currentTimeMillis() - lastUpdate > 3000000) generateAccessToken()
    }

    fun generateAccessToken() {
        try {
            accesstoken = GoogleRefreshTokenRequest(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                REFRESHTOKEN,
                CLIENTID,
                CLIENTSECRET
            ).execute().accessToken
            lastUpdate = System.currentTimeMillis()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        }
    }
}