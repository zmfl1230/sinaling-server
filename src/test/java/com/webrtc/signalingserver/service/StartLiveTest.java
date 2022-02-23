package com.webrtc.signalingserver.service;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.domain.entity.MemberRole;
import com.webrtc.signalingserver.repository.MockObjectRepository;
import com.webrtc.signalingserver.repository.MockSessionRepository;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.webrtc.signalingserver.util.EncryptString.*;
import static org.assertj.core.api.Assertions.*;

public class StartLiveTest {

    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    CommonRequest commonRequest;

    Member teacher;
    Member teacher1;
    Member student1;

    Lecture lecture;
    Lecture lecture1;

    @BeforeEach
    public void setUpObject() {
        objectRepository = new MockObjectRepository();
        sessionRepository = new MockSessionRepository();

        commonRequest = new CommonRequest(objectRepository, sessionRepository);

        teacher = new Member(1L, "teacher", MemberRole.LECTURER);
        teacher1 = new Member(4L, "teacher1", MemberRole.LECTURER);
        student1 = new Member(2L, "student1", MemberRole.STUDENT);

        lecture = new Lecture(1L, teacher);
        lecture1 = new Lecture(2L, teacher1);
        lecture1.getStudents().add(student1);

        objectRepository.saveMember(teacher);
        objectRepository.saveMember(teacher1);
        objectRepository.saveMember(student1);
        objectRepository.saveLecture(lecture);
        objectRepository.saveLecture(lecture1);
    }


    @Test
    @DisplayName("강의자가 본인 강의 라이브 시작")
    public void startLiveByCorrectLecturer() {
        //when
        commonRequest.startLive(teacher.getId(), lecture.getId());

        //then
        assertThat(sessionRepository.containsLectureSessionOnSessionManager(changeLongToString(lecture.getId())))
                .isTrue();
        assertThat(sessionRepository.getConnectionsByLectureId(changeLongToString(lecture.getId())).size()).isEqualTo(1);

    }


    @Test
    @DisplayName("강의자가 본인강의가 아닌 강의 라이브 시작 -> IllegalArgumentException")
    public void startLiveByWrongLecturer() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> commonRequest.startLive(teacher1.getId(), lecture.getId()));
    }

    @Test
    @DisplayName("강의자가 아닌 학생이 라이브 시작 -> IllegalArgumentException")
    public void startLiveByStudent() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> commonRequest.startLive(teacher1.getId(), lecture.getId()));
    }

    @Test
    @DisplayName("라이브 강의 생성시, sessionManager 생성 확인")
    public void checkSessionManager() {
        //when
        commonRequest.startLive(teacher.getId(), lecture.getId());

        //given
        String encryptedKey = changeLongToString(teacher.getId(), lecture.getId());
        assertThat(sessionRepository.containsKeyOnConnections(encryptedKey)).isTrue();
        assertThat(sessionRepository.getWebSocketOnConnections(encryptedKey)).isSameAs(commonRequest.getConnection());
    }
}
