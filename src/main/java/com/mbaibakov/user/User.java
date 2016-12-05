package com.mbaibakov.user;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private String name;
    private String color;

    public User(String name, String color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String toString() {
        try {
            return new JSONObject().put("name",name).put("color", color).toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
