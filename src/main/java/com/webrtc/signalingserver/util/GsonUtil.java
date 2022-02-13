package com.webrtc.signalingserver.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

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

    private static JsonObject makeJson(String type, Long userId, int status) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("type", type);
        jsonobject.addProperty("status", status);
        jsonobject.addProperty("requester", userId);

        return jsonobject;
    }

    public static void commonSendMessage(WebSocket socket, String type, Long userId, int status) {
        JsonObject obj = makeJson(type, userId, status);

        try {
            socket.send(encode(obj));
        } catch (WebsocketNotConnectedException e) {
            System.out.println("e = " + e);
        }
    }

    public static void sendLiveStatusMessage(WebSocket socket, String type, Long userId,
                                              int status, boolean proceeding){
        JsonObject obj = makeJson(type, userId, status);
        obj.addProperty("proceeding", proceeding);

        try {
            socket.send(GsonUtil.encode(obj));
        } catch (WebsocketNotConnectedException e) {
            System.out.println("e = " + e);
        }
    }

}
