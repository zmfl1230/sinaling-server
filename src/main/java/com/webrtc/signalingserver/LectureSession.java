package com.webrtc.signalingserver;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.net.InetSocketAddress;
import java.util.*;

@Slf4j
public class LectureSession extends WebSocketServer {

    @PersistenceContext
    EntityManager em;

    // member id, member socket
    private final Map<Long, WebSocket> connections = new HashMap<>();

    // lecture id, session participants
    private final Map<Long, List<Long>> sessionManager = new HashMap<>();

    // member id, message
    private final Map<Long, String> messageOffer = new HashMap<>();

    public LectureSession() {
        super(new InetSocketAddress(8888));
        log.info("소켓 시작");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.info("New client connected: " + conn.getRemoteSocketAddress() + " hash " + conn.getRemoteSocketAddress().hashCode());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JSONObject obj = new JSONObject(message);
            String message_type = obj.getString("type");
            Long requester = obj.getLong("requesterId");
            Long lectureId = obj.getLong("lectureId");

            switch (message_type) {
                case "STARTLIVE":
                    Lecture lectureToStart = em.find(Lecture.class, lectureId);
                    ValidatePermission.validateLecturer(requester, lectureToStart);
                    startLiveLecture(lectureId, requester, conn);
                    break;
                case "ENTERLIVE":
                    Member member = em.find(Member.class, requester);
                    Lecture lectureToEnter = em.find(Lecture.class, lectureId);

                    ValidatePermission.validateAccessPermission(member, lectureToEnter);

                    enterLiveLecture(lectureId, requester, conn);
                    break;
                case "OFFER":
                    obj.append("sender", requester);
                    message = obj.toString();

                    // 본인 sdp를 포함한 메세지 저장
                    messageOffer.put(requester, message);

                    // 본인을 제외한 나머지 참여자에게 offer 전달
                    sendToAll(lectureId, requester,  message);

                    // 나머지 참여자의 offer 본인에게 전달
                    for (Long sessionMember : sessionManager.get(lectureId)) {
                        // 본인을 제외한 모두에게 요청보냄
                        if (!sessionMember.equals(requester)) {
                            conn.send(messageOffer.get(sessionMember));
                        }
                    }
                    break;
                case "ANSWER":
                    if(!obj.has("sender")) sendToAll(lectureId, requester, message);
                    else {
                        Long sender = obj.getLong("sender");
                        connections.get(sender).send(message);
                    }
                    break;
            }
        } catch (JSONException e) {
            log.info("전송이 실패되었습니다.");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        try {
            JSONObject data = new JSONObject(reason);
            Long requester = data.getLong("requester");
            Long lectureId = data.getLong("lectureId");
            Lecture lecture = em.find(Lecture.class, lectureId);

            // 강의자 요청
            if(lecture.getLecturer().getId().equals(requester)) {
                // 강의 세션 종료
                for (Long existed : sessionManager.get(lectureId)) {
                    removeConnections(existed);
                }
                sessionManager.remove(lectureId);
            }

            // 수강생 요청
            else {
                sessionManager.get(lectureId).remove(requester);
                removeConnections(requester);
            }

            log.info("client disconnected: {} (code: {})", requester, code);
        } catch (JSONException e){
            log.info(e.getMessage());
        }finally {
            log.info("client disconnected (code: {})", code);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception exc) {
        System.out.println("Error happened: " + exc);
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


    public void removeConnections(Long target) {
        connections.get(target).close();
        connections.remove(target);
        messageOffer.remove(target);
    }

}