package com.example.asklive_api.service;

import com.example.asklive_api.domain.Poll;
import com.example.asklive_api.domain.PollOption;
import com.example.asklive_api.domain.Question;
import com.example.asklive_api.dto.HostQuestionResponse;
import com.example.asklive_api.dto.PollOptionResponse;
import com.example.asklive_api.dto.PollResponse;
import com.example.asklive_api.dto.QuestionResponse;
import com.example.asklive_api.repository.PollOptionRepository;
import com.example.asklive_api.repository.UpvoteRepository;
import com.example.asklive_api.repository.VoteRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class DtoMapper {

    private final PollOptionRepository pollOptionRepository;
    private final VoteRepository voteRepository;
    private final UpvoteRepository upvoteRepository;

    public DtoMapper(PollOptionRepository pollOptionRepository, VoteRepository voteRepository,
            UpvoteRepository upvoteRepository) {
        this.pollOptionRepository = pollOptionRepository;
        this.voteRepository = voteRepository;
        this.upvoteRepository = upvoteRepository;
    }

    public PollResponse toPollResponse(Poll poll) {
        List<PollOption> options = pollOptionRepository.findByPoll_IdOrderByIdAsc(poll.getId());
        Map<Long, Long> voteCounts = countVotes(options.stream().map(PollOption::getId).toList());
        List<PollOptionResponse> optionResponses = options.stream()
                .map(option -> new PollOptionResponse(option.getId(), option.getText(),
                        voteCounts.getOrDefault(option.getId(), 0L)))
                .toList();
        return new PollResponse(poll.getId(), poll.getQuestion(), poll.isMultipleChoice(), poll.getStatus(),
                optionResponses);
    }

    public QuestionResponse toQuestionResponse(Question question) {
        return toQuestionResponses(List.of(question)).getFirst();
    }

    public List<QuestionResponse> toQuestionResponses(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> upvoteCounts = countUpvotes(questions.stream().map(Question::getId).toList());
        return questions.stream()
                .map(question -> new QuestionResponse(question.getId(), question.getText(),
                        question.getParticipant().getDisplayName(),
                        upvoteCounts.getOrDefault(question.getId(), 0L), question.getCreatedAt()))
                .sorted(Comparator.comparingLong(QuestionResponse::upvotes).reversed()
                        .thenComparing(QuestionResponse::createdAt))
                .toList();
    }

    public List<HostQuestionResponse> toHostQuestionResponses(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> upvoteCounts = countUpvotes(questions.stream().map(Question::getId).toList());
        return questions.stream()
                .map(question -> new HostQuestionResponse(question.getId(), question.getText(),
                        question.getParticipant().getDisplayName(),
                        upvoteCounts.getOrDefault(question.getId(), 0L), question.getCreatedAt(),
                        question.getStatus()))
                .sorted(Comparator.comparingLong(HostQuestionResponse::upvotes).reversed()
                        .thenComparing(HostQuestionResponse::createdAt))
                .toList();
    }

    public Map<Long, Long> countVotes(Collection<Long> optionIds) {
        return countBy(optionIds, voteRepository::countVotesByOptionIds);
    }

    public Map<Long, Long> countUpvotes(Collection<Long> questionIds) {
        return countBy(questionIds, upvoteRepository::countUpvotesByQuestionIds);
    }

    private Map<Long, Long> countBy(Collection<Long> ids, Function<Collection<Long>, List<Object[]>> counter) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : counter.apply(ids)) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return counts;
    }

}
