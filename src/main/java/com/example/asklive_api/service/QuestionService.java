package com.example.asklive_api.service;

import com.example.asklive_api.domain.Participant;
import com.example.asklive_api.domain.Question;
import com.example.asklive_api.domain.Session;
import com.example.asklive_api.domain.Upvote;
import com.example.asklive_api.dto.CreateQuestionRequest;
import com.example.asklive_api.dto.HostQuestionResponse;
import com.example.asklive_api.dto.QuestionResponse;
import com.example.asklive_api.dto.UpdateQuestionRequest;
import com.example.asklive_api.error.ApiException;
import com.example.asklive_api.repository.QuestionRepository;
import com.example.asklive_api.repository.UpvoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final UpvoteRepository upvoteRepository;
    private final SessionService sessionService;
    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final DtoMapper dtoMapper;
    private final SessionBroadcaster broadcaster;

    public QuestionService(QuestionRepository questionRepository, UpvoteRepository upvoteRepository,
            SessionService sessionService, AuthService authService, RateLimitService rateLimitService,
            DtoMapper dtoMapper, SessionBroadcaster broadcaster) {
        this.questionRepository = questionRepository;
        this.upvoteRepository = upvoteRepository;
        this.sessionService = sessionService;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.dtoMapper = dtoMapper;
        this.broadcaster = broadcaster;
    }

    @Transactional
    public QuestionResponse create(String code, String participantToken, CreateQuestionRequest request) {
        Session session = sessionService.requireSession(code);
        Participant participant = authService.requireParticipant(session, participantToken);
        rateLimitService.check(participant.getToken());
        Question question = questionRepository.save(new Question(session, participant, request.text().trim()));
        broadcaster.publishQuestions(session.getCode(), sessionService.visibleQuestions(session.getId()));
        return dtoMapper.toQuestionResponse(question);
    }

    @Transactional
    public QuestionResponse upvote(String code, Long questionId, String participantToken) {
        Session session = sessionService.requireSession(code);
        Participant participant = authService.requireParticipant(session, participantToken);
        rateLimitService.check(participant.getToken());
        Question question = requireQuestion(session, questionId);
        if (upvoteRepository.existsByQuestion_IdAndParticipant_Id(questionId, participant.getId())) {
            throw ApiException.conflict("You already upvoted this question");
        }
        upvoteRepository.save(new Upvote(question, participant));
        broadcaster.publishQuestions(session.getCode(), sessionService.visibleQuestions(session.getId()));
        return dtoMapper.toQuestionResponse(question);
    }

    @Transactional(readOnly = true)
    public List<HostQuestionResponse> listAllForHost(String code, String hostToken) {
        Session session = sessionService.requireSession(code);
        authService.requireHost(session, hostToken);
        return dtoMapper.toHostQuestionResponses(questionRepository.findBySession_IdOrderByIdAsc(session.getId()));
    }

    @Transactional
    public QuestionResponse updateStatus(String code, Long questionId, String hostToken,
            UpdateQuestionRequest request) {
        Session session = sessionService.requireSession(code);
        authService.requireHost(session, hostToken);
        Question question = requireQuestion(session, questionId);
        question.setStatus(request.status());
        broadcaster.publishQuestions(session.getCode(), sessionService.visibleQuestions(session.getId()));
        return dtoMapper.toQuestionResponse(question);
    }

    @Transactional
    public void delete(String code, Long questionId, String hostToken) {
        Session session = sessionService.requireSession(code);
        authService.requireHost(session, hostToken);
        questionRepository.delete(requireQuestion(session, questionId));
        broadcaster.publishQuestions(session.getCode(), sessionService.visibleQuestions(session.getId()));
    }

    private Question requireQuestion(Session session, Long questionId) {
        return questionRepository.findByIdAndSession_Id(questionId, session.getId())
                .orElseThrow(() -> ApiException.notFound("Unknown question"));
    }

}
