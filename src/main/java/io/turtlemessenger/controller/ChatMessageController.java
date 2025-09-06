package io.turtlemessenger.controller;

import io.turtlemessenger.dto.MessageDTO;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;
import io.turtlemessenger.service.ChatService;

@Controller
public class ChatMessageController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final Logger logger = LoggerFactory.getLogger(ChatMessageController.class);

    public ChatMessageController(SimpMessagingTemplate messagingTemplate, ChatService chatService) {
        this.messagingTemplate = messagingTemplate;
        this.chatService = chatService;
    }

    @MessageMapping("rooms.{roomId}.send")
    public void send(@DestinationVariable Long roomId, MessageDTO message, Principal principal) {
        if (message.getTs() == 0) {
            message.setTs(System.currentTimeMillis());
        }
        message.setRoomId(roomId);
        if (principal != null) {
            message.setSenderId(principal.getName());
        }
        try {
            chatService.saveMessage(roomId, message, principal);
        } catch (Exception e) {
            logger.error("Failed to persist message for room {}: {}", roomId, e.getMessage());
        }
        messagingTemplate.convertAndSend("/topic/rooms." + roomId, message);
    }
}
