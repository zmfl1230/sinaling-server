package com.webrtc.signalingserver.service;

import com.google.gson.Gson;
import com.webrtc.signalingserver.domain.dto.LiveRequestDto;
import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.exception.ValidatePermission;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.webrtc.signalingserver.util.EncryptString.*;

@Slf4j
public class WebSocketService {

    @PersistenceContext
    EntityManager em;

    @Autowired
    private RedisTemplate<?, ?> redisTemplate;

    // member id, member socket
//    private final ValueOperations<Long, WebSocket> con = redisTemplate.opsForValue();
    private final Map<String, WebSocket> connections = new HashMap<>();

    // lecture id, session participants
    private final Map<String, List<String>> sessionManager = new HashMap<>();

    // member id, message
    private final Map<String, String> messageOffer = new HashMap<>();

    public void startLive(WebSocket socket, LiveRequestDto messageObj) {
        Lecture lecture = em.find(Lecture.class, messageObj.lectureId);
        ValidatePermission.validateLecturer(messageObj.userId, lecture);
        startLiveLecture(messageObj.lectureId, messageObj.userId, socket);

        socket.send("라이브 생성 성공");
        log.info("라이브 생성 성공, {}", socket.getRemoteSocketAddress());
    }

    public void enterLive(WebSocket socket, LiveRequestDto messageObj) {
        Member member = em.find(Member.class, messageObj.userId);
        Lecture lectureToEnter = em.find(Lecture.class, messageObj.lectureId);

        ValidatePermission.validateAccessPermission(member, lectureToEnter);
        enterLiveLecture(messageObj.lectureId, messageObj.userId, socket);

        socket.send("라이브 입장");
        log.info("라이브 입장 성공, {}", socket.getRemoteSocketAddress());

        sendToAll(messageObj.lectureId, messageObj.userId, String.format("%s님이 입장하셨습니다.", member.getName()));
    }

    public void sdp(WebSocket socket, LiveRequestDto messageObj, String message) {
        switch(messageObj.sdp.type) {
            case "offer":
                this.offer(socket, messageObj, message);
                break;

            case "answer":
                this.answer(socket, messageObj, message);
                break;

            default:
                log.info("잘못된 sdp 요청입니다.");
                break;
        }

    }

    public void offer(WebSocket socket, LiveRequestDto messageObj, String message) {
        // 본인 sdp를 포함한 메세지 저장
        String encryptedUser = convertedToEncryption(messageObj.lectureId, messageObj.userId);
        messageOffer.put(encryptedUser, message);

        // 본인을 제외한 나머지 참여자에게 offer 전달
        sendToAll(messageObj.lectureId, messageObj.userId,  message);

        // 나머지 참여자의 offer 본인에게 전달
        for (String target : sessionManager.get(changeLongToString(messageObj.lectureId))) {
            // 본인을 제외한 모두에게 요청보냄
            if (!target.equals(encryptedUser)) {
                socket.send(messageOffer.get(target));
            }
        }
    }

    public void answer(WebSocket socket, LiveRequestDto messageObj, String message) {

        // Gson 객체 생성
        Gson gson = new Gson();

        // Json 문자열 -> Map
        Map<String, Object> map = gson.fromJson(message, Map.class);
        if(map.containsKey("sender") && map.get("sender") instanceof Long) {
            String encryptedSender = convertedToEncryption(messageObj.lectureId, (Long) map.get("sender"));
            if(connections.containsKey(encryptedSender)) connections.get(encryptedSender).send(message);
        }
        else {
            sendToAll(messageObj.lectureId, messageObj.userId, message);
        }
    }

    public void exitLive(WebSocket socket, LiveRequestDto messageObj) {
        Lecture lecture = em.find(Lecture.class, messageObj.lectureId);

        // TODO: 잘못된 사용자 요청 검증
        // if(member == null || lecture == null)

        // 강사의 강의 종료
        if(lecture.getLecturer().getId().equals(messageObj.userId)) {
            // sessionManager 돌면서 현재 session에 참여하고 있는 user 탐색
            String lectureToString = changeLongToString(lecture.getId());
            for (String needToRemove : sessionManager.get(lectureToString)) {
                removeConnections(needToRemove);
            }
            sessionManager.remove(lectureToString);
        }
        // 수강생 강의 종료
        else {
             String encryptedUser = convertedToEncryption(messageObj.lectureId, messageObj.userId);
             removeConnections(encryptedUser);
             sessionManager.get(changeLongToString(lecture.getId())).remove(encryptedUser);
             sendToAll(messageObj.lectureId, messageObj.userId, String.format("%s 님이 라이브 강의를 나갔습니다.", messageObj.userId));
        }
        log.info("client exited: {}", messageObj.userId);
    }


    private void sendToAll(Long lectureId, Long userId, String message) {
        String changedValue = convertedToEncryption(lectureId, userId);
        for (String target : sessionManager.get(changeLongToString(lectureId))) {
            // 본인을 제외한 모두에게 요청보냄
            if (!target.equals(changedValue)) connections.get(target).send(message);
        }
    }

    private void startLiveLecture(Long lectureId, Long userId, WebSocket conn) {
        String lectureToString = changeLongToString(lectureId);
        sessionManager.put(lectureToString, new LinkedList<>());
        String changedValue = convertedToEncryption(lectureId, userId);
        sessionManager.get(lectureToString).add(changedValue);
        connections.put(changedValue, conn);

        log.info("강의 세션이 생성되었습니다.");
    }

    private void enterLiveLecture(Long lectureId, Long userId, WebSocket conn) {
        String lectureToString = changeLongToString(lectureId);
        if(!sessionManager.containsKey(lectureToString)) throw new IllegalArgumentException("현재 진행하지 않는 강의입니다.");

        String changedValue = convertedToEncryption(lectureId, userId);
        sessionManager.get(lectureToString).add(changedValue);
        connections.put(changedValue, conn);

        log.info("강의 세션에 참가하였습니다");
    }

    private void removeConnections(String target) {
        if(connections.containsKey(target)){
            connections.get(target).close();
            connections.remove(target);
        }
        messageOffer.remove(target);
    }

}
