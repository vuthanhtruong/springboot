package com.example.demo.websocket.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

public class ChatMessage {
    private String senderId;
    private String recipientId;
    private String content;
    private String timestamp; // 🔹 Thêm thuộc tính thời gian gửi

    // Getters và Setters
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

