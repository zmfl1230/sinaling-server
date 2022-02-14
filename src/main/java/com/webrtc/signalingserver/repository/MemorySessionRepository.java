package com.webrtc.signalingserver.repository;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemorySessionRepository implements SessionRepository{
    //lecture id, waiting session participants
    private final ConcurrentMap<String, List<WebSocket>> waitingRoom = new ConcurrentHashMap<>();

    // member id, member socket
    private final ConcurrentMap<String, WebSocket> connections = new ConcurrentHashMap<>();

    // lecture id, session participants
    private final ConcurrentMap<String, List<String>> sessionManager = new ConcurrentHashMap<>();

    // member id, message
    private final ConcurrentMap<String, String> messageOffer = new ConcurrentHashMap<>();

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
        try {
            connections.get(key).send(message);
        } catch (WebsocketNotConnectedException e) {
            System.out.println("e = " + e);
        }
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
    public Boolean containsKeyOnMessageOffer(String key) {
        return messageOffer.containsKey(key);
    }

    @Override
    public void removeSessionOnLecture(String lectureId, String targetToRemove) {
        sessionManager.get(lectureId).remove(targetToRemove);
    }

    @Override
    public Boolean containsLectureSessionOnSessionManager(String key) {
        return sessionManager.containsKey(key);
    }

    @Override
    public Boolean containsConnectionOnLectureSession(String lectureId, String key) {
        return sessionManager.get(lectureId).contains(key);
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

    @Override
    public Boolean containsKeyOnWaitingRoom(String key) {
        return waitingRoom.containsKey(key);
    }

    @Override
    public void addConnectionOnWaitingRoom(String key, WebSocket connection) {
        waitingRoom.get(key).add(connection);
    }

    @Override
    public List<WebSocket> getConnectionsOnWaitingRoom(String key) {
        return waitingRoom.get(key);
    }

    @Override
    public void removeKeyOnWaitingRoom(String key) {
        waitingRoom.remove(key);
    }

    @Override
    public void createWaitingRoomByLectureId(String key) {
        waitingRoom.put(key, new LinkedList<>());
    }
}
