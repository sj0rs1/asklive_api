package com.example.asklive_api.repository;

import com.example.asklive_api.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsByParticipant_IdAndOption_Poll_Id(Long participantId, Long pollId);

    @Query("select v.option.id, count(v) from Vote v where v.option.id in :optionIds group by v.option.id")
    List<Object[]> countVotesByOptionIds(@Param("optionIds") Collection<Long> optionIds);

}
