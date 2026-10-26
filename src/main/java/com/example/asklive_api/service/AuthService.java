package com.example.asklive_api.service;

import com.example.asklive_api.domain.Participant;
import com.example.asklive_api.domain.Session;
import com.example.asklive_api.error.ApiException;
import com.example.asklive_api.repository.ParticipantRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final ParticipantRepository participantRepository;

    public AuthService(ParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    public void requireHost(Session session, String hostToken) {
        if (hostToken == null || !session.getHostToken().equals(hostToken)) {
            throw ApiException.forbidden("Missing or invalid host token");
        }
    }

    public Participant requireParticipant(Session session, String participantToken) {
        if (participantToken == null) {
            throw ApiException.forbidden("Missing participant token");
        }
        Participant participant = participantRepository.findByToken(participantToken)
                .orElseThrow(() -> ApiException.forbidden("Invalid participant token"));
        if (!participant.getSession().getId().equals(session.getId())) {
            throw ApiException.forbidden("Participant token does not belong to this session");
        }
        return participant;
    }

    public void requireHostOrParticipant(Session session, String hostToken, String participantToken) {
        if (hostToken != null && session.getHostToken().equals(hostToken)) {
            return;
        }
        requireParticipant(session, participantToken);
    }

}
