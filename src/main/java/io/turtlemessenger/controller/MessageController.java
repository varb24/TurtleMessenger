package io.turtlemessenger.controller;

import io.turtlemessenger.dto.MessageDTO;
import io.turtlemessenger.model.Message;
import io.turtlemessenger.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final ChatService chatService;
    private final Logger logger = LoggerFactory.getLogger(MessageController.class);

    public MessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public String saveMessage(@RequestBody String text, @RequestParam(name = "roomId", defaultValue = "1") Long roomId) {
        MessageDTO dto = new MessageDTO(roomId, "api", text, System.currentTimeMillis());
        Message saved = chatService.saveMessage(roomId, dto, null);
        logger.info("Saved message: id={}, room={}, sender={}, len={}",
                saved.getId(), saved.getRoom().getId(), saved.getSenderUsername(), saved.getContent().length());
        return "Message Saved";
    }
}
