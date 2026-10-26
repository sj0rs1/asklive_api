package com.example.asklive_api.repository;

import com.example.asklive_api.domain.Upvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UpvoteRepository extends JpaRepository<Upvote, Long> {

    boolean existsByQuestion_IdAndParticipant_Id(Long questionId, Long participantId);

    @Query("select u.question.id, count(u) from Upvote u where u.question.id in :questionIds group by u.question.id")
    List<Object[]> countUpvotesByQuestionIds(@Param("questionIds") Collection<Long> questionIds);

}
