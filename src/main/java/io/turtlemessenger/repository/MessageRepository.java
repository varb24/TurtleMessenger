package io.turtlemessenger.repository;

import io.turtlemessenger.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByRoom_IdAndCreatedAtLessThanOrderByCreatedAtDesc(Long roomId, Instant before, Pageable pageable);
    List<Message> findByRoom_IdOrderByCreatedAtDesc(Long roomId, Pageable pageable);
}

