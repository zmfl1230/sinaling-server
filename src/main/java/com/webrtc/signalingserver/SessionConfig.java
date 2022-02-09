package com.webrtc.signalingserver;

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
        return new WebSocketService();
    }

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
