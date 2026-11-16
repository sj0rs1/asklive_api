package com.example.asklive_api;

import com.example.asklive_api.dto.CreatePollRequest;
import com.example.asklive_api.dto.CreateQuestionRequest;
import com.example.asklive_api.dto.JoinRequest;
import com.example.asklive_api.dto.VoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SessionApiIntegrationTest {

    private static final String HOST_TOKEN = "X-Host-Token";
    private static final String PARTICIPANT_TOKEN = "X-Participant-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("brokerChannel")
    private MessageChannel brokerChannel;

    @Autowired
    @Qualifier("clientOutboundChannel")
    private MessageChannel clientOutboundChannel;

    private static final List<Message<?>> SENT_MESSAGES = new CopyOnWriteArrayList<>();
    private static boolean interceptorsAdded = false;

    @BeforeEach
    void resetCapturedMessages() {
        SENT_MESSAGES.clear();
        if (!interceptorsAdded) {
            ChannelInterceptor capture = new ChannelInterceptor() {
                @Override
                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                    SENT_MESSAGES.add(message);
                    return message;
                }
            };
            if (brokerChannel instanceof AbstractSubscribableChannel subscribable) {
                subscribable.addInterceptor(capture);
            }
            if (clientOutboundChannel instanceof AbstractSubscribableChannel subscribable) {
                subscribable.addInterceptor(capture);
            }
            interceptorsAdded = true;
        }
    }

    @Test
    @DisplayName("BT-01 - create session: 201, unique 6-character code and host token")
    void createSessionReturnsCodeAndHostToken() throws Exception {
        Host first = createSession();
        Host second = createSession();

        // TokenService generates 6 characters (Session column allows up to 8).
        assertTrue(first.code().matches("[A-Z2-9]{6}"), "Unexpected code: " + first.code());
        assertFalse(first.token().isBlank(), "Host token must not be blank");
        assertNotEquals(first.code(), second.code(), "Session codes must be unique");
    }

    @Test
    @DisplayName("BT-02 - join with valid code: token returned and state readable")
    void joinWithValidCodeReturnsTokenAndState() throws Exception {
        Host host = createSession();
        Attendee attendee = join(host.code(), null);

        assertFalse(attendee.token().isBlank(), "Participant token must not be blank");
        mockMvc.perform(get("/api/sessions/{code}", host.code())
                        .header(PARTICIPANT_TOKEN, attendee.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(host.code()))
                .andExpect(jsonPath("$.participantCount").value(1));
    }

    @Test
    @DisplayName("BT-03 - join with invalid code: 404")
    void joinWithInvalidCodeReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/sessions/ZZZZZZ/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("BT-04 - join ended session: 409")
    void joinEndedSessionReturnsConflict() throws Exception {
        Host host = createSession();
        mockMvc.perform(post("/api/sessions/{code}/end", host.code())
                        .header(HOST_TOKEN, host.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/sessions/{code}/participants", host.code())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("BT-05 - anonymous question: authorName is null")
    void anonymousQuestionHasNoAuthorName() throws Exception {
        Host host = createSession();
        Attendee attendee = join(host.code(), null);
        askQuestion(host.code(), attendee.token(), "Anonymous question");

        mockMvc.perform(get("/api/sessions/{code}", host.code())
                        .header(PARTICIPANT_TOKEN, attendee.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].text").value("Anonymous question"))
                .andExpect(jsonPath("$.questions[0].authorName").value(nullValue()));
    }

    @Test
    @DisplayName("BT-09 - create poll with two options: 201, DRAFT")
    void createPollWithTwoOptionsReturnsDraft() throws Exception {
        Host host = createSession();

        mockMvc.perform(post("/api/sessions/{code}/polls", host.code())
                        .header(HOST_TOKEN, host.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePollRequest("Which one?", false, List.of("A", "B")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.options.length()").value(2));
    }

    @Test
    @DisplayName("BT-11 - launch poll: ACTIVE and STOMP /poll event")
    void launchPollSetsActiveAndBroadcasts() throws Exception {
        Host host = createSession();
        long pollId = createPoll(host, "Live?", List.of("Yes", "No"));

        mockMvc.perform(post("/api/sessions/{code}/polls/{pollId}/launch", host.code(), pollId)
                        .header(HOST_TOKEN, host.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        String expectedDestination = "/topic/session/" + host.code() + "/poll";
        Message<?> event = SENT_MESSAGES.stream()
                .filter(message -> expectedDestination.equals(destination(message)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No STOMP message sent to " + expectedDestination));
        assertTrue(payload(event).contains("\"status\":\"ACTIVE\""),
                "Expected ACTIVE poll in payload, got: " + payload(event));
    }

    @Test
    @DisplayName("BT-12 - launch second poll while one is active: 409")
    void launchSecondActivePollReturnsConflict() throws Exception {
        Host host = createSession();
        long first = createPoll(host, "First", List.of("A", "B"));
        long second = createPoll(host, "Second", List.of("A", "B"));

        launchPoll(host, first);
        mockMvc.perform(post("/api/sessions/{code}/polls/{pollId}/launch", host.code(), second)
                        .header(HOST_TOKEN, host.token()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("BT-14 - vote in active poll: 200 and vote count incremented")
    void voteInActivePollIncrementsCount() throws Exception {
        Host host = createSession();
        Attendee attendee = join(host.code(), "Alice");
        long pollId = createPoll(host, "Pick one", List.of("A", "B"));
        launchPoll(host, pollId);
        long optionId = firstOptionId(host.code(), attendee.token());

        mockMvc.perform(post("/api/sessions/{code}/polls/{pollId}/votes", host.code(), pollId)
                        .header(PARTICIPANT_TOKEN, attendee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VoteRequest(List.of(optionId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.options[0].votes").value(1));
    }

    @Test
    @DisplayName("BT-15 - vote twice: 409 and counts unchanged")
    void voteTwiceReturnsConflictAndCountsUnchanged() throws Exception {
        Host host = createSession();
        Attendee attendee = join(host.code(), "Alice");
        long pollId = createPoll(host, "Pick one", List.of("A", "B"));
        launchPoll(host, pollId);
        long optionId = firstOptionId(host.code(), attendee.token());
        String voteBody = objectMapper.writeValueAsString(new VoteRequest(List.of(optionId)));

        mockMvc.perform(post("/api/sessions/{code}/polls/{pollId}/votes", host.code(), pollId)
                        .header(PARTICIPANT_TOKEN, attendee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voteBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/sessions/{code}/polls/{pollId}/votes", host.code(), pollId)
                        .header(PARTICIPANT_TOKEN, attendee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voteBody))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/sessions/{code}", host.code())
                        .header(PARTICIPANT_TOKEN, attendee.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poll.options[0].votes").value(1));
    }

    @Test
    @DisplayName("BT-19 - submit question: 201 and appears in attendee list")
    void submitQuestionAppearsInAttendeeList() throws Exception {
        Host host = createSession();
        Attendee attendee = join(host.code(), "Bob");

        mockMvc.perform(post("/api/sessions/{code}/questions", host.code())
                        .header(PARTICIPANT_TOKEN, attendee.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateQuestionRequest("Why?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Why?"));

        mockMvc.perform(get("/api/sessions/{code}", host.code())
                        .header(PARTICIPANT_TOKEN, attendee.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].text").value("Why?"));
    }

    @Test
    @DisplayName("BT-21 - upvote: count +1 and question list re-sorted")
    void upvoteIncrementsCountAndReordersList() throws Exception {
        Host host = createSession();
        Attendee first = join(host.code(), "A");
        Attendee second = join(host.code(), "B");

        askQuestion(host.code(), first.token(), "Older question");
        long newer = askQuestion(host.code(), second.token(), "Newer question");

        mockMvc.perform(post("/api/sessions/{code}/questions/{questionId}/upvotes", host.code(), newer)
                        .header(PARTICIPANT_TOKEN, first.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1));

        mockMvc.perform(get("/api/sessions/{code}", host.code())
                        .header(PARTICIPANT_TOKEN, first.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].text").value("Newer question"))
                .andExpect(jsonPath("$.questions[0].upvotes").value(1));
    }

    @Test
    @DisplayName("BT-22 - upvote same question twice: 409 and count stays 1")
    void upvoteTwiceReturnsConflictAndCountUnchanged() throws Exception {
        Host host = createSession();
        Attendee author = join(host.code(), "A");
        Attendee voter = join(host.code(), "B");
        long questionId = askQuestion(host.code(), author.token(), "Question");

        mockMvc.perform(post("/api/sessions/{code}/questions/{questionId}/upvotes", host.code(), questionId)
                        .header(PARTICIPANT_TOKEN, voter.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upvotes").value(1));

        mockMvc.perform(post("/api/sessions/{code}/questions/{questionId}/upvotes", host.code(), questionId)
                        .header(PARTICIPANT_TOKEN, voter.token()))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/sessions/{code}", host.code())
                        .header(PARTICIPANT_TOKEN, voter.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions[0].upvotes").value(1));
    }

    private Host createSession() throws Exception {
        String json = mockMvc.perform(post("/api/sessions"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(json);
        return new Host(body.get("code").asString(), body.get("hostToken").asString());
    }

    private Attendee join(String code, String displayName) throws Exception {
        String request = displayName == null
                ? "{}"
                : objectMapper.writeValueAsString(new JoinRequest(displayName));
        String json = mockMvc.perform(post("/api/sessions/{code}/participants", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Attendee(objectMapper.readTree(json).get("participantToken").asString());
    }

    private long createPoll(Host host, String question, List<String> options) throws Exception {
        String json = mockMvc.perform(post("/api/sessions/{code}/polls", host.code())
                        .header(HOST_TOKEN, host.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreatePollRequest(question, false, options))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("pollId").asLong();
    }

    private void launchPoll(Host host, long pollId) throws Exception {
        mockMvc.perform(post("/api/sessions/{code}/polls/{pollId}/launch", host.code(), pollId)
                        .header(HOST_TOKEN, host.token()))
                .andExpect(status().isOk());
    }

    private long askQuestion(String code, String attendeeToken, String text) throws Exception {
        String json = mockMvc.perform(post("/api/sessions/{code}/questions", code)
                        .header(PARTICIPANT_TOKEN, attendeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateQuestionRequest(text))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private long firstOptionId(String code, String attendeeToken) throws Exception {
        String json = mockMvc.perform(get("/api/sessions/{code}", code)
                        .header(PARTICIPANT_TOKEN, attendeeToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("poll").get("options").get(0).get("id").asLong();
    }

    private static String destination(Message<?> message) {
        return SimpMessageHeaderAccessor.wrap(message).getDestination();
    }

    private static String payload(Message<?> message) {
        Object body = message.getPayload();
        if (body instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return body.toString();
    }

    private record Host(String code, String token) {
    }

    private record Attendee(String token) {
    }
}
