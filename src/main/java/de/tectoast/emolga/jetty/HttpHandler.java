package de.tectoast.emolga.jetty;

import de.tectoast.emolga.commands.Command;
import de.tectoast.emolga.utils.sql.DBManagers;
import de.tectoast.jsolf.JSONArray;
import de.tectoast.jsolf.JSONObject;
import de.tectoast.jsolf.JSONTokener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.tectoast.emolga.bot.EmolgaMain.emolgajda;
import static de.tectoast.emolga.utils.sql.DBManagers.DISCORD_AUTH;
import static net.dv8tion.jda.api.entities.User.DEFAULT_AVATAR_URL;

public class HttpHandler extends AbstractHandler {

    private static final OkHttpClient client = new OkHttpClient().newBuilder().build();

    private static final HashMap<String, JSONArray> guildCache = new HashMap<>();
    private static final HashMap<String, JSONObject> userCache = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    @Route(route = "/discordauth", needsCookie = false)
    public static void exchangeCodeRoute(String cookie, HttpServletRequest req, HttpServletResponse res) throws IOException {
        Map<String, String> map = getQueryMap(req.getQueryString());
        if (!map.containsKey("code")) {
            res.sendRedirect("http://localhost:4200");
        }
        String code = map.get("code");
        JSONObject tokens = exchangeCode(code);
        logger.info("tokens = " + tokens);
        JSONObject data = getUserInfo(tokens.getString("access_token"));
        logger.info("data = " + data);
        res.sendRedirect("https://emolga.epizy.com/?c=" + DISCORD_AUTH.generateCookie(tokens, Long.parseLong(data.getString("id"))));
        //.send(new JSONObject().put("key", "token").put("token", DISCORD_AUTH.generateCookie(tokens, Long.parseLong(data.getString("id")))).toString());
    }

    @Route(route = "/userdata")
    public static void userData(String cookie, HttpServletRequest req, HttpServletResponse res) throws SQLException, IOException {
        if (userCache.containsKey(cookie)) {
            res.getWriter().println(userCache.get(cookie));
            return;
        }
        ResultSet session = DISCORD_AUTH.getSessionByCookie(cookie);
        if (session.next()) {
            String token = getAccessToken(session);
            JSONObject userinfo = getUserInfo(token);
            JSONObject put = new JSONObject().put("name", userinfo.getString("username"));
            String avatarurl;
            if (userinfo.isNull("avatar")) {
                int dis = Integer.parseInt(userinfo.getString("discriminator"));
                avatarurl = String.format(DEFAULT_AVATAR_URL, dis % 5);
            } else {
                avatarurl = "https://cdn.discordapp.com/avatars/%s/%s"
                        .formatted(userinfo.getString("id"), userinfo.getString("avatar") + (userinfo.getString("avatar").startsWith("a_") ? ".gif" : ".png"));
            }
            JSONObject json = put.put("avatarurl", avatarurl);
            userCache.put(cookie, json);
            res.getWriter().println(json);
        } else {
            res.getWriter().println(new JSONObject().put("error", "invalidcookie").toString());
        }
    }

    @Route(route = "/guilds")
    public static void guilds(String cookie, HttpServletRequest req, HttpServletResponse res) throws SQLException, IOException {
        if (guildCache.containsKey(cookie)) {
            logger.info("FROM CACHE");
            res.getWriter().println(guildCache.get(cookie));
            return;
        }
        ResultSet session = DISCORD_AUTH.getSessionByCookie(cookie);
        if (session.next()) {
            String token = getAccessToken(session);
            JSONArray guilds = getGuilds(token);
            logger.info("token = " + token);
            List<String> gl = emolgajda.getGuilds().stream().map(Guild::getId).toList();
            JSONArray arr = new JSONArray();
            List<JSONObject> joined = new LinkedList<>();
            List<JSONObject> notJoined = new LinkedList<>();
            for (JSONObject json : guilds.toJSONList()) {
                JSONObject o = new JSONObject();
                if ((json.getLong("permissions") & Permission.MANAGE_SERVER.getRawValue()) > 0) {
                    o.put("id", json.getString("id"));
                    o.put("name", json.getString("name"));
                    o.put("url", json.isNull("icon") ? "assets/images/defaultservericon.png"
                            : "https://cdn.discordapp.com/icons/" + json.getLong("id") + "/" + json.getString("icon") + ".png");
                    boolean j = gl.contains(json.getString("id"));
                    o.put("joined", j);
                    if (j) joined.add(o);
                    else notJoined.add(o);
                }
            }
            joined.forEach(arr::put);
            notJoined.forEach(arr::put);
            guildCache.put(cookie, arr);
            res.getWriter().println(arr);
        } else {
            res.getWriter().println(new JSONObject().put("error", "invalidcookie").toString());
        }
    }

    @Route(route = "/guilddata/(\\d+)/banned")
    public static void bannedUsers(String cookie, HttpServletRequest req, HttpServletResponse res, List<String> args) throws SQLException, IOException {
        ResultSet session = DISCORD_AUTH.getSessionByCookie(cookie);
        if (session.next()) {
            long gid = Long.parseLong(args.get(0));
            Guild g = emolgajda.getGuildById(gid);
            if (g == null) {
                replyError(res, "No matching guild");
                return;
            }
            if (!g.retrieveMemberById(session.getLong("userid")).complete().hasPermission(Permission.MANAGE_SERVER)) {
                replyError(res, "No permission to view banned users for guild %s", g.getName());
                return;
            }
            res.getWriter().println(DBManagers.BAN.getBans(g));
        } else {
            res.getWriter().println(new JSONObject().put("error", "invalidcookie").toString());
        }
    }

    @Route(route = "/guilddata/(\\d+)/warned")
    public static void warnedUsers(String cookie, HttpServletRequest req, HttpServletResponse res, List<String> args) throws SQLException, IOException {
        ResultSet session = DISCORD_AUTH.getSessionByCookie(cookie);
        if (session.next()) {
            long gid = Long.parseLong(args.get(0));
            Guild g = emolgajda.getGuildById(gid);
            if (g == null) {
                replyError(res, "No matching guild");
                return;
            }
            if (!g.retrieveMemberById(session.getLong("userid")).complete().hasPermission(Permission.MANAGE_SERVER)) {
                replyError(res, "No permission to view warned users for guild %s", g.getName());
                return;
            }
            res.getWriter().println(DBManagers.WARNS.getWarns(g));
        } else {
            res.getWriter().println(new JSONObject().put("error", "invalidcookie").toString());
        }
    }

    public static JSONObject getBody(HttpServletRequest req) {
        try {
            return new JSONObject(new JSONTokener(req.getReader()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Route(route = "/unban", method = "POST")
    public static void unban(String cookie, HttpServletRequest req, HttpServletResponse res) throws SQLException, IOException {
        ResultSet session = DISCORD_AUTH.getSessionByCookie(cookie);
        if (session.next()) {
            JSONObject body = getBody(req);
            long gid = Long.parseLong(body.getString("guild"));
            Guild g = emolgajda.getGuildById(gid);
            if (g == null) {
                replyError(res, "No matching guild");
                return;
            }
            if (!g.retrieveMemberById(session.getLong("userid")).complete().hasPermission(Permission.MANAGE_SERVER)) {
                replyError(res, "No permission to unban users for guild %s", g.getName());
                return;
            }
            res.getWriter().println(DBManagers.BAN.unban(g, Long.parseLong(body.getString("member"))));
        } else {
            res.getWriter().println(new JSONObject().put("error", "invalidcookie").toString());
        }
    }

    public static void replyError(HttpServletResponse res, String msg, Object... args) throws IOException {
        res.getWriter().println(new JSONObject().put("error", msg.formatted(args)));
    }

    public static Map<String, String> getQueryMap(String query) {
        HashMap<String, String> map = new HashMap<>();
        if (query == null) return map;
        for (String s : query.split("&")) {
            String[] sp = s.split("=");
            map.put(sp[0], sp[1]);
        }
        return map;
    }

    private static String getAccessToken(ResultSet set) throws SQLException, IOException {
        Timestamp tokenexpires = set.getTimestamp("tokenexpires");
        if (tokenexpires.toInstant().isAfter(Instant.now())) {
            return set.getString("lastAccesstoken");
        }
        String refreshtoken = set.getString("refreshtoken");
        System.out.printf("Requesting new AccessToken with Refreshtoken %s%n", refreshtoken);
        JSONObject res = refreshToken(refreshtoken);
        logger.info("res.toString(4) = " + res.toString(4));
        String access = res.getString("access_token");
        set.updateString("lastAccesstoken", access);
        set.updateTimestamp("tokenexpires", new Timestamp(System.currentTimeMillis() + res.getInt("expires_in")));
        set.updateRow();
        return access;
    }

    private static JSONArray getGuilds(String token) throws IOException {
        String arr = client.newCall(new okhttp3.Request.Builder()
                .url("https://discordapp.com/api/users/@me/guilds")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build()).execute().body().string();
        logger.info("arr = " + arr);
        return new JSONArray(arr);
    }

    private static JSONObject getUserInfo(String token) throws IOException {
        return new JSONObject(client.newCall(new okhttp3.Request.Builder()
                .url("https://discordapp.com/api/users/@me")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build()).execute().body().string());
    }

    private static JSONObject exchangeCode(String code) throws IOException {
        return new JSONObject(client.newCall(new okhttp3.Request.Builder()
                .url("https://discord.com/api/v8/oauth2/token")
                .method("POST", new FormBody.Builder()
                        .add("client_id", "723829878755164202")
                        .add("client_secret", Command.tokens.getJSONObject("oauth2").getString("clientsecret"))
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", "https://florixserver.selfhost.eu:51216/api/discordauth")
                        .build())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()).execute().body().string());
    }

    private static JSONObject refreshToken(String refreshToken) throws IOException {
        return new JSONObject(client.newCall(new okhttp3.Request.Builder()
                .url("https://discord.com/api/v8/oauth2/token")
                .method("POST", new FormBody.Builder()
                        .add("client_id", "723829878755164202")
                        .add("client_secret", Command.tokens.getJSONObject("oauth2").getString("clientsecret"))
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .build())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()).execute().body().string());
    }


    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setStatus(200);
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, *");
        if (request.getMethod().equals("OPTIONS")) {
            response.setStatus(204);
            baseRequest.setHandled(true);
            return;
        }
        String auth = request.getHeader("Authorization");
        String path = target.substring("/api".length());
        logger.info("target = " + target);
        logger.info("auth = " + auth);
        for (Method method : HttpHandler.class.getDeclaredMethods()) {
            Route a = method.getAnnotation(Route.class);
            if (a == null) continue;
            Matcher matcher = Pattern.compile(a.route()).matcher(path);
            if (matcher.find() && baseRequest.getMethod().equals(a.method())) {
                baseRequest.setHandled(true);
                if (a.needsCookie() && (auth == null || auth.isEmpty())) {
                    response.getWriter().println(new JSONObject().put("error", "Missing authorization"));
                    return;
                }
                List<String> l = new ArrayList<>(matcher.groupCount());
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    l.add(matcher.group(i));
                }
                try {
                    if (l.size() > 0) method.invoke(null, auth, request, response, l);
                    else
                        method.invoke(null, auth, request, response);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

}
