package com.webrtc.signalingserver;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class SessionConfig {

    @Bean
    public LectureSession lectureSession() {
        return new LectureSession();
    }

    @PostConstruct
    public void startLectureSession() {
        this.lectureSession().start();
        log.info("소켓 서버가 시작됩니다. port: {}", this.lectureSession().getPort());
    }
}
