package com.example.asklive_api.repository;

import com.example.asklive_api.domain.Question;
import com.example.asklive_api.domain.QuestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByIdAndSession_Id(Long id, Long sessionId);

    List<Question> findBySession_IdAndStatusOrderByCreatedAtAsc(Long sessionId, QuestionStatus status);

    List<Question> findBySession_IdOrderByIdAsc(Long sessionId);

}
