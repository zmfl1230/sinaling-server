package com.webrtc.signalingserver.repository;

import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MemorySessionRepository implements SessionRepository{
    // member id, member socket
    private final Map<String, WebSocket> connections = new HashMap<>();

    // lecture id, session participants
    private final Map<String, List<String>> sessionManager = new HashMap<>();

    // member id, message
    private final Map<String, String> messageOffer = new HashMap<>();

    @Override
    public Boolean containsKeyOnConnections(String key) {
        return connections.containsKey(key);
    }

    @Override
    public void closeConnection(String key) {
        connections.get(key).close();
    }

    @Override
    public WebSocket getWebSocketOnConnections(String key) {
        return connections.get(key);
    }

    @Override
    public void sendMessageUsingConnectionKey(String key, String message) {
        connections.get(key).send(message);
    }

    @Override
    public void removeMessageOnMessageOffer(String key) {
        messageOffer.remove(key);
    }

    @Override
    public void removeKeyOnConnections(String key) {
        connections.remove(key);
    }

    @Override
    public void addWebSocketOnConnections(String key, WebSocket socket) {
        connections.put(key, socket);
    }

    @Override
    public String getMessageOnMessageOffer(String key) {
        return messageOffer.get(key);
    }

    @Override
    public void addMessageOnMessageOffer(String key, String message) {
        messageOffer.put(key, message);
    }

    @Override
    public void removeSessionOnLecture(String lectureId, String targetToRemove) {
        sessionManager.get(lectureId).remove(targetToRemove);
    }

    @Override
    public List<String> getSessionsByLectureId(String lectureId) {
        return sessionManager.get(lectureId);
    }

    @Override
    public void addLectureSession(String lectureId) {
        sessionManager.put(lectureId, new LinkedList<>());
    }

    @Override
    public void removeLectureSessionByLectureId(String lectureId) {
        sessionManager.remove(lectureId);
    }

    @Override
    public void addSessionOnLecture(String lectureId, String targetToAdd) {
        sessionManager.get(lectureId).add(targetToAdd);
    }
}
