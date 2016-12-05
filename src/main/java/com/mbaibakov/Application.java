package com.mbaibakov;

import com.mbaibakov.chat.ChatWebSocketHandler;
import com.mbaibakov.session.AuthorizedUsersHolder;
import com.mbaibakov.user.User;
import com.mbaibakov.utils.ViewUtils;
import org.eclipse.jetty.util.StringUtil;
import spark.ModelAndView;

import java.util.HashMap;
import java.util.Map;

import static com.mbaibakov.utils.Const.*;
import static spark.Spark.*;

public class Application {

    public static void main(String[] args) {
        staticFiles.location("/static");
        staticFiles.expireTime(600);

        webSocket(WS_CHAT_URL, ChatWebSocketHandler.class);
        init();

        //chat page
        get(CHATROOM_URL, (request, response) -> {
            User currentUser = request.session().attribute("currentUser");
            if (currentUser == null) {
                response.redirect(LOGIN_URL);
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            model.put("currentUser", currentUser);
            // The wm files are located under the resources directory
            return new ModelAndView(model, "velocity/chatroom.html");
        }, ViewUtils.strictVelocityEngine());

        //login page
        get(LOGIN_URL, (request, response) -> {
            if (request.session().attribute("currentUser") != null) {
                response.redirect(CHATROOM_URL);
                return null;
            }
            Map<String, Object> model = new HashMap<>();
            model.put("error", request.queryParams("error"));
            return new ModelAndView(model, "velocity/login.html");
        }, ViewUtils.strictVelocityEngine());

        //login controller
        post(LOGIN_URL, ((request, response) -> {
            String name = request.queryParams("name");
            if (StringUtil.isNotBlank(name)) {
                String color = request.queryParams("color");
                User user = new User(name, color);
                request.session().attribute("currentUser", user);
                AuthorizedUsersHolder.put(request, user);
                response.redirect(CHATROOM_URL);
            } else {
                response.redirect(LOGIN_URL + "?error=name");
            }
            return null;
        }));

        //logout controller
        get(LOGOUT_URL, ((request, response) -> {
            AuthorizedUsersHolder.remove(request.session().attribute("currentUser"));
            request.session().removeAttribute("currentUser");
            response.redirect(LOGIN_URL);
            return null;
        }));

        //redirect
        get("/",  (request, response) -> {
            response.redirect(LOGIN_URL);
            return null;
        });
    }
}
