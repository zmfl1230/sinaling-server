package com.webrtc.signalingserver.exception;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;

public class ValidatePermission {

    public static void validateLecturer(Long lecturerId, Lecture lecture) {
        if(lecturerId == null || lecture == null || !lecturerId.equals(lecture.getLecturer().getId())) throw new IllegalArgumentException("권한이 없는 사용자입니다.");
    }

    public static void validateAccessPermission(Member student, Lecture lecture) {
        if(student == null || lecture == null || !lecture.contains(student)) throw new IllegalArgumentException("권한이 없는 사용자입니다.");
    }
}
