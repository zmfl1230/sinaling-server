package com.webrtc.signalingserver.service.redis;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;
import com.webrtc.signalingserver.domain.entity.MemberRole;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import com.webrtc.signalingserver.service.CommonRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.webrtc.signalingserver.util.EncryptString.changeLongToString;

@SpringBootTest
public class SynchronizedUsingTransactionManagerTest {

    @Autowired
    ObjectRepository objectRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    CommonRequest commonRequest;

    @Autowired
    PlatformTransactionManager platformTransactionManager;

    TransactionTemplate transactionTemplate;

    Member teacher;
    Lecture lecture;
    Member firstStudent;
    Member secondStudent;

    @BeforeEach
    public void setUp() {
        teacher = new Member();
        teacher.setName("teacher");
        teacher.setRole(MemberRole.LECTURER);

        lecture = new Lecture();
        lecture.setLecturer(teacher);

        firstStudent = new Member();
        firstStudent.setName("firstStudent");
        firstStudent.setRole(MemberRole.STUDENT);

        secondStudent = new Member();
        secondStudent.setName("secondStudent");
        secondStudent.setRole(MemberRole.STUDENT);

        lecture.getStudents().add(firstStudent);
        lecture.getStudents().add(secondStudent);

    }

    private void saveEntity() {
        objectRepository.saveMember(teacher);
        objectRepository.saveMember(firstStudent);
        objectRepository.saveMember(secondStudent);

        objectRepository.saveLecture(lecture);
    }

    @Test
    public void enterLiveSynchronizedTest() throws InterruptedException {

        int numberOfThreads = 2;
        transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.execute(status -> {
            saveEntity();
            commonRequest.startLive(teacher.getId(), lecture.getId());
            return null;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        executorService.execute(() -> transactionTemplate.execute(status -> {
            commonRequest.enterLive(firstStudent.getId(), lecture.getId());
            return null;
        }));
        executorService.execute(() -> transactionTemplate.execute(status -> {
            commonRequest.enterLive(secondStudent.getId(), lecture.getId());
            return null;
        }));

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        transactionTemplate.execute(status -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e){
                System.out.println("e = " + e);
            }
            // 추가된 학생 + 강사
            Assertions.assertThat(sessionRepository.getConnectionsByLectureId(changeLongToString(lecture.getId())).size())
                    .isEqualTo(numberOfThreads + 1);

            commonRequest.exitLive(teacher.getId(), lecture.getId());

            // 모두 제거된 상태
            Assertions.assertThat(sessionRepository.containsLectureSessionOnSessionManager(changeLongToString(lecture.getId())))
                    .isFalse();
            return null;
        });

    }

    @Test //note that there is no @Transactional configured for the method
    public void enterWaitingRoomSynchronizedTest() throws InterruptedException {
        int numberOfThreads = 2;
        transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.execute(status -> {
            saveEntity();
            return null;
        });

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        executorService.execute(() -> transactionTemplate.execute(status -> {
            commonRequest.enterWaitingRoom(firstStudent.getId(), lecture.getId());
            return null;
        }));
        executorService.execute(() -> transactionTemplate.execute(status -> {
            commonRequest.enterWaitingRoom(secondStudent.getId(), lecture.getId());
            return null;
        }));

        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);

        transactionTemplate.execute(status -> {
            // validate test results in transaction
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e){
                System.out.println("e = " + e);
            }

            Assertions.assertThat(sessionRepository.getConnectionsOnWaitingRoom(changeLongToString(lecture.getId())).size())
                    .isEqualTo(2);

            commonRequest.startLive(teacher.getId(), lecture.getId());
            commonRequest.exitLive(teacher.getId(), lecture.getId());
            return null;
        });

    }

}
