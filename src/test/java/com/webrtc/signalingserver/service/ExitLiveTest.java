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

public class ExitLiveTest {

    WebSocketClient teacherClient;
    TemplateForSynchronized template;
    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    WebSocketService webSocketService;

    Member teacher1;
    Member teacher2;
    Member student1;
    Member student2;

    Lecture lecture;
    Lecture lecture1;

    @BeforeEach
    public void setUp() {
        objectRepository = new MemoryRepository();
        sessionRepository = new MemorySessionRepository();
        template = new TemplateForSynchronized();
        webSocketService = new WebSocketService(objectRepository, sessionRepository, template);

        teacher1 = new Member(1L, "teacher", MemberRole.LECTURER);
        teacher2 = new Member(4L, "teacher1", MemberRole.LECTURER);
        student1 = new Member(2L, "student1", MemberRole.STUDENT);
        student2 = new Member(3L, "student2", MemberRole.STUDENT);

        lecture = new Lecture(1L, teacher1);
        lecture.getStudents().add(student1);
        lecture.getStudents().add(student2);
        lecture1 = new Lecture(2L, teacher1);
        lecture1.getStudents().add(teacher2);

        objectRepository.saveMember(teacher1);
        objectRepository.saveMember(teacher2);
        objectRepository.saveMember(student1);
        objectRepository.saveMember(student2);
        objectRepository.saveLecture(lecture);
        objectRepository.saveLecture(lecture1);

        // start live
        teacherClient = new TestWebSocketClient(URI.create("ws://localhost:8888/"));
        LiveRequestDto startLive1 = LiveRequestDto.buildBasicDto("startLive", teacher1.getId(), lecture.getId(), null);
        webSocketService.startLive(teacherClient.getConnection(), startLive1);

        teacherClient = new TestWebSocketClient(URI.create("ws://localhost:8888/"));
        LiveRequestDto startLive2 = LiveRequestDto.buildBasicDto("startLive", teacher1.getId(), lecture1.getId(), null);
        webSocketService.startLive(teacherClient.getConnection(), startLive2);
    }

    // 1. 강의자 강의 세션 종료

    @Test
    @DisplayName("강의자가 강의 세션 종료 시, 올바른 데이터들이 삭제되는 지 확인")
    public void exitLiveByAuthorizedTeacher() {

        //given
        for (Member student : lecture.getStudents()) {
            LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterLive", student.getId(), lecture.getId(), null);
            webSocketService.enterLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), enterLive);
        }

        //when
        LiveRequestDto exitLive = LiveRequestDto.buildBasicDto("exitLive", lecture.getLecturer().getId(), lecture.getId(), null);
        webSocketService.exitLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), exitLive);

        //then
        // lectureSession에서 해당 강의 제거되었는지 확인
        assertThat(sessionRepository.containsLectureSessionOnSessionManager(changeLongToString(lecture.getId()))).isFalse();

        // 강의자 관련 데이터가 완전히 제거되었는지 확인
        String encryption = changeLongToString(lecture.getId(), lecture.getLecturer().getId());
        assertThat(sessionRepository.containsKeyOnConnections(encryption)).isFalse();

        // 강의 수강생 관련 데이터가 완전히 제거되었는지 확인
        for (Member student : lecture.getStudents()) {
            encryption = changeLongToString(lecture.getId(), student.getId());
            assertThat(sessionRepository.containsKeyOnConnections(encryption)).isFalse();
        }

    }
    @Test
    @DisplayName("권한이 없는 강의자가 강의 세션 종료 시, exception")
    public void exitLiveByUnAuthorizedTeacher() {

        //given
        for (Member student : lecture.getStudents()) {
            LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterLive", student.getId(), lecture.getId(), null);
            webSocketService.enterLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), enterLive);
        }

        //when
        LiveRequestDto exitLive = LiveRequestDto.buildBasicDto("exitLive", teacher2.getId(), lecture.getId(), null);

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> webSocketService.exitLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), exitLive) );

    }

    @Test
    @DisplayName("ROLE 은 강사이지만, 특정 강의의 수강생으로 참여하는 경우 -> 학생과 동일한 처리")
    public void leaveLiveByAuthorizedTeacher() {

        //given
        for (Member student : lecture1.getStudents()) {
            LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterLive", student.getId(), lecture1.getId(), null);
            webSocketService.enterLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), enterLive);
        }

        //when
        LiveRequestDto exitLive = LiveRequestDto.buildBasicDto("exitLive", teacher2.getId(), lecture1.getId(), null);
        webSocketService.exitLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), exitLive);

        //then
        // lectureSession에서 해당 강의가 아직 존재하는지 확인(삭제되면 안됨)
        assertThat(sessionRepository.containsLectureSessionOnSessionManager(changeLongToString(lecture1.getId()))).isTrue();

        // 강의자 관련 데이터가 아직 존재하는지 확인(삭제되면 안됨)
        String encryption = changeLongToString(lecture1.getId(), lecture1.getLecturer().getId());
        assertThat(sessionRepository.containsKeyOnConnections(encryption)).isTrue();

        // 강의 수강생 관련 데이터가 완전히 제거되었는지 확인
        encryption = changeLongToString(lecture1.getId(), teacher2.getId());
        assertThat(sessionRepository.containsKeyOnConnections(encryption)).isFalse();

    }
    // 2. 학생 강의 종료

    @Test
    @DisplayName("학생이 강의 세션 종료 시, 올바른 데이터들이 삭제되는 지 확인")
    public void leaveLiveByAuthorizedStudent() {

        //given
        for (Member student : lecture.getStudents()) {
            LiveRequestDto enterLive = LiveRequestDto.buildBasicDto("enterLive", student.getId(), lecture.getId(), null);
            webSocketService.enterLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), enterLive);
        }

        //when
        LiveRequestDto exitLive = LiveRequestDto.buildBasicDto("exitLive", student1.getId(), lecture.getId(), null);
        webSocketService.exitLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), exitLive);

        //then
        // lectureSession에서 해당 강의가 아직 존재하는지 확인(삭제되면 안됨)
        assertThat(sessionRepository.containsLectureSessionOnSessionManager(changeLongToString(lecture.getId()))).isTrue();

        // 강의자 관련 데이터가 아직 존재하는지 확인(삭제되면 안됨)
        String encryption = changeLongToString(lecture.getId(), lecture.getLecturer().getId());
        assertThat(sessionRepository.containsKeyOnConnections(encryption)).isTrue();

        // 강의 수강생 관련 데이터가 완전히 제거되었는지 확인
        encryption = changeLongToString(lecture.getId(), student1.getId());
        assertThat(sessionRepository.containsKeyOnConnections(encryption)).isFalse();
    }

    @Test
    @DisplayName("권한이 없는 학생이 강의 종료 시, exception")
    public void leaveLiveByUnUnAuthorizedStudent() {

        //given
        LiveRequestDto exitLive = LiveRequestDto.buildBasicDto("exitLive", student1.getId(), lecture1.getId(), null);

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> webSocketService.exitLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), exitLive) );

    }

    @Test
    @DisplayName("세션에 없는 학생이 강의 종료 시, exception")
    public void exitLiveByUnAuthorizedStudent() {

        //given
        LiveRequestDto exitLive = LiveRequestDto.buildBasicDto("exitLive", student1.getId(), lecture1.getId(), null);

        //then
        Assertions.assertThrows(IllegalArgumentException.class, () -> webSocketService.exitLive(new TestWebSocketClient(URI.create("ws://localhost:8888/")).getConnection(), exitLive) );

    }
}
