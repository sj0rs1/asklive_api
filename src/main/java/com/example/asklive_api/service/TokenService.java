package com.example.asklive_api.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class TokenService {

    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CODE_LENGTH = 6;
    private static final int CODE_ATTEMPTS = 20;

    private final SecureRandom random = new SecureRandom();

    public String generateSessionCode(Predicate<String> codeInUse) {
        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            String candidate = generateSessionCode();
            if (!codeInUse.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique session code");
    }

    private String generateSessionCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)]);
        }
        return code.toString();
    }

    public String generateToken() {
        return UUID.randomUUID().toString();
    }

}
