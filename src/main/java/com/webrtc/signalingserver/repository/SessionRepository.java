package com.webrtc.signalingserver.repository;

import org.java_websocket.WebSocket;

import java.util.List;

public interface SessionRepository {
    // connection
    Boolean containsKeyOnConnections(String key);
    WebSocket getWebSocketOnConnections(String key);
    void sendMessageUsingConnectionKey(String key, String message);
    void removeKeyOnConnections(String key);
    void addWebSocketOnConnections(String key, WebSocket socket);
    void closeConnection(String key);

    // messageOffer
    String getMessageOnMessageOffer(String key);
    Boolean containsKeyOnMessageOffer(String key);
    void addMessageOnMessageOffer(String key, String message);
    void removeMessageOnMessageOffer(String key);

    // sessionManager
    Boolean containsLectureSessionOnSessionManager(String key);
    List<String> getSessionsByLectureId(String lectureId);
    void addLectureSession(String lectureId);
    void removeLectureSessionByLectureId(String lectureId);
    void removeSessionOnLecture(String lectureId, String targetToRemove);
    void addSessionOnLecture(String lectureId, String targetToAdd);


}
