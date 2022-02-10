package com.webrtc.signalingserver.repository;

import com.webrtc.signalingserver.domain.entity.Lecture;
import com.webrtc.signalingserver.domain.entity.Member;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class JpaRepository implements ObjectRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public void saveLecture(Lecture lecture) {
        entityManager.persist(lecture);
    }

    @Override
    public void saveMember(Member member) {
        entityManager.persist(member);
    }

    @Override
    public Lecture findLecture(Long lectureId) {
        return entityManager.find(Lecture.class, lectureId);
    }

    @Override
    public Member findMember(Long memberId) {
        return entityManager.find(Member.class, memberId);
    }
}
