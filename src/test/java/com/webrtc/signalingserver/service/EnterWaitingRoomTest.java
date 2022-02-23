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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.webrtc.signalingserver.util.EncryptString.changeLongToString;
import static org.assertj.core.api.Assertions.assertThat;

class EnterWaitingRoomTest {

    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    CommonRequest commonRequest;

    Member teacher;
    Member student1;
    Member student2;

    Lecture lecture;

    @BeforeEach
    public void setUpObject() {
        objectRepository = new MockObjectRepository();
        sessionRepository = new MockSessionRepository();

        commonRequest = new CommonRequest(objectRepository, sessionRepository);

        teacher = new Member(1L, "teacher", MemberRole.LECTURER);
        student1 = new Member(2L, "student1", MemberRole.STUDENT);
        student2 = new Member(3L, "student2", MemberRole.STUDENT);

        lecture = new Lecture(1L, teacher);
        lecture.getStudents().add(student1);
        lecture.getStudents().add(student2);

        objectRepository.saveMember(teacher);
        objectRepository.saveMember(student1);
        objectRepository.saveMember(student2);
        objectRepository.saveLecture(lecture);
    }

    @Test
    @DisplayName("대기실에 입장 시도(강의 생성 이전), 정상적으로 waiting room에 컨넥션 적재 성공")
    public void enterWaitingRoomBeforeLiveStarted() {
        //When
        commonRequest.enterWaitingRoom(student1.getId(), lecture.getId());

        //Then
        // 정상적으로 대기실이 생성되는지 확인
        assertThat(sessionRepository.containsKeyOnWaitingRoom(changeLongToString(lecture.getId()))).isTrue();
        // 클라이언트가 정상적으로 담기는지 확인
        assertThat(sessionRepository.getConnectionsOnWaitingRoom(changeLongToString(lecture.getId())).size()).isEqualTo(1);

        //When
        commonRequest.enterWaitingRoom(student1.getId(), lecture.getId());

        //Then
        // 두번쨰 대기실 입장 요청시 클라이언트가 정상적으로 담기는지 확인
        assertThat(sessionRepository.getConnectionsOnWaitingRoom(changeLongToString(lecture.getId())).size()).isEqualTo(2);

    }

    @Test
    @DisplayName("대기실 입장 시도(강의 생성 이후) 대기실 입장 거부, 생성 X")
    public void enterWaitingRoomAfterLiveStarted() {

        //Given
        // start live
        commonRequest.startLive(teacher.getId(), lecture.getId());

        //When
        // 강의 생성 후, 대기실 접속 요청
        commonRequest.enterWaitingRoom(student1.getId(), lecture.getId());

        //Then
        // 정상적으로 대기실이 생성이 거부되었는지 확인
        assertThat(sessionRepository.containsKeyOnWaitingRoom(changeLongToString(lecture.getId()))).isFalse();

    }

    // 대기실 입장, 라이브 시작 간 동기화 실현
    @Test
    @DisplayName("스레드를 이용한 동기화 처리 확인, 최종적으로 대기실이 비어있어야 함")
    public void checkSynchronizedBetweenEnterWaitingRoomAndStartLive() throws Exception {
        //When
        int numberOfThreads = 6;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        service.submit(() -> {
            commonRequest.enterWaitingRoom(student1.getId(), lecture.getId());
            latch.countDown();
        });
        service.submit(() -> {
            commonRequest.enterWaitingRoom(student2.getId(), lecture.getId());
            latch.countDown();
        });
        service.submit(() -> {
            commonRequest.enterWaitingRoom(student1.getId(), lecture.getId());
            latch.countDown();
        });
        service.submit(() -> {
            commonRequest.enterWaitingRoom(student2.getId(), lecture.getId());
            latch.countDown();
        });
        service.submit(() -> {
            commonRequest.enterWaitingRoom(student1.getId(), lecture.getId());
            latch.countDown();
        });
        service.submit(() -> {
            commonRequest.startLive(teacher.getId(), lecture.getId());
            latch.countDown();
        });

        latch.await();

        //Then
        assertThat(sessionRepository.containsKeyOnWaitingRoom(changeLongToString(lecture.getId()))).isFalse();

    }

}