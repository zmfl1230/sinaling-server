package com.webrtc.signalingserver.service;

import com.webrtc.signalingserver.WebSocketClientStub;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.webrtc.signalingserver.util.EncryptString.changeLongToString;
import static org.assertj.core.api.Assertions.assertThat;

class EnterWaitingRoomTest {
    WebSocketClient client;
    WebSocketClient client1;
    WebSocketClient teacherClient;
    ObjectRepository objectRepository;
    SessionRepository sessionRepository;
    WebSocketService webSocketService;
    TemplateForSynchronized template;

    Member teacher;
    Member student;
    Member student1;
    Lecture lecture;

    @BeforeEach
    public void setUp() {
        objectRepository = new MemoryRepository();
        sessionRepository = new MemorySessionRepository();
        template = new TemplateForSynchronized();
        webSocketService = new WebSocketService(objectRepository, sessionRepository, template);

        teacher = new Member(1L, "teacher", MemberRole.LECTURER);
        student = new Member(2L, "student1", MemberRole.STUDENT);
        student1 = new Member(3L, "student1", MemberRole.STUDENT);

        lecture = new Lecture(1L, teacher);
        lecture.getStudents().add(student);
        lecture.getStudents().add(student1);

        objectRepository.saveMember(teacher);
        objectRepository.saveMember(student);
        objectRepository.saveMember(student1);
        objectRepository.saveLecture(lecture);

    }

    @Test
    @DisplayName("대기실에 입장 시도(강의 생성 이전), 정상적으로 waiting room에 컨넥션 적재 성공")
    public void enterWaitingRoomBeforeLiveStarted() throws Exception {
        //Given
        LiveRequestDto enterWaitingRoom = LiveRequestDto.buildBasicDto("enterWaitingRoom", student.getId(), lecture.getId(), null);
        client = new WebSocketClientStub(URI.create("ws://localhost:8888/"));

        LiveRequestDto enterWaitingRoom1 = LiveRequestDto.buildBasicDto("enterWaitingRoom", student1.getId(), lecture.getId(), null);
        client1 = new WebSocketClientStub(URI.create("ws://localhost:8888/"));

        //When
        webSocketService.enterWaitingRoom(client.getConnection(), enterWaitingRoom);

        //Then
        // 정상적으로 대기실이 생성되는지 확인
        assertThat(sessionRepository.containsKeyOnWaitingRoom(changeLongToString(lecture.getId()))).isTrue();
        // 클라이언트가 정상적으로 담기는지 확인
        assertThat(sessionRepository.getConnectionsOnWaitingRoom(changeLongToString(lecture.getId())).size()).isEqualTo(1);

        //When
        webSocketService.enterWaitingRoom(client1.getConnection(), enterWaitingRoom1);

        //Then
        // 두번쨰 대기실 입장 요청시 클라이언트가 정상적으로 담기는지 확인
        assertThat(sessionRepository.getConnectionsOnWaitingRoom(changeLongToString(lecture.getId())).size()).isEqualTo(2);

    }

    @Test
    @DisplayName("대기실 입장 시도(강의 생성 이후) 대기실 입장 거부, 생성 X")
    public void enterWaitingRoomAfterLiveStarted() throws Exception {

        //Given
        // start live
        teacherClient = new WebSocketClientStub(URI.create("ws://localhost:8888/"));
        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher.getId(), lecture.getId(), null);
        webSocketService.startLive(teacherClient.getConnection(), startLive);

        // 강의 생성 대기실 접속 요청
        LiveRequestDto enterWaitingRoom = LiveRequestDto.buildBasicDto("enterWaitingRoom", student.getId(), lecture.getId(), null);
        client = new WebSocketClientStub(URI.create("ws://localhost:8888/"));

        //When
        webSocketService.enterWaitingRoom(client.getConnection(), enterWaitingRoom);

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
            enterWaitingRoom(student.getId());
            latch.countDown();
        });
        service.submit(() -> {
            enterWaitingRoom(student.getId());
            latch.countDown();
        });
        service.submit(() -> {
            enterWaitingRoom(student.getId());
            latch.countDown();
        });
        service.submit(() -> {
            enterWaitingRoom(student.getId());
            latch.countDown();
        });
        service.submit(() -> {
            enterWaitingRoom(student.getId());
            latch.countDown();
        });
        service.submit(() -> {
            createLectureLive();
            latch.countDown();
        });

        latch.await();

        //Then
        assertThat(sessionRepository.containsKeyOnWaitingRoom(changeLongToString(lecture.getId()))).isFalse();

    }

    void createLectureLive() {
        teacherClient = new WebSocketClientStub(URI.create("ws://localhost:8888/"));
        LiveRequestDto startLive = LiveRequestDto.buildBasicDto("startLive", teacher.getId(), lecture.getId(), null);
        webSocketService.startLive(teacherClient.getConnection(), startLive);
    }

    void enterWaitingRoom(Long studentId) {
        LiveRequestDto enterWaitingRoom = LiveRequestDto.buildBasicDto("enterWaitingRoom", studentId, lecture.getId(), null);
        client = new WebSocketClientStub(URI.create("ws://localhost:8888/"));
        webSocketService.enterWaitingRoom(client.getConnection(), enterWaitingRoom);
    }

}