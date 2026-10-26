package com.example.asklive_api.repository;

import com.example.asklive_api.domain.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByPoll_IdOrderByIdAsc(Long pollId);

    List<PollOption> findByIdInAndPoll_Id(Collection<Long> ids, Long pollId);

}
