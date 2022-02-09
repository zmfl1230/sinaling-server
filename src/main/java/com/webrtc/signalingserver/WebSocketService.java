package com.webrtc.signalingserver;

import com.google.gson.Gson;
import com.webrtc.signalingserver.dto.LiveRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
public class WebSocketService {

    @PersistenceContext
    EntityManager em;

    // member id, member socket
    private final Map<Long, WebSocket> connections = new HashMap<>();

    // lecture id, session participants
    private final Map<Long, List<Long>> sessionManager = new HashMap<>();

    // member id, message
    private final Map<Long, String> messageOffer = new HashMap<>();

    public void startLive(WebSocket socket, LiveRequestDto messageObj) {
        Lecture lectureToStart = em.find(Lecture.class, messageObj.lectureId);
        ValidatePermission.validateLecturer(messageObj.userId, lectureToStart);
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
            messageOffer.put(messageObj.userId, message);

            // 본인을 제외한 나머지 참여자에게 offer 전달
            sendToAll(messageObj.lectureId, messageObj.userId,  message);

            // 나머지 참여자의 offer 본인에게 전달
            for (Long sessionMember : sessionManager.get(messageObj.lectureId)) {
                // 본인을 제외한 모두에게 요청보냄
                if (!sessionMember.equals(messageObj.userId)) {
                    socket.send(messageOffer.get(sessionMember));
                }
            }
    }

    public void answer(WebSocket socket, LiveRequestDto messageObj, String message) {

        // Gson 객체 생성
        Gson gson = new Gson();

        // Json 문자열 -> Map
        Map<String, Object> map = gson.fromJson(message, Map.class);
        if(map.containsKey("sender") && map.get("sender") instanceof Long
                && connections.containsKey((Long)map.get("sender"))) connections.get((Long)map.get("sender")).send(message);
        else {
            sendToAll(messageObj.lectureId, messageObj.userId, message);
        }
    }

    public void exitLive(WebSocket socket, LiveRequestDto messageObj) {
        Member member = em.find(Member.class, messageObj.userId);
        Lecture lecture = em.find(Lecture.class, messageObj.lectureId);

        // TODO: 잘못된 사용자 요청 검증
        // if(member == null || lecture == null)

        // 강사의 강의 종료
        if(lecture.getId().equals(messageObj.userId)) {
            // sessionManager 돌면서 현재 session에 참여하고 있는 user 탐색
            for (Long needToRemove : sessionManager.get(lecture.getId())) {
                removeConnections(needToRemove);
            }
            sessionManager.remove(lecture.getId());
        }
        // 수강생 강의 종료
        else {
             removeConnections(messageObj.userId);
             sessionManager.get(lecture.getId()).remove(messageObj.userId);
             sendToAll(messageObj.lectureId, messageObj.userId, String.format("%s 님이 라이브 강의를 나갔습니다.", messageObj.userId));
        }
        log.info("client exited: {}", messageObj.userId);
    }


    private void sendToAll(Long lectureId, Long requester, String message) {
        for (Long member : sessionManager.get(lectureId)) {
            // 본인을 제외한 모두에게 요청보냄
            if (!member.equals(requester)) connections.get(member).send(message);
        }
    }

    private void startLiveLecture(Long lectureId, Long lecturerId, WebSocket conn) {
        sessionManager.put(lectureId, new LinkedList<>());
        sessionManager.get(lectureId).add(lecturerId);

        connections.put(lecturerId, conn);
        log.info("강의 세션이 생성되었습니다.");
    }

    private void enterLiveLecture(Long lectureId, Long studentId, WebSocket conn) {
        if(!sessionManager.containsKey(lectureId)) throw new IllegalArgumentException("현재 진행하지 않는 강의입니다.");

        sessionManager.get(lectureId).add(studentId);
        connections.put(studentId, conn);
        log.info("강의 세션에 참가하였습니다");
    }

    private void removeConnections(Long target) {
        if(connections.containsKey(target)){
            connections.get(target).close();
            connections.remove(target);
        }
        messageOffer.remove(target);
    }

}
