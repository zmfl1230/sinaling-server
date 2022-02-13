package com.webrtc.signalingserver.service;

import com.webrtc.signalingserver.domain.dto.LiveRequestDto;
import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.domain.entity.MemberRole;
import com.webrtc.signalingserver.exception.ValidatePermission;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import com.webrtc.signalingserver.util.GsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import java.util.Map;

import static com.webrtc.signalingserver.util.EncryptString.*;

@Slf4j
public class WebSocketService {

    private final ObjectRepository objectRepository;
    private final SessionRepository sessionRepository;

    public WebSocketService(ObjectRepository objectRepository, SessionRepository sessionRepository) {
        this.objectRepository = objectRepository;
        this.sessionRepository = sessionRepository;
    }

    public void isLiveProceeding(WebSocket socket, LiveRequestDto messageObj) {
        boolean proceeding = sessionRepository.containsLectureSessionOnSessionManager(changeLongToString(messageObj.lectureId));

        Map<String, Object> objectMap = GsonUtil.makeCommonMap("isLiveProceeding", messageObj.userId, 200);
        objectMap.put("proceeding", proceeding);
        GsonUtil.commonSendMessage(socket, objectMap);

        log.info("라이브 진행 여부 발송 성공 proceeding: {}",proceeding);
    }

    public void enterWaitingRoom(WebSocket socket, LiveRequestDto messageObj) {
        Member member = objectRepository.findMember(messageObj.userId);
        Lecture lecture = objectRepository.findLecture(messageObj.lectureId);
        ValidatePermission.validateAccessPermission(member, lecture);

        // waitingRoom에 해당 강의 Id가 키로 있는지 조사(요청 시점에 강의가 생성되었을 수도 있으니)
        Boolean lectureProceeding = sessionRepository.containsKeyOnWaitingRoom(changeLongToString(lecture.getId()));

        // 있는 경우, 해당 컬렉션에 본인 커넥션 추가
        if (lectureProceeding){
            sessionRepository.addConnectionOnWaitingRoom(changeLongToString(lecture.getId()), socket);
            Map<String, Object> objectMap = GsonUtil.makeCommonMap("enterWaitingRoom", messageObj.userId, 200);
            objectMap.put("lecturerId", lecture.getLecturer().getId());
            GsonUtil.commonSendMessage(socket, objectMap);

            log.info("대기실 입장: {}",messageObj.userId);
        } else{
        // 없는 경우(요청 시점에 강의가 생성된 경우)
        // 컬렉션에 커넥션 추가할 필요없이 강의가 생성되었음을 알림

            Map<String, Object> objectMap = GsonUtil.makeCommonMap("liveStarted", messageObj.userId, 200);
            objectMap.put("lecturerId", lecture.getLecturer().getId());
            GsonUtil.commonSendMessage(socket, objectMap);

            log.info("강의 열림: {}",messageObj.lectureId);
        }


    }

    public void startLive(WebSocket socket, LiveRequestDto messageObj) {
        Lecture lecture = objectRepository.findLecture(messageObj.lectureId);
        ValidatePermission.validateLecturer(messageObj.userId, lecture);
        startLiveLecture(messageObj.lectureId, messageObj.userId, socket);

        Map<String, Object> objectMap = GsonUtil.makeCommonMap("startLive", messageObj.userId, 200);
        GsonUtil.commonSendMessage(socket, objectMap);


        log.info("라이브 생성 성공, {}", socket.getRemoteSocketAddress());
    }

    public void enterLive(WebSocket socket, LiveRequestDto messageObj) {
        Member member = objectRepository.findMember(messageObj.userId);
        Lecture lectureToEnter = objectRepository.findLecture(messageObj.lectureId);

        ValidatePermission.validateAccessPermission(member, lectureToEnter);
        enterLiveLecture(messageObj.lectureId, messageObj.userId, socket);

        Map<String, Object> objectMap = GsonUtil.makeCommonMap("enterLive", messageObj.userId, 200);
        GsonUtil.commonSendMessage(socket, objectMap);

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
        sessionRepository.addMessageOnMessageOffer(encryptedUser, message);

        // 본인을 제외한 나머지 참여자에게 offer 전달
        sendToAll(messageObj.lectureId, messageObj.userId,  message);

        // 나머지 참여자의 offer 본인에게 전달
        for (String target : sessionRepository.getSessionsByLectureId(changeLongToString(messageObj.lectureId))) {
            // 본인을 제외한 모두의 message를 본인에게 보냄
            if (!target.equals(encryptedUser)) {
                socket.send(sessionRepository.getMessageOnMessageOffer(target));
            }
        }
        Map<String, Object> objectMap = GsonUtil.makeCommonMap("sdp", messageObj.userId, 200);
        GsonUtil.commonSendMessage(socket, objectMap);

    }

    public void answer(WebSocket socket, LiveRequestDto messageObj, String message) {
        // Json 문자열 -> Map
        Map<String, Object> map = GsonUtil.decode(message, Map.class);
        if(map.containsKey("sender") && map.get("sender") instanceof Long) {
            String encryptedSender = convertedToEncryption(messageObj.lectureId, (Long) map.get("sender"));
            if(sessionRepository.containsKeyOnConnections(encryptedSender))
                sessionRepository.sendMessageUsingConnectionKey(encryptedSender, message);
        }
        else {
            sendToAll(messageObj.lectureId, messageObj.userId, message);
        }
        Map<String, Object> objectMap = GsonUtil.makeCommonMap("sdp", messageObj.userId, 200);
        GsonUtil.commonSendMessage(socket, objectMap);

    }

    public void exitLive(WebSocket socket, LiveRequestDto messageObj) {
        Lecture lecture = objectRepository.findLecture(messageObj.lectureId);
        Member member = objectRepository.findMember(messageObj.userId);

        // 해당 멤버가 lecture 관련 회원인지 검증(강의자, 수강자)
        ValidatePermission.validateAccessPermission(member, lecture);

        // 강사의 강의 종료
        if(member.getRole() == MemberRole.LECTURER && !lecture.contains(member)) {
            // 강사와 요청 멤버가 동일한 인물인지 검증
            ValidatePermission.validateLecturer(member.getId(), lecture);
            // sessionManager 돌면서 현재 session에 참여하고 있는 user 탐색
            String lectureToString = changeLongToString(lecture.getId());
            for (String needToRemove : sessionRepository.getSessionsByLectureId(lectureToString)) {
                removeConnections(needToRemove);
            }
            sessionRepository.removeLectureSessionByLectureId(lectureToString);
        }
        // 수강생 강의 종료
        else {
             String encryptedUser = convertedToEncryption(messageObj.lectureId, messageObj.userId);
            if(sessionRepository.containsConnectionOnLectureSession(changeLongToString(messageObj.lectureId), encryptedUser))
                throw new IllegalArgumentException("접속 정보가 없는 사용자입니다.");
             removeConnections(encryptedUser);
             sessionRepository.removeSessionOnLecture(changeLongToString(lecture.getId()), encryptedUser);
             sendToAll(messageObj.lectureId, messageObj.userId, String.format("%s 님이 라이브 강의를 나갔습니다.", messageObj.userId));
        }
        log.info("client exited: {}", messageObj.userId);

        Map<String, Object> objectMap = GsonUtil.makeCommonMap("exitLive", messageObj.userId, 200);
        GsonUtil.commonSendMessage(socket, objectMap);

    }


    private void sendToAll(Long lectureId, Long userId, String message) {
        String changedValue = convertedToEncryption(lectureId, userId);
        for (String target : sessionRepository.getSessionsByLectureId(changeLongToString(lectureId))) {
            // 본인을 제외한 모두에게 요청보냄
            if (!target.equals(changedValue))
                sessionRepository.sendMessageUsingConnectionKey(target, message);
        }
    }

    private void startLiveLecture(Long lectureId, Long userId, WebSocket conn) {
        String lectureToString = changeLongToString(lectureId);
        sessionRepository.addLectureSession(lectureToString);
        String changedValue = convertedToEncryption(lectureId, userId);
        sessionRepository.addSessionOnLecture(lectureToString, changedValue);
        sessionRepository.addWebSocketOnConnections(changedValue, conn);

        log.info("강의 세션이 생성되었습니다.");
    }

    private void enterLiveLecture(Long lectureId, Long userId, WebSocket conn) {
        String lectureToString = changeLongToString(lectureId);
        if(!sessionRepository.containsLectureSessionOnSessionManager(lectureToString)) throw new IllegalArgumentException("현재 진행하지 않는 강의입니다.");

        String changedValue = convertedToEncryption(lectureId, userId);
        sessionRepository.addSessionOnLecture(lectureToString, changedValue);
        sessionRepository.addWebSocketOnConnections(changedValue, conn);
        log.info("강의 세션에 참가하였습니다");
    }

    private void removeConnections(String target) {
        if(sessionRepository.containsKeyOnConnections(target)){
            sessionRepository.closeConnection(target);
            sessionRepository.removeKeyOnConnections(target);
        }
        sessionRepository.removeMessageOnMessageOffer(target);
    }

}
