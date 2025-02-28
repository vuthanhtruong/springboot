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
    @Transactional  // 🔹 Đảm bảo giao dịch không bị rollback
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

                Messages savedMessage = messagesRepository.save(message);  // 🔹 Kiểm tra xem có lưu vào DB không
                System.out.println("Tin nhắn đã lưu với ID: " + savedMessage.getMessagesID());

                // Gửi tin nhắn tới người nhận qua WebSocket
                messagingTemplate.convertAndSendToUser(
                        chatMessage.getRecipientId(),
                        "/queue/messages",
                        chatMessage
                );
            } else {
                System.out.println("Người gửi hoặc người nhận không tồn tại");
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi gửi tin nhắn: " + e.getMessage());
        }
    }
}
