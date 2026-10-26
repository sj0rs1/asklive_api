package com.example.asklive_api.service;

import com.example.asklive_api.domain.Poll;
import com.example.asklive_api.domain.PollOption;
import com.example.asklive_api.domain.Question;
import com.example.asklive_api.domain.Session;
import com.example.asklive_api.repository.PollRepository;
import com.example.asklive_api.repository.QuestionRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class CsvExporter {

    private static final String ANONYMOUS = "Anonymous";

    private final PollRepository pollRepository;
    private final QuestionRepository questionRepository;
    private final DtoMapper dtoMapper;

    public CsvExporter(PollRepository pollRepository, QuestionRepository questionRepository, DtoMapper dtoMapper) {
        this.pollRepository = pollRepository;
        this.questionRepository = questionRepository;
        this.dtoMapper = dtoMapper;
    }

    public byte[] export(Session session) {
        List<Poll> polls = pollRepository.findBySession_IdOrderByIdAsc(session.getId());
        List<Question> questions = questionRepository.findBySession_IdOrderByIdAsc(session.getId());

        List<Long> optionIds = polls.stream().flatMap(poll -> poll.getOptions().stream())
                .map(PollOption::getId).toList();
        Map<Long, Long> voteCounts = dtoMapper.countVotes(optionIds);
        Map<Long, Long> upvoteCounts = dtoMapper
                .countUpvotes(questions.stream().map(Question::getId).toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Question,Option,Votes\n");
        for (Poll poll : polls) {
            for (PollOption option : poll.getOptions()) {
                csv.append(escape(poll.getQuestion())).append(',')
                        .append(escape(option.getText())).append(',')
                        .append(voteCounts.getOrDefault(option.getId(), 0L)).append('\n');
            }
        }
        csv.append('\n').append("Question,Author,Upvotes\n");
        for (Question question : questions) {
            String author = question.getParticipant().getDisplayName();
            csv.append(escape(question.getText())).append(',')
                    .append(escape(author == null ? ANONYMOUS : author)).append(',')
                    .append(upvoteCounts.getOrDefault(question.getId(), 0L)).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        String cell = value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ");
        return "\"" + cell + "\"";
    }

}
