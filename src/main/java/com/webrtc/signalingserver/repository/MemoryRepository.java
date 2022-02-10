package com.webrtc.signalingserver.repository;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;

import java.util.HashMap;
import java.util.Map;

public class MemoryRepository implements ObjectRepository {

    Map<Long, Member> members = new HashMap<>();
    Map<Long, Lecture> lectures = new HashMap<>();

    @Override
    public void saveLecture(Lecture lecture) {
        lectures.put((long) (lectures.size()+1), lecture);
    }

    @Override
    public void saveMember(Member member) {
        members.put((long) (members.size()+1), member);
    }

    @Override
    public Lecture findLecture(Long lectureId) {
        return lectures.get(lectureId);
    }

    @Override
    public Member findMember(Long memberId) {
        return members.get(memberId);
    }
}
