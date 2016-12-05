package com.mbaibakov.chat;

import com.mbaibakov.session.AuthorizedUsersHolder;
import com.mbaibakov.user.User;
import com.mbaibakov.utils.Const;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpCookie;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static j2html.TagCreator.*;

@WebSocket
public class ChatWebSocketHandler {
    public static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static Map<Session, User> userSessions = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) throws Exception {
        User user = resolveUser(session);
        if (user != null) {
            userSessions.put(session, user);
            broadcastMessage(user, null);
            log.info("{} joined the chat", user.getName());
        } else {
            log.warn("Can not find user");
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        User user = userSessions.get(session);
        if (user != null) {
            userSessions.remove(session);
            //update user list
            broadcastMessage(user, null);

        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        User user = userSessions.get(session);
        broadcastMessage(user, message);
    }

    //Sends a message to all users and list of online users
    public static void broadcastMessage(User sender, String message) {
        userSessions.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                JSONObject json = new JSONObject().put("userlist", getUserList());
                //if message is null we need update list of online users only
                if (message != null) {
                    json.put("userMessage", createHtmlMessage(sender, message));
                }
                session.getRemote().sendString(json.toString());
            } catch (Exception e) {
                log.error("Error while sending message.", e);
            }
        });
    }

    private static JSONArray getUserList(){
        JSONArray array = new JSONArray();
        for(User user : userSessions.values()){
            array.put(new JSONObject(user));
        }
        return array;
    }

    //Builds a HTML element with a sender-name, a message, and a timestamp
    private static String createHtmlMessage(User user, String message) {
        return article().withClass(user.getColor()).with(
                b(user.getName() + ":"),
                p(message),
                span().withClass("timestamp").withText(DATE_FORMAT.format(new Date()))
        ).render();
    }

    //resolve current user for websocket session
    public static User resolveUser(Session session) {
        String jsessionid = getJsessionid(session);
        if (jsessionid == null) {
            return null;
        }
        return AuthorizedUsersHolder.getByJsessionid(jsessionid);
    }

    //find jsessesionid cookie value from websocket session
    private static String getJsessionid(Session session) {
        for (HttpCookie cookie : session.getUpgradeRequest().getCookies()) {
            if (Const.JSESSIONID.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
