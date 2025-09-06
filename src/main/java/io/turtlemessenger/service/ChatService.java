package io.turtlemessenger.service;

import io.turtlemessenger.dto.MessageDTO;
import io.turtlemessenger.model.ChatRoom;
import io.turtlemessenger.model.Message;
import io.turtlemessenger.model.UserAccount;
import io.turtlemessenger.repository.ChatRoomRepository;
import io.turtlemessenger.repository.MessageRepository;
import io.turtlemessenger.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatService {
    private final ChatRoomRepository rooms;
    private final MessageRepository messages;
    private final UserRepository users;

    public ChatService(ChatRoomRepository rooms, MessageRepository messages, UserRepository users) {
        this.rooms = rooms;
        this.messages = messages;
        this.users = users;
    }

    public ChatRoom ensureRoom(Long roomId) {
        return rooms.findById(roomId).orElseGet(() -> rooms.save(new ChatRoom(roomId, "Room " + roomId)));
    }

    @Transactional
    public Message saveMessage(Long roomId, MessageDTO dto, Principal principal) {
        ChatRoom room = ensureRoom(roomId);
        String username = principal != null ? principal.getName() : Optional.ofNullable(dto.getSenderId()).orElse("anonymous");
        UserAccount sender = users.findByUsername(username).orElse(null);
        Message m = new Message();
        m.setRoom(room);
        m.setSenderUser(sender);
        m.setSenderUsername(username);
        m.setContent(dto.getContent() == null ? "" : dto.getContent());
        m.setCreatedAt(dto.getTs() > 0 ? Instant.ofEpochMilli(dto.getTs()) : Instant.now());
        return messages.save(m);
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> getHistory(Long roomId, Integer size, Long beforeTs) {
        int limit = (size == null || size <= 0 || size > 200) ? 50 : size;
        List<Message> list;
        if (beforeTs != null && beforeTs > 0) {
            list = messages.findByRoom_IdAndCreatedAtLessThanOrderByCreatedAtDesc(roomId, Instant.ofEpochMilli(beforeTs), PageRequest.of(0, limit));
        } else {
            list = messages.findByRoom_IdOrderByCreatedAtDesc(roomId, PageRequest.of(0, limit));
        }
        Collections.reverse(list); // ascending for UI
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    private MessageDTO toDto(Message m) {
        return new MessageDTO(
                m.getRoom().getId(),
                m.getSenderUsername(),
                m.getContent(),
                m.getCreatedAt().toEpochMilli()
        );
    }
}
