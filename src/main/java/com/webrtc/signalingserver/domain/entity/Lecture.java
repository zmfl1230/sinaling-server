package com.webrtc.signalingserver.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Lecture {

    @Id @GeneratedValue
    @Column(name = "lecture_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member lecturer;

    @ManyToMany
    private List<Member> students = new ArrayList<>();

    public Lecture(Long id, Member lecturer) {
        this.id = id;
        this.lecturer = lecturer;
    }

    public Boolean contains(Member student) {
        return students.contains(student);
    }
}
