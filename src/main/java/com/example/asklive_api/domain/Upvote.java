package com.example.asklive_api.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "upvote", uniqueConstraints = @UniqueConstraint(name = "uk_upvote_question_participant",
        columnNames = { "question_id", "participant_id" }))
public class Upvote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    protected Upvote() {
    }

    public Upvote(Question question, Participant participant) {
        this.question = question;
        this.participant = participant;
    }

    public Long getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public Participant getParticipant() {
        return participant;
    }

}
