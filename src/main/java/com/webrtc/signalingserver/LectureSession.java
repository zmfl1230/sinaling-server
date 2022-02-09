package com.webrtc.signalingserver;

import com.google.gson.Gson;
import com.webrtc.signalingserver.domain.dto.LiveRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.*;

@Slf4j
public class LectureSession extends WebSocketServer {

    private Map<String, Method> methodMap = new HashMap<>();

    public LectureSession() {
        super(new InetSocketAddress(8888));
        log.info("소켓 시작");
    }

    public void setMethodMap(Map<String, Method> methodMap) {
        this.methodMap = methodMap;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("New client connected: " + conn.getRemoteSocketAddress() + " hash " + conn.getRemoteSocketAddress().hashCode());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            log.info(message);

            // Gson 객체 생성
            Gson gson = new Gson();
            LiveRequestDto messageObj = gson.fromJson(message, LiveRequestDto.class);

            if(messageObj.type.equalsIgnoreCase("sdp")) {
                // sdp [offer, answer]
                methodMap.get(messageObj.type).invoke(this, conn, messageObj, message);
            } else {
                // start, enter, exit
                methodMap.get(messageObj.type).invoke(this, conn, messageObj);
            }

        } catch (IllegalAccessException | InvocationTargetException | NullPointerException e) {
            log.info(e.getMessage());
        } finally {
            conn.close();
        }

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.info("client disconnected (code: {})", code);
    }

    @Override
    public void onError(WebSocket conn, Exception exc) {
        System.out.println("Error happened: " + exc);
    }

}