package com.example.asklive_api.repository;

import com.example.asklive_api.domain.Poll;
import com.example.asklive_api.domain.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll, Long> {

    Optional<Poll> findByIdAndSession_Id(Long id, Long sessionId);

    Optional<Poll> findBySession_IdAndStatus(Long sessionId, PollStatus status);

    List<Poll> findBySession_IdOrderByIdAsc(Long sessionId);

}
