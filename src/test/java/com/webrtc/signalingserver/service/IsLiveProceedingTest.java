package com.webrtc.signalingserver.service;

import com.webrtc.signalingserver.TestWebSocketClient;
import com.webrtc.signalingserver.domain.dto.LiveRequestDto;
import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.domain.entity.MemberRole;
import com.webrtc.signalingserver.repository.MemoryRepository;
import com.webrtc.signalingserver.repository.MemorySessionRepository;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import org.java_websocket.client.WebSocketClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

class IsLiveProceedingTest {
    WebSocketClient client;
    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    WebSocketService webSocketService;
    TemplateForSynchronized template;

    Member teacher;
    Member student;
    Lecture lecture;

    @BeforeEach
    public void setUp() {
        objectRepository = new MemoryRepository();
        sessionRepository = new MemorySessionRepository();
        template = new TemplateForSynchronized();
        webSocketService = new WebSocketService(objectRepository, sessionRepository, template);

        teacher = new Member(1L, "teacher", MemberRole.LECTURER);
        student = new Member(2L, "student1", MemberRole.STUDENT);

        lecture = new Lecture(1L, teacher);
        lecture.getStudents().add(student);

        objectRepository.saveMember(teacher);
        objectRepository.saveMember(student);
        objectRepository.saveLecture(lecture);

    }

    @Test
    @DisplayName("라이브 진행 중인 아닌경우, 라이브 진행 여부 요청시, false")
    void isNotLiveProceeding() {
        //  check isLiveProceeding
        client = new TestWebSocketClient(URI.create("ws://localhost:8888/"));
        LiveRequestDto isLiveProceeding = LiveRequestDto.buildBasicDto("isLiveProceeding", student.getId(), lecture.getId(), null);
        webSocketService.isLiveProceeding(client.getConnection(), isLiveProceeding);
    }

    @Test
    @DisplayName("라이브 진행 중인 경우, 라이브 진행 여부 요청시, true")
    void isLiveProceeding() {
        //  check isLiveProceeding
        // start live
        WebSocketClient teacherClient = new TestWebSocketClient(URI.create("ws://localhost:8888/"));
        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher.getId(), lecture.getId(), null);
        webSocketService.startLive(teacherClient.getConnection(), startLive);

        //  check isLiveProceeding
        client = new TestWebSocketClient(URI.create("ws://localhost:8888/"));
        LiveRequestDto isLiveProceeding = LiveRequestDto.buildBasicDto("isLiveProceeding", student.getId(), lecture.getId(), null);
        webSocketService.isLiveProceeding(client.getConnection(), isLiveProceeding);
    }
}