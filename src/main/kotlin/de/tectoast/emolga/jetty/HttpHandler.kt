package de.tectoast.emolga.jetty

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.sql.managers.BanManager
import de.tectoast.emolga.utils.sql.managers.DiscordAuthManager
import de.tectoast.emolga.utils.sql.managers.WarnsManager
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONObject
import de.tectoast.jsolf.JSONTokener
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import okhttp3.FormBody
import okhttp3.OkHttpClient
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import java.util.function.Consumer
import java.util.regex.Pattern

class HttpHandler : AbstractHandler() {
    @Throws(IOException::class)
    override fun handle(
        target: String,
        baseRequest: Request,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        response.contentType = "application/json"
        response.status = 200
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Headers", "Authorization, *")
        if (request.method == "OPTIONS") {
            response.status = 204
            baseRequest.isHandled = true
            return
        }
        val auth = request.getHeader("Authorization")
        val path = target.substring("/api".length)
        logger.info("target = $target")
        logger.info("auth = $auth")
        for (method in HttpHandler::class.java.declaredMethods) {
            val a = method.getAnnotation(Route::class.java) ?: continue
            val matcher = Pattern.compile(a.route).matcher(path)
            if (matcher.find() && baseRequest.method == a.method) {
                baseRequest.isHandled = true
                if (a.needsCookie && (auth == null || auth.isEmpty())) {
                    response.writer.println(JSONObject().put("error", "Missing authorization"))
                    return
                }
                val l: MutableList<String> = ArrayList(matcher.groupCount())
                for (i in 1..matcher.groupCount()) {
                    l.add(matcher.group(i))
                }
                try {
                    method.invoke(null, Data(auth ?: "", request, response, l))
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
                break
            }
        }
    }

    data class Data(
        val cookie: String,
        val req: HttpServletRequest,
        val res: HttpServletResponse,
        val args: List<String>
    )

    companion object {
        private val client = OkHttpClient().newBuilder().build()
        private val guildCache = HashMap<String, JSONArray>()
        private val userCache = HashMap<String, JSONObject>()
        private val logger = LoggerFactory.getLogger(HttpHandler::class.java)

        @Route(route = "/discordauth", needsCookie = false)
        @JvmStatic
        @Throws(IOException::class)
        fun exchangeCodeRoute(dt: Data) {
            val (_, req, res, _) = dt
            val map = getQueryMap(req.queryString)
            if (!map.containsKey("code")) {
                res.sendRedirect("http://localhost:4200")
                return
            }
            val code = map["code"]!!
            val tokens = exchangeCode(code)
            logger.info("tokens = $tokens")
            val data = getUserInfo(tokens.getString("access_token"))
            logger.info("data = $data")
            res.sendRedirect(
                "https://emolga.epizy.com/?c=" + DiscordAuthManager.generateCookie(
                    tokens,
                    data.getString("id").toLong()
                )
            )
            //.send(new JSONObject().put("key", "token").put("token", DISCORD_AUTH.generateCookie(tokens, Long.parseLong(data.getString("id")))).toString());
        }

        @Route(route = "/userdata")
        @JvmStatic
        @Throws(SQLException::class, IOException::class)
        fun userData(dt: Data) {
            val (cookie, _, res, _) = dt
            if (userCache.containsKey(cookie)) {
                res.writer.println(userCache[cookie])
                return
            }
            val session = DiscordAuthManager.getSessionByCookie(cookie)
            if (session.next()) {
                val token = getAccessToken(session)
                val userinfo = getUserInfo(token)
                val put = JSONObject().put("name", userinfo.getString("username"))
                val avatarurl: String = if (userinfo.isNull("avatar")) {
                    val dis = userinfo.getString("discriminator").toInt()
                    String.format(User.DEFAULT_AVATAR_URL, dis % 5)
                } else {
                    "https://cdn.discordapp.com/avatars/${userinfo.getString("id")}/${
                        userinfo.getString("avatar") + if (userinfo.getString("avatar")
                                .startsWith("a_")
                        ) ".gif" else ".png"
                    }"
                }
                val json = put.put("avatarurl", avatarurl)
                userCache[cookie] = json
                res.writer.println(json)
            } else {
                res.writer.println(JSONObject().put("error", "invalidcookie").toString())
            }
        }

        @Route(route = "/guilds")
        @JvmStatic
        @Throws(SQLException::class, IOException::class)
        fun guilds(dt: Data) {
            val (cookie, _, res, _) = dt
            if (guildCache.containsKey(cookie)) {
                logger.info("FROM CACHE")
                res.writer.println(guildCache[cookie])
                return
            }
            val session = DiscordAuthManager.getSessionByCookie(cookie)
            if (session.next()) {
                val token = getAccessToken(session)
                val guilds = getGuilds(token)
                logger.info("token = $token")
                val gl: List<String> = emolgajda.guilds.map { it.id }
                val arr = JSONArray()
                val joined: MutableList<JSONObject> = LinkedList()
                val notJoined: MutableList<JSONObject> = LinkedList()
                for (json in guilds.toJSONList()) {
                    val o = JSONObject()
                    if (json.getLong("permissions") and Permission.MANAGE_SERVER.rawValue > 0) {
                        o.put("id", json.getString("id"))
                        o.put("name", json.getString("name"))
                        o.put(
                            "url",
                            if (json.isNull("icon")) "assets/images/defaultservericon.png" else "https://cdn.discordapp.com/icons/" + json.getLong(
                                "id"
                            ) + "/" + json.getString("icon") + ".png"
                        )
                        val j = gl.contains(json.getString("id"))
                        o.put("joined", j)
                        if (j) joined.add(o) else notJoined.add(o)
                    }
                }
                joined.forEach(Consumer { value: JSONObject? -> arr.put(value) })
                notJoined.forEach(Consumer { value: JSONObject? -> arr.put(value) })
                guildCache[cookie] = arr
                res.writer.println(arr)
            } else {
                res.writer.println(JSONObject().put("error", "invalidcookie").toString())
            }
        }

        @Route(route = "/guilddata/(\\d+)/banned")
        @JvmStatic
        @Throws(SQLException::class, IOException::class)
        fun bannedUsers(dt: Data) {
            val (cookie, _, res, args) = dt
            val session = DiscordAuthManager.getSessionByCookie(cookie)
            if (session.next()) {
                val gid = args[0].toLong()
                val g: Guild? = emolgajda.getGuildById(gid)
                if (g == null) {
                    replyError(res, "No matching guild")
                    return
                }
                if (!g.retrieveMemberById(session.getLong("userid")).complete()
                        .hasPermission(Permission.MANAGE_SERVER)
                ) {
                    replyError(res, "No permission to view banned users for guild %s", g.name)
                    return
                }
                res.writer.println(BanManager.getBans(g))
            } else {
                res.writer.println(JSONObject().put("error", "invalidcookie").toString())
            }
        }

        @Route(route = "/guilddata/(\\d+)/warned")
        @JvmStatic
        @Throws(SQLException::class, IOException::class)
        fun warnedUsers(dt: Data) {
            val (cookie, _, res, args) = dt
            val session = DiscordAuthManager.getSessionByCookie(cookie)
            if (session.next()) {
                val gid = args[0].toLong()
                val g: Guild? = emolgajda.getGuildById(gid)
                if (g == null) {
                    replyError(res, "No matching guild")
                    return
                }
                if (!g.retrieveMemberById(session.getLong("userid")).complete()
                        .hasPermission(Permission.MANAGE_SERVER)
                ) {
                    replyError(res, "No permission to view warned users for guild %s", g.name)
                    return
                }
                res.writer.println(WarnsManager.getWarns(g))
            } else {
                res.writer.println(JSONObject().put("error", "invalidcookie").toString())
            }
        }

        private fun getBody(req: HttpServletRequest): JSONObject? {
            try {
                return JSONObject(JSONTokener(req.reader))
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return null
        }

        @Route(route = "/unban", method = "POST")
        @JvmStatic
        @Throws(SQLException::class, IOException::class)
        fun unban(dt: Data) {
            val (cookie, req, res, _) = dt
            val session = DiscordAuthManager.getSessionByCookie(cookie)
            if (session.next()) {
                val body = getBody(req)
                val gid = body!!.getString("guild").toLong()
                val g: Guild? = emolgajda.getGuildById(gid)
                if (g == null) {
                    replyError(res, "No matching guild")
                    return
                }
                if (!g.retrieveMemberById(session.getLong("userid")).complete()
                        .hasPermission(Permission.MANAGE_SERVER)
                ) {
                    replyError(res, "No permission to unban users for guild %s", g.name)
                    return
                }
                res.writer.println(BanManager.unbanWebsite(g, body.getString("member").toLong()))
            } else {
                res.writer.println(JSONObject().put("error", "invalidcookie").toString())
            }
        }

        @Throws(IOException::class)
        fun replyError(res: HttpServletResponse, msg: String, vararg args: Any?) {
            res.writer.println(JSONObject().put("error", msg.format(*args)))
        }

        private fun getQueryMap(query: String?): Map<String, String> {
            val map = HashMap<String, String>()
            if (query == null) return map
            for (s in query.split("&")) {
                val sp = s.split("=")
                map[sp[0]] = sp[1]
            }
            return map
        }

        @Throws(SQLException::class, IOException::class)
        private fun getAccessToken(set: ResultSet): String {
            val tokenexpires = set.getTimestamp("tokenexpires")
            if (tokenexpires.toInstant().isAfter(Instant.now())) {
                return set.getString("lastAccesstoken")
            }
            val refreshtoken = set.getString("refreshtoken")
            System.out.printf("Requesting new AccessToken with Refreshtoken %s%n", refreshtoken)
            val res = refreshToken(refreshtoken)
            logger.info("res.toString(4) = " + res.toString(4))
            val access = res.getString("access_token")
            set.updateString("lastAccesstoken", access)
            set.updateTimestamp("tokenexpires", Timestamp(System.currentTimeMillis() + res.getInt("expires_in")))
            set.updateRow()
            return access
        }

        @Throws(IOException::class)
        private fun getGuilds(token: String): JSONArray {
            val res = client.newCall(
                okhttp3.Request.Builder()
                    .url("https://discordapp.com/api/users/@me/guilds")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            ).execute()
            val arr = res.body!!.string()
            res.close()
            logger.info("arr = $arr")
            return JSONArray(arr)
        }

        @Throws(IOException::class)
        private fun getUserInfo(token: String): JSONObject {
            val res = client.newCall(
                okhttp3.Request.Builder()
                    .url("https://discordapp.com/api/users/@me")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            ).execute()
            val respStr = res.body!!.string()
            res.close()
            return JSONObject(respStr)
        }

        @Throws(IOException::class)
        private fun exchangeCode(code: String): JSONObject {
            val res = client.newCall(
                okhttp3.Request.Builder()
                    .url("https://discord.com/api/v8/oauth2/token")
                    .method(
                        "POST", FormBody.Builder()
                            .add("client_id", "723829878755164202")
                            .add("client_secret", Command.tokens.getJSONObject("oauth2").getString("clientsecret"))
                            .add("grant_type", "authorization_code")
                            .add("code", code)
                            .add("redirect_uri", "https://florixserver.selfhost.eu:51216/api/discordauth")
                            .build()
                    )
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
            ).execute()
            val respStr = res.body!!.string()
            res.close()
            return JSONObject(respStr)
        }

        @Throws(IOException::class)
        private fun refreshToken(refreshToken: String): JSONObject {
            val res = client.newCall(
                okhttp3.Request.Builder()
                    .url("https://discord.com/api/v8/oauth2/token")
                    .method(
                        "POST", FormBody.Builder()
                            .add("client_id", "723829878755164202")
                            .add("client_secret", Command.tokens.getJSONObject("oauth2").getString("clientsecret"))
                            .add("grant_type", "refresh_token")
                            .add("refresh_token", refreshToken)
                            .build()
                    )
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build()
            ).execute()
            val respStr = res.body!!.string()
            res.close()
            return JSONObject(respStr)
        }
    }
}