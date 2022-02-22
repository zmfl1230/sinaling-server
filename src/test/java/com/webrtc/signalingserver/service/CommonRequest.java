package com.webrtc.signalingserver.service;

import com.webrtc.signalingserver.WebSocketClientStub;
import com.webrtc.signalingserver.domain.dto.LiveRequestDto;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class CommonRequest {
    WebSocketClient client;
    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    WebSocketService webSocketService;
    TemplateForSynchronized template;

    public CommonRequest(ObjectRepository objectRepository, SessionRepository sessionRepository) {
        client = new WebSocketClientStub(URI.create("ws://localhost:8888/"));

        this.objectRepository = objectRepository;
        this.sessionRepository = sessionRepository;
        template = new TemplateForSynchronized();
        webSocketService = new WebSocketService(objectRepository, sessionRepository, template);
    }

    WebSocket getConnection() {
        return client.getConnection();
    }

    public void startLive(Long memberId, Long lectureId) {
        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", memberId, lectureId, null);
        webSocketService.startLive(client.getConnection(), startLive);
    }

    public void enterWaitingRoom(Long memberId, Long lectureId) {
        LiveRequestDto enterWaitingRoom = LiveRequestDto.buildBasicDto("enterWaitingRoom", memberId, lectureId, null);
        webSocketService.enterWaitingRoom(client.getConnection(), enterWaitingRoom);
    }

    public void enterLive(Long memberId, Long lectureId) {
        LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterWaitingRoom", memberId, lectureId, null);
        client = new WebSocketClientStub(URI.create("ws://localhost:8888/"));
        webSocketService.enterLive(client.getConnection(), enterLive);
    }


    public void exitLive(Long memberId, Long lectureId) {
        LiveRequestDto exitLive = LiveRequestDto.buildBasicDto("exitLive", memberId, lectureId, null);
        webSocketService.exitLive(client.getConnection(), exitLive);
    }

    public void isLiveProceeding(Long memberId, Long lectureId) {
        LiveRequestDto isLiveProceeding = LiveRequestDto.buildBasicDto("isLiveProceeding", memberId, lectureId, null);
        webSocketService.isLiveProceeding(client.getConnection(), isLiveProceeding);
    }


}
