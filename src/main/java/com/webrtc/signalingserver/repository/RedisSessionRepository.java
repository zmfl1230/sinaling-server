package com.webrtc.signalingserver.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RedisSessionRepository extends SessionManagerRepository{

    private final RedisTemplate<String, String> redisTemplate;

    // lecture id, session participants
    private final SetOperations<String, String> sessionManager;

    public RedisSessionRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.sessionManager = redisTemplate.opsForSet();
    }

    // sessionManager
    /**
     * sessionManager.size(key) 해당 키값으로 저장된 값이 없는 경우, 0 반환
     * @return
     */
    @Override
    public Boolean containsLectureSessionOnSessionManager(String key) {
        return sessionManager.size(key) != 0;
    }

    /**
     * sessionManager.members(lectureId)
     * 해당 키 값으로 반환 값 없을 경우, return []
     */
    @Override
    public Boolean containsConnectionOnLectureSession(String lectureId, String key) {
        if(sessionManager.members(lectureId) == null) return false;
        return Objects.requireNonNull(sessionManager.members(lectureId)).contains(key);
    }

    @Override
    public List<String> getSessionsByLectureId(String lectureId) {
        return new ArrayList<>(sessionManager.members(lectureId));
    }

    @Override
    public void removeLectureSessionByLectureId(String lectureId) {
        redisTemplate.delete(lectureId);
    }

    @Override
    public void removeSessionOnLecture(String lectureId, String targetToRemove) {
        sessionManager.remove(lectureId, targetToRemove);
    }

    @Override
    public void addSessionOnLecture(String lectureId, String targetToAdd) {
        sessionManager.add(lectureId, targetToAdd);
    }

}
