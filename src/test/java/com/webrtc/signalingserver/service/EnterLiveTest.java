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
import static org.assertj.core.api.Assertions.assertThat;

public class EnterLiveTest {
    WebSocketClient teacherClient;
    WebSocketClient studentClient;
    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    WebSocketService webSocketService;

    Member teacher;
    Member student1;
    Member student2;

    Lecture lecture;
    Lecture lecture1;

    @BeforeEach
    public void setUp() {
        objectRepository = new MemoryRepository();
        sessionRepository = new MemorySessionRepository();
        webSocketService = new WebSocketService(objectRepository, sessionRepository);

        teacher = new Member(1L, "teacher", MemberRole.LECTURER);
        student1 = new Member(2L, "student1", MemberRole.STUDENT);
        student2 = new Member(3L, "student2", MemberRole.STUDENT);

        lecture = new Lecture(1L, teacher);
        lecture.getStudents().add(student1);
        lecture1 = new Lecture(2L, teacher);

        objectRepository.saveMember(teacher);
        objectRepository.saveMember(student1);
        objectRepository.saveLecture(lecture);

        // start live
        teacherClient = new TestWebSocketClient(URI.create("ws://localhost:8888/"));
        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher.getId(), lecture.getId(), null);
        webSocketService.startLive(teacherClient.getConnection(), startLive);
    }

    @Test
    @DisplayName("강의 수강자 접속 시, LectureSession 및 Connection 에 저장 여부 확인")
    public void enterLiveAuthorizedStudent(){
        //Given
        LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterLive", student1.getId(), lecture.getId(), null);

        //When
        studentClient = new TestWebSocketClient(URI.create("ws://localhost:8888/"));
        webSocketService.enterLive(studentClient.getConnection(), enterLive);

        //Then
        String encryptedKey = convertedToEncryption(lecture.getId(), student1.getId());
        //connection 저장 여부 확인
        assertThat(sessionRepository.containsKeyOnConnections(encryptedKey)).isTrue();
        assertThat(sessionRepository.getWebSocketOnConnections(encryptedKey)).isSameAs(studentClient.getConnection());

        // 해당 lecture session 에 학생 키 저장되어 있는지 확인
        assertThat(sessionRepository.getSessionsByLectureId(changeLongToString(lecture.getId())).contains(encryptedKey)).isTrue();

    }

    @Test
    @DisplayName("강의 수강생이 아닌 학생이 접속 요청 시, exception")
    public void enterLiveUnAuthorizedStudent(){
        //Given
        objectRepository.saveMember(student2);
        LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterLive", student2.getId(), lecture.getId(), null);

        //When
        studentClient = new TestWebSocketClient(URI.create("ws://localhost:8888/"));

        //Then
        Assertions.assertThrows(IllegalArgumentException.class, () -> webSocketService.enterLive(studentClient.getConnection(), enterLive));
    }

    @Test
    @DisplayName("lecture session에 없는 라이브 강의에 접속 요청 시, exception")
    public void enterLectureNotExisted(){
        //Given
        objectRepository.saveLecture(lecture1);
        LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterLive", student1.getId(), lecture1.getId(), null);

        //When
        studentClient = new TestWebSocketClient(URI.create("ws://localhost:8888/"));

        //Then
        Assertions.assertThrows(IllegalArgumentException.class, () -> webSocketService.enterLive(studentClient.getConnection(), enterLive));
    }
}
