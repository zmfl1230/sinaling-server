package com.webrtc.signalingserver.config;

import com.webrtc.signalingserver.LectureSession;
import com.webrtc.signalingserver.repository.MemoryRepository;
import com.webrtc.signalingserver.repository.MemorySessionRepository;
import com.webrtc.signalingserver.repository.ObjectRepository;
import com.webrtc.signalingserver.repository.SessionRepository;
import com.webrtc.signalingserver.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class SessionConfig {

    @Bean
    public LectureSession lectureSession() {
        return new LectureSession();
    }

    @Bean
    WebSocketService webSocketService() {
        return new WebSocketService(objectRepository(), sessionRepository());
    }

    @Bean
    public ObjectRepository objectRepository() {
        return new MemoryRepository();
    }

    @Bean
    public SessionRepository sessionRepository() {return new MemorySessionRepository();}

    @PostConstruct
    public void startLectureSession() {
        this.lectureSession().start();
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
