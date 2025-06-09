package com.example.demo.ControllerGET;

import com.example.demo.ModelOOP.Notifications;
import com.example.demo.ModelOOP.Messages;
import com.example.demo.ModelOOP.Persons;
import com.example.demo.ModelOOP.Students;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.websocket.dto.ChatMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Controller
public class MessageController {

    private final PersonRepository personRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public MessageController(PersonRepository personRepository, SimpMessagingTemplate messagingTemplate) {
        this.personRepository = personRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat")
    @Transactional
    public void sendMessage(ChatMessage chatMessage) {
        System.out.println("📥 Nhận tin nhắn từ client: " + chatMessage.getSenderId() + " -> " + chatMessage.getRecipientId() + ": " + chatMessage.getContent());
        try {
            if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                System.out.println("⚠️ Nội dung tin nhắn không hợp lệ");
                return;
            }

            Optional<Persons> sender = personRepository.findById(chatMessage.getSenderId());
            Optional<Persons> recipient = personRepository.findById(chatMessage.getRecipientId());

            if (sender.isPresent() && recipient.isPresent()) {
                Messages message = new Messages();
                message.setSender(sender.get());
                message.setRecipient(recipient.get());
                message.setDatetime(LocalDateTime.now());
                message.setText(chatMessage.getContent());

                Notifications event = entityManager.find(Notifications.class, 2);
                message.setEvent(event);

                entityManager.persist(message);
                entityManager.flush();

                System.out.println("✅ Tin nhắn đã lưu với ID: " + message.getMessagesID());

                ChatMessage response = new ChatMessage(
                        chatMessage.getSenderId(),
                        chatMessage.getRecipientId(),
                        chatMessage.getContent(),
                        message.getDatetime().toString()
                );
                response.setMessageId(String.valueOf(message.getMessagesID()));

                // Chỉ gửi tới người nhận
                messagingTemplate.convertAndSendToUser(
                        chatMessage.getRecipientId(),
                        "/queue/messages",
                        response
                );
                System.out.println("📤 Đã gửi tin nhắn tới /user/" + chatMessage.getRecipientId() + "/queue/messages");
            } else {
                System.out.println("⚠️ Người gửi hoặc người nhận không tồn tại");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi khi gửi tin nhắn: " + e.getMessage());
        }
    }

    @GetMapping("/ChiTietTinNhan")
    public String ChiTietTinNhan(HttpSession session, ModelMap model,
                                 @RequestParam(value = "id", required = false) String chatPartnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Persons currentUser = entityManager.find(Persons.class, userId);

        if (currentUser == null) {
            return "redirect:/TinNhanCuaBan?error=UserNotFound";
        }

        List<Messages> allMessages = entityManager.createQuery(
                        "FROM Messages m WHERE m.sender = :user OR m.recipient = :user", Messages.class)
                .setParameter("user", currentUser)
                .getResultList();

        Set<Persons> contacts = new HashSet<>();
        for (Messages message : allMessages) {
            if (!message.getSender().equals(currentUser)) {
                contacts.add(message.getSender());
            }
            if (!message.getRecipient().equals(currentUser)) {
                contacts.add(message.getRecipient());
            }
        }

        List<Messages> messages = null;
        Persons chatPartner = null;
        if (chatPartnerId != null && !chatPartnerId.isEmpty()) {
            chatPartner = entityManager.find(Persons.class, chatPartnerId);
            if (chatPartner != null) {
                messages = entityManager.createQuery(
                                "FROM Messages m WHERE " +
                                        "(m.sender = :currentUser AND m.recipient = :chatPartner) " +
                                        "OR (m.sender = :chatPartner AND m.recipient = :currentUser) " +
                                        "ORDER BY m.datetime ASC", Messages.class)
                        .setParameter("currentUser", currentUser)
                        .setParameter("chatPartner", chatPartner)
                        .getResultList();
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatPartner", chatPartner);
        model.addAttribute("messages", messages);
        model.addAttribute("contacts", contacts);
        model.addAttribute("trangchu", currentUser instanceof Students ? "TrangChuHocSinh" : "TrangChuGiaoVien");

        return "ChiTietTinNhan";
    }

    @MessageMapping("/deleteMessage")
    @Transactional
    public void deleteMessage(ChatMessage chatMessage) {
        try {
            Optional<Persons> sender = personRepository.findById(chatMessage.getSenderId());
            Optional<Messages> messageOpt = entityManager.createQuery(
                            "FROM Messages m WHERE m.messagesID = :messageId AND m.sender = :sender", Messages.class)
                    .setParameter("messageId", Integer.parseInt(chatMessage.getMessageId()))
                    .setParameter("sender", sender.get())
                    .getResultList().stream().findFirst();

            if (messageOpt.isPresent()) {
                Messages message = messageOpt.get();
                message.setText("Người dùng này đã xóa tin nhắn");
                entityManager.merge(message);
                entityManager.flush();

                System.out.println("✅ Tin nhắn đã được cập nhật với ID: " + message.getMessagesID());

                ChatMessage response = new ChatMessage(
                        chatMessage.getSenderId(),
                        message.getRecipient().getId(),
                        message.getText(),
                        message.getDatetime().toString()
                );
                response.setAction("delete");
                response.setMessageId(String.valueOf(message.getMessagesID()));

                messagingTemplate.convertAndSendToUser(
                        message.getRecipient().getId(),
                        "/queue/messages",
                        response
                );
                System.out.println("📤 Đã gửi thông báo xóa tới /user/" + message.getRecipient().getId() + "/queue/messages");

                messagingTemplate.convertAndSendToUser(
                        chatMessage.getSenderId(),
                        "/queue/messages",
                        response
                );
                System.out.println("📤 Đã gửi thông báo xóa tới /user/" + chatMessage.getSenderId() + "/queue/messages");
            } else {
                System.out.println("⚠️ Không tìm thấy tin nhắn hoặc không có quyền xóa");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi khi xóa tin nhắn: " + e.getMessage());
        }
    }
}