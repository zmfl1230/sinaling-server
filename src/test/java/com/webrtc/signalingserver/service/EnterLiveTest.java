package com.webrtc.signalingserver.service;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.domain.entity.MemberRole;
import com.webrtc.signalingserver.repository.MemoryRepository;
import com.webrtc.signalingserver.repository.MemorySessionRepository;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.webrtc.signalingserver.util.EncryptString.*;
import static org.assertj.core.api.Assertions.assertThat;

public class EnterLiveTest {
    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    CommonRequest commonRequest;

    Member teacher;
    Member student1;
    Member student2;

    Lecture lecture;
    Lecture lecture1;

    @BeforeEach
    public void setUp() {
        objectRepository = new MemoryRepository();
        sessionRepository = new MemorySessionRepository();

        commonRequest = new CommonRequest(objectRepository, sessionRepository);

        teacher = new Member(1L, "teacher", MemberRole.LECTURER);
        student1 = new Member(2L, "student1", MemberRole.STUDENT);
        student2 = new Member(3L, "student2", MemberRole.STUDENT);

        lecture = new Lecture(1L, teacher);
        lecture1 = new Lecture(2L, teacher);
        lecture.getStudents().add(student1);

        objectRepository.saveMember(teacher);
        objectRepository.saveMember(student1);
        objectRepository.saveMember(student2);
        objectRepository.saveLecture(lecture);
        objectRepository.saveLecture(lecture1);

        // start live
        commonRequest.startLive(teacher.getId(), lecture.getId());
    }

    @Test
    @DisplayName("강의 수강자 접속 시, LectureSession 및 Connection 에 저장 여부 확인")
    public void enterLiveAuthorizedStudent(){
        //When
        commonRequest.enterLive(student1.getId(), lecture.getId());

        //Then
        String encryptedKey = changeLongToString(lecture.getId(), student1.getId());
        //connection 저장 여부 확인
        assertThat(sessionRepository.containsKeyOnConnections(encryptedKey)).isTrue();
        assertThat(sessionRepository.getWebSocketOnConnections(encryptedKey)).isSameAs(commonRequest.getConnection());

        // 해당 lecture session 에 학생 키 저장되어 있는지 확인
        assertThat(sessionRepository.getConnectionsByLectureId(changeLongToString(lecture.getId())).contains(encryptedKey)).isTrue();

    }

    @Test
    @DisplayName("강의 수강생이 아닌 학생이 접속 요청 시, exception")
    public void enterLiveUnAuthorizedStudent(){
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> commonRequest.enterLive(student2.getId(), lecture.getId()));
    }

    @Test
    @DisplayName("lecture session에 없는 라이브 강의에 접속 요청 시, exception")
    public void enterLectureNotExisted(){
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> commonRequest.enterLive(student1.getId(), lecture1.getId()));
    }
}
