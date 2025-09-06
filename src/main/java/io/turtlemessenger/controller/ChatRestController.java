package io.turtlemessenger.controller;

import io.turtlemessenger.dto.MessageDTO;
import io.turtlemessenger.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatRestController {

    private final ChatService chatService;
    public ChatRestController(ChatService chatService) { this.chatService = chatService; }

    @GetMapping("/rooms/{roomId}/messages")
    public List<MessageDTO> getHistory(
            @PathVariable Long roomId,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "before", required = false) Long before
    ) {
        return chatService.getHistory(roomId, size, before);
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Void> append(@PathVariable Long roomId, @RequestBody MessageDTO message) {
        chatService.saveMessage(roomId, message, null);
        return ResponseEntity.accepted().build();
    }
}
