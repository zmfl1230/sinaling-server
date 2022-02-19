package com.webrtc.signalingserver.repository;

import org.java_websocket.WebSocket;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemorySessionRepository extends SessionManagerRepository {

    // lecture id, session participants
    private final ConcurrentMap<String, List<String>> sessionManager = new ConcurrentHashMap<>();


    @Override
    public void removeConnectionOnLectureSession(String lectureId, String targetToRemove) {
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
    public List<String> getConnectionsByLectureId(String lectureId) {
        return sessionManager.get(lectureId);
    }


    @Override
    public void removeLectureSessionByLectureId(String lectureId) {
        sessionManager.remove(lectureId);
    }

    @Override
    public void addConnectionOnLectureSession(String lectureId, String targetToAdd) {
        if(!sessionManager.containsKey(lectureId)) addLectureSession(lectureId);
        sessionManager.get(lectureId).add(targetToAdd);
    }

    private void addLectureSession(String lectureId) {
        sessionManager.put(lectureId, new LinkedList<>());
    }

}
