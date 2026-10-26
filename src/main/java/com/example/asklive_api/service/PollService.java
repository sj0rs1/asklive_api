package com.example.asklive_api.service;

import com.example.asklive_api.domain.Participant;
import com.example.asklive_api.domain.Poll;
import com.example.asklive_api.domain.PollOption;
import com.example.asklive_api.domain.PollStatus;
import com.example.asklive_api.domain.Session;
import com.example.asklive_api.domain.Vote;
import com.example.asklive_api.dto.CreatePollRequest;
import com.example.asklive_api.dto.PollResponse;
import com.example.asklive_api.dto.VoteRequest;
import com.example.asklive_api.error.ApiException;
import com.example.asklive_api.repository.PollOptionRepository;
import com.example.asklive_api.repository.PollRepository;
import com.example.asklive_api.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Service
public class PollService {

    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final VoteRepository voteRepository;
    private final SessionService sessionService;
    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final DtoMapper dtoMapper;
    private final SessionBroadcaster broadcaster;

    public PollService(PollRepository pollRepository, PollOptionRepository pollOptionRepository,
            VoteRepository voteRepository, SessionService sessionService, AuthService authService,
            RateLimitService rateLimitService, DtoMapper dtoMapper, SessionBroadcaster broadcaster) {
        this.pollRepository = pollRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.voteRepository = voteRepository;
        this.sessionService = sessionService;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.dtoMapper = dtoMapper;
        this.broadcaster = broadcaster;
    }

    @Transactional(readOnly = true)
    public List<PollResponse> listAll(String code, String hostToken, String participantToken) {
        Session session = sessionService.requireSession(code);
        authService.requireHostOrParticipant(session, hostToken, participantToken);
        return pollRepository.findBySession_IdOrderByIdAsc(session.getId()).stream()
                .map(dtoMapper::toPollResponse)
                .toList();
    }

    @Transactional
    public PollResponse create(String code, String hostToken, CreatePollRequest request) {
        Session session = sessionService.requireSession(code);
        authService.requireHost(session, hostToken);
        Poll poll = new Poll(session, request.question().trim(), Boolean.TRUE.equals(request.multipleChoice()));
        for (String option : request.options()) {
            poll.addOption(option.trim());
        }
        return dtoMapper.toPollResponse(pollRepository.save(poll));
    }

    @Transactional
    public PollResponse launch(String code, Long pollId, String hostToken) {
        Session session = sessionService.requireSession(code);
        authService.requireHost(session, hostToken);
        Poll poll = requirePoll(session, pollId);
        Optional<Poll> activePoll = pollRepository.findBySession_IdAndStatus(session.getId(), PollStatus.ACTIVE);
        if (activePoll.isPresent() && !activePoll.get().getId().equals(pollId)) {
            throw ApiException.conflict("Another poll is already active in this session");
        }
        poll.setStatus(PollStatus.ACTIVE);
        return publish(session.getCode(), poll);
    }

    @Transactional
    public PollResponse close(String code, Long pollId, String hostToken) {
        Session session = sessionService.requireSession(code);
        authService.requireHost(session, hostToken);
        Poll poll = requirePoll(session, pollId);
        poll.setStatus(PollStatus.CLOSED);
        return publish(session.getCode(), poll);
    }

    @Transactional
    public PollResponse vote(String code, Long pollId, String participantToken, VoteRequest request) {
        Session session = sessionService.requireSession(code);
        Participant participant = authService.requireParticipant(session, participantToken);
        rateLimitService.check(participant.getToken());
        Poll poll = requirePoll(session, pollId);
        if (poll.getStatus() != PollStatus.ACTIVE) {
            throw ApiException.conflict("This poll is not active");
        }
        List<Long> optionIds = new ArrayList<>(new LinkedHashSet<>(request.optionIds()));
        if (!poll.isMultipleChoice() && optionIds.size() > 1) {
            throw ApiException.badRequest("This poll only accepts a single option");
        }
        List<PollOption> options = pollOptionRepository.findByIdInAndPoll_Id(optionIds, pollId);
        if (options.size() != optionIds.size()) {
            throw ApiException.badRequest("One or more options do not belong to this poll");
        }
        if (voteRepository.existsByParticipant_IdAndOption_Poll_Id(participant.getId(), pollId)) {
            throw ApiException.conflict("You already voted in this poll");
        }
        for (PollOption option : options) {
            voteRepository.save(new Vote(option, participant));
        }
        return publish(session.getCode(), poll);
    }

    private Poll requirePoll(Session session, Long pollId) {
        return pollRepository.findByIdAndSession_Id(pollId, session.getId())
                .orElseThrow(() -> ApiException.notFound("Unknown poll"));
    }

    private PollResponse publish(String code, Poll poll) {
        PollResponse response = dtoMapper.toPollResponse(poll);
        broadcaster.publishPoll(code, response);
        return response;
    }

}
