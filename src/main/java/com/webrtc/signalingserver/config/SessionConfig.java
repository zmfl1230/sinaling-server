package com.webrtc.signalingserver.config;

import com.webrtc.signalingserver.LectureSession;
import com.webrtc.signalingserver.repository.*;
import com.webrtc.signalingserver.service.TemplateForSynchronized;
import com.webrtc.signalingserver.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class SessionConfig {
    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Bean
    public LectureSession lectureSession() {
        return new LectureSession();
    }

    @Bean
    WebSocketService webSocketService() {
        return new WebSocketService(objectRepository(), sessionRepository(), templateForSynchronized());
    }

    @Bean TemplateForSynchronized templateForSynchronized() {
        return new TemplateForSynchronized();
    }

    @Bean
    public ObjectRepository objectRepository() {
        return new JpaRepository();
    }

    @Bean
    public SessionRepository sessionRepository() {
//        return new MemorySessionRepository();
        return new RedisSessionRepository(redisTemplate);
    }

    @PostConstruct
    public void startLectureSession() {
        this.lectureSession().start();
        this.lectureSession().setWebSocketService(webSocketService());
        this.lectureSession().setMethodMap(lectureSessionInit());
        log.info("소켓 서버가 시작됩니다. port: {}", this.lectureSession().getPort());
    }

    private Map<String, Method> lectureSessionInit() {
        Map<String, Method> methodMap = new HashMap<>();
        Method[] methods = WebSocketService.class.getMethods();

        for (Method method : methods) {
            methodMap.put(method.getName().toLowerCase(), method);
        }

        return methodMap;
    }
}
