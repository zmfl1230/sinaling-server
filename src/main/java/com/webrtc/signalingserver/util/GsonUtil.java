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

    public static void commonSendMessage(WebSocket socket, Long userId, String message) {

        System.out.println("socket = " + socket.getLocalSocketAddress());
        System.out.println("socket = " + socket.getRemoteSocketAddress());
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("message", message);
        jsonobject.addProperty("requester", userId);

        try {
            socket.send(GsonUtil.encode(jsonobject));
        } catch (WebsocketNotConnectedException e) {
            System.out.println("e = " + e);
        }
    }

}
