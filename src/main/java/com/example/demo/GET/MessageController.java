package com.example.demo.GET;

import com.example.demo.OOP.Messages;
import com.example.demo.OOP.Person;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.websocket.dto.ChatMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class MessageController {

    private final PersonRepository personRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager; // 🆕 Inject EntityManager

    public MessageController(PersonRepository personRepository, SimpMessagingTemplate messagingTemplate) {
        this.personRepository = personRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat")
    @Transactional  // ✅ Transaction đảm bảo tính nhất quán dữ liệu
    public void sendMessage(ChatMessage chatMessage) {
        try {
            Optional<Person> sender = personRepository.findById(chatMessage.getSenderId());
            Optional<Person> recipient = personRepository.findById(chatMessage.getRecipientId());

            if (sender.isPresent() && recipient.isPresent()) {
                Messages message = new Messages();
                message.setSender(sender.get());
                message.setRecipient(recipient.get());
                message.setDatetime(LocalDateTime.now());
                message.setText(chatMessage.getContent());

                entityManager.persist(message);  // ✅ Lưu tin nhắn bằng EntityManager
                entityManager.flush();  // ✅ Đẩy dữ liệu ngay xuống database

                System.out.println("✅ Tin nhắn đã lưu với ID: " + message.getMessagesID());

                // 🔹 Gửi tin nhắn tới người nhận qua WebSocket
                messagingTemplate.convertAndSendToUser(
                        chatMessage.getRecipientId(),
                        "/queue/messages",
                        chatMessage
                );
            } else {
                System.out.println("⚠ Người gửi hoặc người nhận không tồn tại");
            }
        } catch (Exception e) {
            System.out.println("❌ Lỗi khi gửi tin nhắn: " + e.getMessage());
        }
    }
}
