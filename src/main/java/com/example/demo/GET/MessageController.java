package com.example.demo.GET;

import com.example.demo.OOP.Events;
import com.example.demo.OOP.Messages;
import com.example.demo.OOP.Person;
import com.example.demo.OOP.Students;
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
import org.springframework.web.bind.annotation.PathVariable;

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
            Optional<Person> sender = personRepository.findById(chatMessage.getSenderId());
            Optional<Person> recipient = personRepository.findById(chatMessage.getRecipientId());

            if (sender.isPresent() && recipient.isPresent()) {
                Messages message = new Messages();
                message.setSender(sender.get());
                message.setRecipient(recipient.get());
                message.setDatetime(LocalDateTime.now());
                message.setText(chatMessage.getContent());

                Events event = entityManager.find(Events.class, 2);
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

                // Gửi tới người nhận
                messagingTemplate.convertAndSendToUser(
                        chatMessage.getRecipientId(),
                        "/queue/messages",
                        response
                );
                System.out.println("📤 Đã gửi tin nhắn tới /user/" + chatMessage.getRecipientId() + "/queue/messages");

                // Gửi lại cho người gửi
                messagingTemplate.convertAndSendToUser(
                        chatMessage.getSenderId(),
                        "/queue/messages",
                        response
                );
                System.out.println("📤 Đã gửi tin nhắn tới /user/" + chatMessage.getSenderId() + "/queue/messages");
            } else {
                System.out.println("⚠️ Người gửi hoặc người nhận không tồn tại");
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi khi gửi tin nhắn: " + e.getMessage());
        }
    }

    @GetMapping("/TinNhanCuaBan")
    public String TinNhanCuaBan(HttpSession session, ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);

        if (person == null) {
            return "redirect:/login";
        }

        List<Messages> messages = entityManager.createQuery(
                        "FROM Messages m WHERE m.sender = :user OR m.recipient = :user", Messages.class)
                .setParameter("user", person)
                .getResultList();

        Set<Person> contacts = new HashSet<>();
        for (Messages message : messages) {
            if (!message.getSender().equals(person)) {
                contacts.add(message.getSender());
            }
            if (!message.getRecipient().equals(person)) {
                contacts.add(message.getRecipient());
            }
        }

        model.addAttribute("trangchu", person instanceof Students ? "TrangChuHocSinh" : "TrangChuGiaoVien");
        model.addAttribute("contacts", contacts);

        return "TinNhanCuaBan";
    }

    @GetMapping("/ChiTietTinNhan/{id}")
    public String ChiTietTinNhan(HttpSession session, ModelMap model, @PathVariable("id") String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Person currentUser = entityManager.find(Person.class, userId);
        Person chatPartner = entityManager.find(Person.class, id);

        if (currentUser == null || chatPartner == null) {
            return "redirect:/TinNhanCuaBan?error=UserNotFound";
        }

        List<Messages> messages = entityManager.createQuery(
                        "FROM Messages m WHERE " +
                                "(m.sender = :currentUser AND m.recipient = :chatPartner) " +
                                "OR (m.sender = :chatPartner AND m.recipient = :currentUser) " +
                                "ORDER BY m.datetime ASC", Messages.class)
                .setParameter("currentUser", currentUser)
                .setParameter("chatPartner", chatPartner)
                .getResultList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatPartner", chatPartner);
        model.addAttribute("messages", messages);
        model.addAttribute("trangchu", currentUser instanceof Students ? "TrangChuHocSinh" : "TrangChuGiaoVien");

        return "ChiTietTinNhan";
    }
}