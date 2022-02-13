package com.webrtc.signalingserver.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import java.util.HashMap;
import java.util.Map;

public class GsonUtil{

    private static final Gson gson = new Gson();

    public static <T> T decode(String s, Class<T> type){
        if(s == null) return null;
        return gson.fromJson(s, type);
    }

    public static <T> String encode(T request){
        if(request == null) return "";
        return gson.toJson(request);
    }

    private static JsonObject makeJson(Map<String, Object> format) {
        JsonObject jsonobject = new JsonObject();
        for (String key : format.keySet()) {
            if(format.get(key) instanceof String) jsonobject.addProperty(key, (String)format.get(key));
            if(format.get(key) instanceof Number) jsonobject.addProperty(key, (Number)format.get(key));
        }
        return jsonobject;
    }

    public static Map<String, Object> makeCommonMap(String type, Long userId, int status) {

        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("userId", userId);
        map.put("status", status);

        return map;
    }

    public static void commonSendMessage(WebSocket socket, Map<String, Object> keyValue) {
        JsonObject obj = makeJson(keyValue);

        try {
            socket.send(encode(obj));
        } catch (WebsocketNotConnectedException e) {
            System.out.println("e = " + e);
        }
    }

}
