package com.webrtc.signalingserver;

import io.lettuce.core.RedisCommandExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class RedisTemplateTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    private SetOperations<String, String> setOperations;
    private HashOperations<String, Object, Object> hashOperations;
    private ListOperations<String, String> listOperations;

    private final String key = "default";

    @AfterEach
    public void setDown() {
        redisTemplate.delete(key);
    }

    @Test
    void testSet() {
        // given
        setOperations = redisTemplate.opsForSet();

        // when
        setOperations.add(key, "h", "e", "l", "l", "o");

        // then
        /**
         * member(String key)
         * Get all elements of set at key.
         * Params: key – must not be null.
         * Returns: null when used in pipeline / transaction.
         */
        Set<String> members = setOperations.members(key);
        Long size = setOperations.size(key);

        assertThat(members).containsOnly("h", "e", "l", "o");
        assertThat(size).isEqualTo(4);
    }

    @Test
    public void checkContainsValueUsingSize(){
        setOperations = redisTemplate.opsForSet();

        assertThat(setOperations.size(key) != 0).isFalse();
        setOperations.add(key, "h", "e", "l", "l", "o");

        System.out.println("setOperations.size(key) = " + setOperations.size(key));
        assertThat(setOperations.size(key) != 0).isTrue();

    }

    @Test
    public void containsConnectionOnLectureSessionTest(){
        setOperations = redisTemplate.opsForSet();

        System.out.println("setOperations.members(key) = " + setOperations.members(key));
        assertThat(Objects.requireNonNull(setOperations.members(key)).contains("l")).isFalse();
        setOperations.add(key, "h", "e", "l", "l", "o");

        assertThat(setOperations.members(key).contains("l")).isTrue();

    }

    @Test
    public void getSessionsByLectureIdTest(){
        setOperations = redisTemplate.opsForSet();

        List<String> changedType = new ArrayList<>(Objects.requireNonNull(setOperations.members(key)));
        System.out.println("setOperations.members(key) = " + changedType);

        assertThat(changedType.size()).isEqualTo(0);
        setOperations.add(key, "h", "e", "l", "l", "o");

        changedType = new ArrayList<>(setOperations.members(key));
        for (String s : changedType) {
            System.out.println("s = " + s);
        }

        assertThat(changedType.size()).isEqualTo(4);
    }


    /**
     * 왜 hashOperations에서 데이터를 저장할 때, void put(H key, HK hashKey, HV value);
     * 와 같이 keu와 hashKey를 분리해서 인자로 넘겨줄까
     * -> 이는 객체를 { key: value } 쌍으로 손쉽게 저장하기 위함이다.
     *
     * 사용자 객체를 저장하기 위한 다음과 같은 예가 있다.
     * redisTemplate.opsForHash().put("userid:1000", "username", "Liu Yue")
     * redisTemplate.opsForHash().put("userid:1000", "password", "123456")
     * redisTemplate.opsForHash().put("userid:1000", "age", "32")
     *
     * 즉, userid:1000이라는 kay 값 내부적으로 사용자 객체를 나타내는 3개의 { key: value } 쌍이 존재하며,
     * 이를 userid:1000 키 내부적으로 존재하는 여러 키 값들을 분리하기 위해 존재한다고 볼 수 있다.
     *
     * @see <a href="https://stackoverflow.com/questions/46062283/what-is-the-difference-between-the-key-and-hash-key-parameters-used-in-a-redis-p">관련 링크</a>
     *
     */

    @Test
    @Transactional
    void testHash() {
        // given
        hashOperations = redisTemplate.opsForHash();

        // when
        hashOperations.put(key, "hello1", "test1");
        hashOperations.put(key, "hello2", "test2");

        // then
        Object value = hashOperations.get(key, "hello1");
        assertThat(value).isEqualTo("test1");

        Map<Object, Object> entries = hashOperations.entries(key);

        assertThat(entries.keySet()).contains("hello1");
        assertThat(entries.values()).contains("test1");

        Long size = hashOperations.size(key);
        assertThat(size).isEqualTo(entries.size());
    }

    /**
     * leftPush - Insert all the specified values at the head of the list stored at key.
     * 데이터를 넣으면 맨 앞에 채워지는 구조, index 0 부터 출력할 시 가장 나중에 넣은 것부터 출력된다.
     *
     * rightPush - Insert all the specified values at the tail of the list stored at key.
     * 데이터를 넣으면 맨 뒤에 채워지는 구조, index 0 부터 출력할 시 가장 나중에 넣은 것이 가장 나중에 출력된다.
     */
    @Test
    @Transactional
    public void testList() {

        //Given
        listOperations = redisTemplate.opsForList();

        //When

        listOperations.rightPush(key, "studentId");
        listOperations.rightPush(key, "studentId");

        //Then
        assertThat(listOperations.size(key)).isEqualTo(2);
        List<String> objects = listOperations.range(key, 1, listOperations.size(key));
        for (Object object : objects) {
            System.out.println("object = " + object);
            listOperations.remove(key,1, object);
        }

    }

    @Test
    @DisplayName("Redis는 하나의 키를 공유한다.")
    public void keyTest() {
        setOperations = redisTemplate.opsForSet();
        hashOperations = redisTemplate.opsForHash();

        hashOperations.put(key, "hashKey", "testHash");
        redisTemplate.delete(key);
        setOperations.add(key, "testSet", "testSet2");

        assertThat(setOperations.members(key)).contains("testSet");
    }

    @Test
    @DisplayName("Redis는 하나의 키를 공유한다.- 키 제거 안한 버전")
    public void keyTestNotRemoveKey() {
        setOperations = redisTemplate.opsForSet();
        hashOperations = redisTemplate.opsForHash();

        hashOperations.put(key, "hashKey", "testHash");

        assertThrows(RedisSystemException.class,
                () -> setOperations.add(key, "testSet", "testSet2"));
    }

}
