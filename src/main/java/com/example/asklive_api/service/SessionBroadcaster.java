package com.example.asklive_api.service;

import com.example.asklive_api.dto.PollResponse;
import com.example.asklive_api.dto.QuestionResponse;
import com.example.asklive_api.dto.SessionStatusResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
public class SessionBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public SessionBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishPoll(String code, PollResponse poll) {
        publishAfterCommit(code, "poll", poll);
    }

    public void publishQuestions(String code, List<QuestionResponse> questions) {
        publishAfterCommit(code, "questions", questions);
    }

    public void publishStatus(String code, SessionStatusResponse status) {
        publishAfterCommit(code, "status", status);
    }

    private void publishAfterCommit(String code, String topic, Object payload) {
        String destination = "/topic/session/" + code + "/" + topic;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messagingTemplate.convertAndSend(destination, payload);
                }
            });
        } else {
            messagingTemplate.convertAndSend(destination, payload);
        }
    }

}
