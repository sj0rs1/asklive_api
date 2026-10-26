package com.example.asklive_api.controller;

import com.example.asklive_api.dto.CreatePollRequest;
import com.example.asklive_api.dto.CreateQuestionRequest;
import com.example.asklive_api.dto.CreateSessionResponse;
import com.example.asklive_api.dto.HostQuestionResponse;
import com.example.asklive_api.dto.JoinRequest;
import com.example.asklive_api.dto.JoinResponse;
import com.example.asklive_api.dto.PollResponse;
import com.example.asklive_api.dto.QuestionResponse;
import com.example.asklive_api.dto.SessionStateResponse;
import com.example.asklive_api.dto.UpdateQuestionRequest;
import com.example.asklive_api.dto.VoteRequest;
import com.example.asklive_api.service.PollService;
import com.example.asklive_api.service.QuestionService;
import com.example.asklive_api.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private static final String HOST_TOKEN = "X-Host-Token";
    private static final String PARTICIPANT_TOKEN = "X-Participant-Token";

    private final SessionService sessionService;
    private final PollService pollService;
    private final QuestionService questionService;

    public SessionController(SessionService sessionService, PollService pollService,
            QuestionService questionService) {
        this.sessionService = sessionService;
        this.pollService = pollService;
        this.questionService = questionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSessionResponse create() {
        return sessionService.create();
    }

    @GetMapping("/{code}")
    public SessionStateResponse getState(@PathVariable String code,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken,
            @RequestHeader(value = PARTICIPANT_TOKEN, required = false) String participantToken) {
        return sessionService.getState(code, hostToken, participantToken);
    }

    @PostMapping("/{code}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(@PathVariable String code,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken) {
        sessionService.end(code, hostToken);
    }

    @PostMapping("/{code}/participants")
    @ResponseStatus(HttpStatus.CREATED)
    public JoinResponse join(@PathVariable String code,
            @RequestBody(required = false) JoinRequest request) {
        return sessionService.join(code, request);
    }

    @GetMapping("/{code}/polls")
    public List<PollResponse> listPolls(@PathVariable String code,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken,
            @RequestHeader(value = PARTICIPANT_TOKEN, required = false) String participantToken) {
        return pollService.listAll(code, hostToken, participantToken);
    }

    @PostMapping("/{code}/polls")
    @ResponseStatus(HttpStatus.CREATED)
    public PollResponse createPoll(@PathVariable String code,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken,
            @Valid @RequestBody CreatePollRequest request) {
        return pollService.create(code, hostToken, request);
    }

    @PostMapping("/{code}/polls/{pollId}/launch")
    public PollResponse launchPoll(@PathVariable String code, @PathVariable Long pollId,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken) {
        return pollService.launch(code, pollId, hostToken);
    }

    @PostMapping("/{code}/polls/{pollId}/close")
    public PollResponse closePoll(@PathVariable String code, @PathVariable Long pollId,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken) {
        return pollService.close(code, pollId, hostToken);
    }

    @PostMapping("/{code}/polls/{pollId}/votes")
    public PollResponse vote(@PathVariable String code, @PathVariable Long pollId,
            @RequestHeader(value = PARTICIPANT_TOKEN, required = false) String participantToken,
            @Valid @RequestBody VoteRequest request) {
        return pollService.vote(code, pollId, participantToken, request);
    }

    @GetMapping("/{code}/questions")
    public List<HostQuestionResponse> listQuestionsForHost(@PathVariable String code,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken) {
        return questionService.listAllForHost(code, hostToken);
    }

    @PostMapping("/{code}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse createQuestion(@PathVariable String code,
            @RequestHeader(value = PARTICIPANT_TOKEN, required = false) String participantToken,
            @Valid @RequestBody CreateQuestionRequest request) {
        return questionService.create(code, participantToken, request);
    }

    @PostMapping("/{code}/questions/{questionId}/upvotes")
    public QuestionResponse upvote(@PathVariable String code, @PathVariable Long questionId,
            @RequestHeader(value = PARTICIPANT_TOKEN, required = false) String participantToken) {
        return questionService.upvote(code, questionId, participantToken);
    }

    @PatchMapping("/{code}/questions/{questionId}")
    public QuestionResponse updateQuestion(@PathVariable String code, @PathVariable Long questionId,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return questionService.updateStatus(code, questionId, hostToken, request);
    }

    @DeleteMapping("/{code}/questions/{questionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable String code, @PathVariable Long questionId,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken) {
        questionService.delete(code, questionId, hostToken);
    }

    @GetMapping("/{code}/export")
    public ResponseEntity<byte[]> export(@PathVariable String code,
            @RequestHeader(value = HOST_TOKEN, required = false) String hostToken) {
        byte[] csv = sessionService.exportCsv(code, hostToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("asklive-" + code + ".csv").build());
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

}
