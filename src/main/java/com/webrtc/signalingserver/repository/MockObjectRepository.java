package com.webrtc.signalingserver.repository;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;

import java.util.HashMap;
import java.util.Map;

public class MockObjectRepository implements ObjectRepository {

    Map<Long, Member> members = new HashMap<>();
    Map<Long, Lecture> lectures = new HashMap<>();

    @Override
    public void saveLecture(Lecture lecture) {
        lectures.put(lecture.getId(), lecture);
    }

    @Override
    public void saveMember(Member member) {
        members.put(member.getId(), member);
    }

    @Override
    public Lecture findLecture(Long lectureId) {
        return lectures.get(lectureId);
    }

    @Override
    public Member findMember(Long memberId) {
        return members.get(memberId);
    }

    @Override
    public Boolean checkIfUserIsLecturerInLecture(Long lectureId, Long memberId) {
        return lectures.get(lectureId).getLecturer().getId().equals(memberId);
    }
}
