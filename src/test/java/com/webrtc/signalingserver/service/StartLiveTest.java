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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static com.webrtc.signalingserver.util.EncryptString.*;
import static org.assertj.core.api.Assertions.*;

public class StartLiveTest {


    private WebSocketClient client;


    ObjectRepository objectRepository = new MemoryRepository();
    SessionRepository sessionRepository = new MemorySessionRepository();
    WebSocketService webSocketService = new WebSocketService(objectRepository, sessionRepository);

    Member teacher;
    Member teacher1;
    Member student1;

    Lecture lecture;
    Lecture lecture1;

    @BeforeEach
    public void setUpObject() {
        client = new TestWebSocketClient(URI.create("ws://localhost:8888/"));

        teacher = new Member();
        teacher.setId(1L);
        teacher.setName("teacher");
        teacher.setRole(MemberRole.LECTURER);

        teacher1 = new Member();
        teacher1.setId(4L);
        teacher1.setName("teacher1");
        teacher1.setRole(MemberRole.LECTURER);

        student1 = new Member();
        student1.setId(2L);
        student1.setName("student1");
        student1.setRole(MemberRole.STUDENT);

        lecture = new Lecture();
        lecture.setId(1L);
        lecture.setLecturer(teacher);

        lecture1 = new Lecture();
        lecture1.setId(2L);
        lecture1.setLecturer(teacher1);
        lecture1.getStudents().add(student1);
    }



    @Test
    @DisplayName("강의자가 본인 강의 라이브 시작")
    public void startLiveByCorrectLecturer() {

        //given
        objectRepository.saveMember(teacher);
        objectRepository.saveLecture(lecture);

        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher.getId(), lecture.getId(), null);

        //when
        webSocketService.startLive(client.getConnection(), startLive);

    }


    @Test
    @DisplayName("강의자가 본인강의가 아닌 강의 라이브 시작 -> IllegalArgumentException")
    public void startLiveByWrongLecturer() {

        //Given
        objectRepository.saveMember(teacher);
        objectRepository.saveMember(teacher1);
        objectRepository.saveLecture(lecture);

        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher1.getId(), lecture.getId(), null);

        //Then
        Assertions.assertThrows(IllegalArgumentException.class, () -> webSocketService.startLive(client.getConnection(), startLive));

    }

    @Test
    @DisplayName("강의자가 아닌 학생이 라이브 시작 -> IllegalArgumentException")
    public void startLiveByStudent() {

        //Given
        objectRepository.saveMember(teacher);
        objectRepository.saveMember(student1);
        objectRepository.saveLecture(lecture);

        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher1.getId(), lecture.getId(), null);
        //Then
        Assertions.assertThrows(IllegalArgumentException.class, () -> webSocketService.startLive(client.getConnection(), startLive));

    }

    @Test
    @DisplayName("라이브 강의 생성시, sessionManager 생성 확인")
    public void checkSessionManager() {
        //given
        objectRepository.saveMember(teacher);
        objectRepository.saveLecture(lecture);

        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher.getId(), lecture.getId(), null);
        //when
        webSocketService.startLive(client.getConnection(), startLive);

        //given
        String encryptedKey = convertedToEncryption(teacher.getId(), lecture.getId());
        assertThat(sessionRepository.containsKeyOnConnections(encryptedKey)).isTrue();
        assertThat(sessionRepository.getWebSocketOnConnections(encryptedKey)).isSameAs(client.getConnection());
    }
}