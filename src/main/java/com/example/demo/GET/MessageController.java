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
        try {
            Optional<Person> sender = personRepository.findById(chatMessage.getSenderId());
            Optional<Person> recipient = personRepository.findById(chatMessage.getRecipientId());

            if (sender.isPresent() && recipient.isPresent()) {
                Messages message = new Messages();
                message.setSender(sender.get());
                message.setRecipient(recipient.get());
                message.setDatetime(LocalDateTime.now());
                message.setText(chatMessage.getContent());

                // Tìm sự kiện với ID=1, nếu không có thì tạo mới
                Events event = entityManager.find(Events.class, 2);

                message.setEvent(event);
                entityManager.persist(message);
                entityManager.flush();

                System.out.println("✅ Tin nhắn đã lưu với ID: " + message.getMessagesID());

                messagingTemplate.convertAndSendToUser(
                        String.valueOf(chatMessage.getRecipientId()),
                        "/queue/messages",
                        chatMessage
                );
            } else {
                System.out.println("⚠ Người gửi hoặc người nhận không tồn tại");
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
        // Truy vấn tin nhắn của người dùng
        List<Messages> messages = entityManager.createQuery(
                        "FROM Messages m WHERE m.sender = :user OR m.recipient = :user", Messages.class)
                .setParameter("user", person)
                .getResultList();

        // Tập hợp các liên hệ mà người dùng đã nhắn tin
        Set<Person> contacts = new HashSet<>();
        for (Messages message : messages) {
            if (!message.getSender().equals(person)) {
                contacts.add(message.getSender());  // Người gửi khác user
            }
            if (!message.getRecipient().equals(person)) {
                contacts.add(message.getRecipient());  // Người nhận khác user
            }
        }
        if (person instanceof Students) {
            model.addAttribute("trangchu", "TrangChuHocSinh");
        } else {
            model.addAttribute("trangchu", "TrangChuGiaoVien");
        }

        model.addAttribute("contacts", contacts);  // Danh sách các liên hệ đã trò chuyện

        return "TinNhanCuaBan";  // Trả về view chung
    }

    @GetMapping("/ChiTietTinNhan/{id}")
    public String ChiTietTinNhan(HttpSession session, ModelMap model, @PathVariable("id") String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        Person currentUser = entityManager.find(Person.class, userId);
        Person chatPartner = entityManager.find(Person.class, id);

        if (currentUser == null || chatPartner == null) {
            return "redirect:/TinNhanCuaBan?error=UserNotFound";
        }

        // Truy vấn tin nhắn giữa currentUser và chatPartner
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
        if (person instanceof Students) {
            model.addAttribute("trangchu", "TrangChuHocSinh");
        } else {
            model.addAttribute("trangchu", "TrangChuGiaoVien");
        }

        return "ChiTietTinNhan";  // Trả về view chung
    }
}