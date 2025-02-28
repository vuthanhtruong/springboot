package com.example.demo.GET;

import com.example.demo.OOP.Messages;
import com.example.demo.OOP.Person;

import com.example.demo.Repository.MessagesRepository;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.websocket.dto.ChatMessage;
import jakarta.transaction.Transactional;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
public class MessageController {

    private final MessagesRepository messagesRepository;
    private final PersonRepository personRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(MessagesRepository messagesRepository, PersonRepository personRepository, SimpMessagingTemplate messagingTemplate) {
        this.messagesRepository = messagesRepository;
        this.personRepository = personRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat")
    @Transactional
    public void sendMessage(ChatMessage chatMessage) {
        try {
            Optional<Person> sender = personRepository.findById(chatMessage.getSenderId());
            Optional<Person> recipient = personRepository.findById(chatMessage.getRecipientId());

            if (sender.isPresent() && recipient.isPresent()) {
                // Tạo và lưu tin nhắn vào DB
                Messages message = new Messages();
                message.setSender(sender.get());
                message.setRecipient(recipient.get());
                message.setDatetime(LocalDateTime.now());
                message.setText(chatMessage.getContent());

                Messages savedMessage = messagesRepository.save(message);
                System.out.println("✅ Tin nhắn đã lưu với ID: " + savedMessage.getMessagesID());

                // 🔹 Chuẩn bị dữ liệu để gửi
                ChatMessage responseMessage = new ChatMessage();
                responseMessage.setSenderId(chatMessage.getSenderId());
                responseMessage.setRecipientId(chatMessage.getRecipientId());
                responseMessage.setContent(chatMessage.getContent());
                responseMessage.setTimestamp(LocalDateTime.now().toString()); // Thêm thời gian gửi

                // ✅ Gửi tin nhắn đến người nhận (recipient)
                String recipientUsername = "user-" + chatMessage.getRecipientId(); // Đảm bảo có username hợp lệ
                messagingTemplate.convertAndSendToUser(
                        recipientUsername, "/queue/messages", responseMessage
                );

                // ✅ Gửi tin nhắn đến người gửi (sender) để cập nhật giao diện
                String senderUsername = "user-" + chatMessage.getSenderId();
                messagingTemplate.convertAndSendToUser(
                        senderUsername, "/queue/messages", responseMessage
                );

            } else {
                System.out.println("❌ Người gửi hoặc người nhận không tồn tại");
            }
        } catch (Exception e) {
            System.out.println("❌ Lỗi khi gửi tin nhắn: " + e.getMessage());
            e.printStackTrace();  // Hiển thị lỗi đầy đủ để debug
        }
    }



}
