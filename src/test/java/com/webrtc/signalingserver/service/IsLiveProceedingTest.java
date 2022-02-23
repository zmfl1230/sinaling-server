package com.webrtc.signalingserver.service;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.domain.entity.MemberRole;
import com.webrtc.signalingserver.repository.MockObjectRepository;
import com.webrtc.signalingserver.repository.MockSessionRepository;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IsLiveProceedingTest {
    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    CommonRequest commonRequest;

    Member teacher;
    Member student;
    Lecture lecture;

    @BeforeEach
    public void setUp() {
        objectRepository = new MockObjectRepository();
        sessionRepository = new MockSessionRepository();

        commonRequest = new CommonRequest(objectRepository, sessionRepository);

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
        commonRequest.isLiveProceeding(student.getId(), lecture.getId());
    }

    @Test
    @DisplayName("라이브 진행 중인 경우, 라이브 진행 여부 요청시, true")
    void isLiveProceeding() {
        //  check isLiveProceeding
        // start live
        commonRequest.startLive(teacher.getId(), lecture.getId());

        //  check isLiveProceeding
        commonRequest.isLiveProceeding(student.getId(), lecture.getId());
    }
}