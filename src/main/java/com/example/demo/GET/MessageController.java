package com.example.demo.GET;

import com.example.demo.OOP.*;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.websocket.dto.ChatMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.List;
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
                Events event = entityManager.find(Events.class, 1);
                message.setEvent(event);

                entityManager.persist(message);  // ✅ Lưu tin nhắn bằng EntityManager
                entityManager.flush();  // ✅ Đẩy dữ liệu ngay xuống database
                entityManager.clear();  // 🆕 Đảm bảo dữ liệu không bị cache

                System.out.println("✅ Tin nhắn đã lưu với ID: " + message.getMessagesID());

                // 🔹 Gửi tin nhắn tới người nhận qua WebSocket
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(chatMessage.getRecipientId()), // 🔄 Fix lỗi convert kiểu dữ liệu
                        "/queue/messages",
                        chatMessage
                );
            } else {
                System.out.println("⚠ Người gửi hoặc người nhận không tồn tại");
            }
        } catch (Exception e) {
            e.printStackTrace(); // 🆕 Hiển thị lỗi rõ hơn trong console
            System.out.println("❌ Lỗi khi gửi tin nhắn: " + e.getMessage());
        }
    }

}