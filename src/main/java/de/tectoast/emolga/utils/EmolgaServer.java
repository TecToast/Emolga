package de.tectoast.emolga.utils;

/*import de.tectoast.emolga.bot.EmolgaMain;
import de.tectoast.emolga.commands.Command;
import net.dv8tion.jda.api.entities.Guild;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jsolf.JSONArray;
import org.jsolf.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static de.tectoast.emolga.utils.sql.DBManagers.DISCORD_AUTH;

public class EmolgaServer extends WebSocketServer {

    private static final OkHttpClient client = new OkHttpClient().newBuilder().build();
    public static EmolgaServer INSTANCE;

    public EmolgaServer() {
        super(new InetSocketAddress(51216));
        INSTANCE = this;
        //setWebSocketFactory(new DefaultSSLWebSocketServerFactory(getContext()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }));
        try {
            for (int i = 1; i <= 3; i++) {
                new Socket("localhost", 51216);
                System.out.println("Failed try " + i);
                Thread.sleep(1000 * i);
            }
        } catch (Exception e) {
            start();
        }
    }



    private static String getAccessToken(ResultSet set) throws SQLException, IOException {
        Timestamp tokenexpires = set.getTimestamp("tokenexpires");
        if (tokenexpires.toInstant().isAfter(Instant.now())) {
            return set.getString("lastAccesstoken");
        }
        JSONObject res = refreshToken(set.getString("refreshtoken"));
        String access = res.getString("access_token");
        set.updateString("lastAccesstoken", access);
        set.updateTimestamp("tokenexpires", new Timestamp(System.currentTimeMillis() + res.getInt("expires_in")));
        set.updateRow();
        return access;
    }

    private static JSONArray getGuilds(String token) throws IOException {
        return new JSONArray(client.newCall(new Request.Builder()
                .url("https://discordapp.com/api/users/@me/guilds")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build()).execute().body().string());
    }

    private static JSONObject getUserInfo(String token) throws IOException {
        return new JSONObject(client.newCall(new Request.Builder()
                .url("https://discordapp.com/api/users/@me")
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build()).execute().body().string());
    }

    private static JSONObject exchangeCode(String code) throws IOException {
        return new JSONObject(client.newCall(new Request.Builder()
                .url("https://discord.com/api/v8/oauth2/token")
                .method("POST", new FormBody.Builder()
                        .add("client_id", "723829878755164202")
                        .add("client_secret", Command.tokens.getJSONObject("oauth2").getString("clientsecret"))
                        .add("grant_type", "authorization_code")
                        .add("code", code)
                        .add("redirect_uri", "https://emolga.epizy.com/discordauth")
                        .build())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()).execute().body().string());
    }

    private static JSONObject refreshToken(String refreshToken) throws IOException {
        return new JSONObject(client.newCall(new Request.Builder()
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
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {

    }

    @Override
    public void onMessage(WebSocket conn, String msg) {
        try {
            String arg = msg.split(";")[1];
            if (msg.startsWith("CODE;")) {

                /*String obj = new JSONObject()
                        .put("client_id", "723829878755164202")
                        .put("client_secret", Command.tokens.getJSONObject("oauth2").getString("clientsecret"))
                        .put("grant_type", "authorization_code")
                        .put("code", arg)
                        .put("redirect_uri", "https://emolga.epizy.com/discordauth")
                        .toString();

                JSONObject tokens = exchangeCode(arg);
                System.out.println("tokens = " + tokens);
                JSONObject data = getUserInfo(tokens.getString("access_token"));
                System.out.println("data = " + data);
                conn.send(new JSONObject().put("key", "token").put("token", DISCORD_AUTH.generateCookie(tokens, Long.parseLong(data.getString("id")))).toString());
            } else if (msg.startsWith("CONNECT")) {
                ResultSet session = DISCORD_AUTH.getSessionByCookie(arg);
                if (session.next()) {
                    String token = getAccessToken(session);
                    JSONObject userinfo = getUserInfo(token);
                    String avatar = userinfo.getString("avatar");
                    conn.send(new JSONObject().put("key", "name").put("username", userinfo.getString("username")).put("avatarurl", "https://cdn.discordapp.com/avatars/%s/%s"
                            .formatted(userinfo.getString("id"), avatar + (avatar.startsWith("a_") ? ".gif" : ".png"))).toString());
                    if (msg.startsWith("CONNECTWG")) {
                        JSONArray guilds = getGuilds(token);
                        System.out.println("token = " + token);
                        List<String> gl = EmolgaMain.emolgajda.getGuilds().stream().map(Guild::getId).collect(Collectors.toList());
                        JSONArray arr = new JSONArray();
                        List<JSONObject> joined = new LinkedList<>();
                        List<JSONObject> notJoined = new LinkedList<>();
                        for (JSONObject json : guilds.toJSONList()) {
                            JSONObject o = new JSONObject();
                            o.put("id", json.getString("id"));
                            o.put("name", json.getString("name"));
                            o.put("url", json.isNull("icon") ? "https://cdn.discordapp.com/embed/avatars/0.png"
                                    : "https://cdn.discordapp.com/icons/" + json.getLong("id") + "/" + json.getString("icon") + ".png");
                            boolean j = gl.contains(json.getString("id"));
                            o.put("joined", j);
                            if(j) joined.add(o);
                            else notJoined.add(o);
                        }
                        joined.forEach(arr::put);
                        notJoined.forEach(arr::put);
                        conn.send(new JSONObject().put("key", "guildlist").put("guilds", arr).toString());
                    }
                } else {
                    conn.send(new JSONObject().put("key", "invalidcookie").toString());
                }
            } else if (msg.startsWith("DELETECOOKIE;")) {
                DISCORD_AUTH.deleteCookie(arg);
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

    }

    @Override
    public void onStart() {
        System.out.println("Successfully started EmolgaServer!");
    }


}
*/