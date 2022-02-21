package com.webrtc.signalingserver;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class RedisTransactionTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private SetOperations<String, String> setOperations;

    @Test
    @DisplayName("트랜젝션 적용 - 중간에 값 조사시 null값 나오는지 확인")
    public void testTransaction1() {

        redisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.multi();
                valueOperations = operations.opsForValue();

                String key1 = "key1";
                String key2 = "key2";

                valueOperations.set(key1, "test1");

                String value = valueOperations.get(key1);
                Assertions.assertThat(value).isEqualTo(null);

                valueOperations.set(key2, "test2");

                // This will contain the results of all operations in the transaction
                return operations.exec();
            }
        });

    }

    /**
     * @Transactional를 사용했을 경우 Read-only 커맨드는 Transaction Queue에 들어가지 않고 바로 실행되며, Write 커맨드만 들어가게 된다.
     * 즉, 아래 실행에서는
     * ```
     * String value = valueOperations.get(key1);
     * Assertions.assertThat(value).isEqualTo(null);
     * ```
     * 이 null이 아닌 "test1"이 담기게 된다.
     */
    @Test
    @Transactional
    @DisplayName("Redis 설정에 트랜잭션 적용 및 @Transaction 사용으로 트랜잭션 실행")
    public void testTransaction2() {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        valueOperations = redisTemplate.opsForValue();

        String key1 = "key1";

        valueOperations.set(key1, "test1");

        String value = valueOperations.get(key1);
        Assertions.assertThat(value).isEqualTo("test1");

    }

    @Test
    @DisplayName("watch에 의해 낙관적 락이 걸리는 지 검증, 하나의 스레드의 요청만 반영이 되야함 (+1)")
    public void testTransaction3() throws InterruptedException {
        valueOperations = redisTemplate.opsForValue();
        //When
        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        String key = "test";
        String yet = valueOperations.get(key);

        service.submit(() -> {
            redisTransactionLogic(key);
            System.out.println("this = " + this);
            latch.countDown();
        });

        service.submit(() -> {
            redisTransactionLogic(key);
            System.out.println("this = " + this);
            latch.countDown();
        });

        latch.await();


        // Then
        String after = valueOperations.get(key);
        // watch에 의해 하나의 스레드의 요청만 반영된 상태 검증
        Assertions.assertThat(Integer.parseInt(after)).isEqualTo(Integer.parseInt(yet)+1);

    }

    private void redisTransactionLogic(String key) {
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                String s = valueOperations.get(key);
                int value = Integer.parseInt(s != null ? s : "0");

                redisTemplate.watch(key);
                operations.multi();

                valueOperations.set(key, Integer.toString(value+ 1));

                // This will contain the results of all operations in the transaction
                return operations.exec();
            }
        });
    }


    @Test
    @DisplayName("SessionCallback을 사용해 여러 클라이언트로부터 동시 요청시, 데이터 유실 피하기")
    public void testTransaction4() throws InterruptedException {
        setOperations = redisTemplate.opsForSet();
        //When
        int numberOfThreads = 2;
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        String key = "testSet";
        Long before = setOperations.size(key);


        service.submit(() -> {
            redisSetTransactionLogic(key);
            latch.countDown();
        });

        service.submit(() -> {
            redisSetTransactionLogic(key);
            latch.countDown();
        });

        latch.await();

        // Then
        Long after = setOperations.size(key);
        Assertions.assertThat(after).isEqualTo(before+numberOfThreads);

    }

    @Transactional
    public void redisSetTransactionLogic(String key) {
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            public List<Object> execute(RedisOperations operations) throws DataAccessException {

                operations.multi();

                setOperations.add(key, String.valueOf(this));
                System.out.println("this.toString() = " + this);

                return operations.exec();

            }
        });
    }

}
