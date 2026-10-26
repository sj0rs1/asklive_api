package com.example.asklive_api.repository;

import com.example.asklive_api.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByCode(String code);

    boolean existsByCode(String code);

}
