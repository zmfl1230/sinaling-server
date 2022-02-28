package com.webrtc.signalingserver.repository;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;


public interface ObjectRepository {
    void saveLecture(Lecture lecture);
    void saveMember(Member member);
    Lecture findLecture(Long lectureId);
    Member findMember(Long memberId);

    Boolean checkIfUserIsLecturerInLecture(Long lectureId, Long memberId);
}
