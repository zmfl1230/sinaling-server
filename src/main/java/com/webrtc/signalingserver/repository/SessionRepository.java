package com.webrtc.signalingserver.repository;

import org.java_websocket.WebSocket;

import java.util.List;

public interface SessionRepository {
    // connection
    Boolean containsKeyOnConnections(String key);
    WebSocket getWebSocketOnConnections(String key);
    void removeKeyOnConnections(String key);
    void addWebSocketOnConnections(String key, WebSocket socket);
    void closeConnection(String key);

    // sessionManager
    Boolean containsLectureSessionOnSessionManager(String key);
    Boolean containsConnectionOnLectureSession(String lectureId, String key);
    List<String> getSessionsByLectureId(String lectureId);
    void removeLectureSessionByLectureId(String lectureId);
    void removeSessionOnLecture(String lectureId, String targetToRemove);
    void addSessionOnLecture(String lectureId, String targetToAdd);

    // waitingRoom
    Boolean containsKeyOnWaitingRoom(String changeLongToString);
    void addConnectionOnWaitingRoom(String changeLongToString, WebSocket socket);
    List<WebSocket> getConnectionsOnWaitingRoom(String changeLongToString);
    void removeKeyOnWaitingRoom(String changeLongToString);
    void createWaitingRoomByLectureId(String changeLongToString);
}
