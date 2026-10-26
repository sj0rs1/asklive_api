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

@Entity
@Table(name = "vote", uniqueConstraints = @UniqueConstraint(name = "uk_vote_option_participant",
        columnNames = { "option_id", "participant_id" }))
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption option;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    protected Vote() {
    }

    public Vote(PollOption option, Participant participant) {
        this.option = option;
        this.participant = participant;
    }

    public Long getId() {
        return id;
    }

    public PollOption getOption() {
        return option;
    }

    public Participant getParticipant() {
        return participant;
    }

}
