package com.example.asklive_api.service;

import com.example.asklive_api.domain.Participant;
import com.example.asklive_api.domain.Poll;
import com.example.asklive_api.domain.PollStatus;
import com.example.asklive_api.domain.Question;
import com.example.asklive_api.domain.QuestionStatus;
import com.example.asklive_api.domain.Session;
import com.example.asklive_api.domain.SessionStatus;
import com.example.asklive_api.dto.CreateSessionResponse;
import com.example.asklive_api.dto.JoinRequest;
import com.example.asklive_api.dto.JoinResponse;
import com.example.asklive_api.dto.QuestionResponse;
import com.example.asklive_api.dto.SessionStateResponse;
import com.example.asklive_api.dto.SessionStatusResponse;
import com.example.asklive_api.error.ApiException;
import com.example.asklive_api.repository.ParticipantRepository;
import com.example.asklive_api.repository.PollRepository;
import com.example.asklive_api.repository.QuestionRepository;
import com.example.asklive_api.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final PollRepository pollRepository;
    private final QuestionRepository questionRepository;
    private final TokenService tokenService;
    private final AuthService authService;
    private final DtoMapper dtoMapper;
    private final CsvExporter csvExporter;
    private final SessionBroadcaster broadcaster;

    public SessionService(SessionRepository sessionRepository, ParticipantRepository participantRepository,
            PollRepository pollRepository, QuestionRepository questionRepository, TokenService tokenService,
            AuthService authService, DtoMapper dtoMapper, CsvExporter csvExporter,
            SessionBroadcaster broadcaster) {
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.pollRepository = pollRepository;
        this.questionRepository = questionRepository;
        this.tokenService = tokenService;
        this.authService = authService;
        this.dtoMapper = dtoMapper;
        this.csvExporter = csvExporter;
        this.broadcaster = broadcaster;
    }

    public Session requireSession(String code) {
        return sessionRepository.findByCode(code).orElseThrow(() -> ApiException.notFound("Unknown session code"));
    }

    @Transactional
    public CreateSessionResponse create() {
        String code = tokenService.generateSessionCode(sessionRepository::existsByCode);
        Session session = sessionRepository.save(new Session(code, tokenService.generateToken()));
        return new CreateSessionResponse(session.getCode(), session.getHostToken());
    }

    @Transactional(readOnly = true)
    public SessionStateResponse getState(String code, String hostToken, String participantToken) {
        Session session = requireSession(code);
        authService.requireHostOrParticipant(session, hostToken, participantToken);
        Poll activePoll = pollRepository.findBySession_IdAndStatus(session.getId(), PollStatus.ACTIVE).orElse(null);
        return new SessionStateResponse(session.getCode(), session.getStatus(),
                activePoll != null ? dtoMapper.toPollResponse(activePoll) : null,
                visibleQuestions(session.getId()));
    }

    @Transactional
    public void end(String code, String hostToken) {
        Session session = requireSession(code);
        authService.requireHost(session, hostToken);
        session.setStatus(SessionStatus.ENDED);
        broadcaster.publishStatus(session.getCode(), new SessionStatusResponse(SessionStatus.ENDED));
    }

    @Transactional
    public JoinResponse join(String code, JoinRequest request) {
        Session session = requireSession(code);
        if (session.getStatus() == SessionStatus.ENDED) {
            throw ApiException.conflict("This session has ended");
        }
        String displayName = request == null ? null : trimToNull(request.displayName());
        Participant participant = participantRepository
                .save(new Participant(session, tokenService.generateToken(), displayName));
        return new JoinResponse(participant.getToken());
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String code, String hostToken) {
        Session session = requireSession(code);
        authService.requireHost(session, hostToken);
        return csvExporter.export(session);
    }

    public List<QuestionResponse> visibleQuestions(Long sessionId) {
        return dtoMapper.toQuestionResponses(
                questionRepository.findBySession_IdAndStatusOrderByCreatedAtAsc(sessionId, QuestionStatus.VISIBLE));
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
