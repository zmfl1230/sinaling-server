package com.webrtc.signalingserver.service.redis;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.repository.*;
import com.webrtc.signalingserver.service.CommonRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.webrtc.signalingserver.util.EncryptString.changeLongToString;

@SpringBootTest
public class SynchronizedTest {
    @Autowired ObjectRepository objectRepository;
    @Autowired SessionRepository sessionRepository;
    @Autowired CommonRequest commonRequest;

    Member teacher;
    Lecture lecture;
    Long firstStudentId = 2L;
    Long secondStudentId = 3L;

    @BeforeEach
    @Transactional
    public void setUp() {
        /**
         * 스레드를 생성해서 돌리면 다른 컨넥션을 공유하기때문에 실제로 테스트 데이터 값을 디비에 저장하지 않는 이상,
         * 권한 에러 발생(디비엔 실제로 테스트를 위해 생성한 엔티티의 값이 저장되지 않는다.)
         * 아니면 엔티티 생성 후, 디비 반영을 위해 flush를 직접 해줘야 한다.
         *
         * 고로 실제에 디비에 아래 아이디를 갖는 데이터가 있음을 가정하고, 테스트를 진행한다.
         * */
        teacher = objectRepository.findMember(1L);
        lecture = objectRepository.findLecture(1L);
    }

    @Test
    public void enterWaitingRoomSynchronizedTest() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        service.submit(() -> {
            commonRequest.enterWaitingRoom(firstStudentId, lecture.getId());
            latch.countDown();
        });

        service.submit(() -> {
            commonRequest.enterWaitingRoom(secondStudentId, lecture.getId());
            latch.countDown();
        });

        latch.await();

        // 추가된 학생 수 (대기실임으로 강사 제외)
        Assertions.assertThat(sessionRepository.getConnectionsOnWaitingRoom(changeLongToString(lecture.getId())).size())
                .isEqualTo(numberOfThreads);

        commonRequest.startLive(teacher.getId(), lecture.getId());
        commonRequest.exitLive(teacher.getId(), lecture.getId());
    }

    @Test
    @Transactional
    public void enterLiveSynchronizedTest() throws InterruptedException {
        objectRepository.saveMember(teacher);
        objectRepository.saveLecture(lecture);

        commonRequest.startLive(teacher.getId(), lecture.getId());

        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        service.submit(() -> {
            commonRequest.enterLive(firstStudentId, lecture.getId());
            latch.countDown();
        });

        service.submit(() -> {
            commonRequest.enterLive(secondStudentId, lecture.getId());
            latch.countDown();
        });

        latch.await();

        // 추가된 학생 + 강사
        Assertions.assertThat(sessionRepository.getConnectionsByLectureId(changeLongToString(lecture.getId())).size())
                .isEqualTo(numberOfThreads + 1);

        commonRequest.exitLive(teacher.getId(), lecture.getId());

        // 모두 제거된 상태
        Assertions.assertThat(sessionRepository.containsLectureSessionOnSessionManager(changeLongToString(lecture.getId())))
                .isFalse();

    }
}
