package com.example.demo.websocket.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessage {
    private String senderId;
    private String recipientId;
    private String content;
    private LocalDateTime timestamp;
}
