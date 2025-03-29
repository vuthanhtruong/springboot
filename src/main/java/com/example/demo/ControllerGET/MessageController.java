package com.example.demo.ControllerGET;

import com.example.demo.ModelOOP.Events;
import com.example.demo.ModelOOP.Messages;
import com.example.demo.ModelOOP.Person;
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

    @GetMapping("/ChiTietTinNhan")
    public String ChiTietTinNhan(HttpSession session, ModelMap model,
                                 @RequestParam(value = "id", required = false) String chatPartnerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Person currentUser = entityManager.find(Person.class, userId);

        if (currentUser == null) {
            return "redirect:/TinNhanCuaBan?error=UserNotFound";
        }

        // Lấy danh sách tất cả tin nhắn của currentUser để tạo danh sách liên hệ
        List<Messages> allMessages = entityManager.createQuery(
                        "FROM Messages m WHERE m.sender = :user OR m.recipient = :user", Messages.class)
                .setParameter("user", currentUser)
                .getResultList();

        // Tạo danh sách liên hệ (contacts)
        Set<Person> contacts = new HashSet<>();
        for (Messages message : allMessages) {
            if (!message.getSender().equals(currentUser)) {
                contacts.add(message.getSender());
            }
            if (!message.getRecipient().equals(currentUser)) {
                contacts.add(message.getRecipient());
            }
        }

        // Xử lý tin nhắn với chatPartner nếu có chatPartnerId
        List<Messages> messages = null;
        Person chatPartner = null;
        if (chatPartnerId != null && !chatPartnerId.isEmpty()) {
            chatPartner = entityManager.find(Person.class, chatPartnerId);
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

        // Thêm các thuộc tính vào model để hiển thị trên giao diện
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("chatPartner", chatPartner); // Có thể null nếu không chọn
        model.addAttribute("messages", messages); // Có thể null nếu không chọn
        model.addAttribute("contacts", contacts); // Danh sách liên hệ luôn có
        model.addAttribute("trangchu", currentUser instanceof Students ? "TrangChuHocSinh" : "TrangChuGiaoVien");

        return "ChiTietTinNhan";
    }

    @MessageMapping("/deleteMessage")
    @Transactional
    public void deleteMessage(ChatMessage chatMessage) {
        try {
            Optional<Person> sender = personRepository.findById(chatMessage.getSenderId());
            Optional<Messages> messageOpt = entityManager.createQuery(
                            "FROM Messages m WHERE m.messagesID = :messageId AND m.sender = :sender", Messages.class)
                    .setParameter("messageId", Integer.parseInt(chatMessage.getMessageId()))
                    .setParameter("sender", sender.get())
                    .getResultList().stream().findFirst();

            if (messageOpt.isPresent()) {
                Messages message = messageOpt.get();
                message.setText("Người dùng này đã xóa tin nhắn"); // Cập nhật nội dung tin nhắn
                entityManager.merge(message);
                entityManager.flush();

                System.out.println("✅ Tin nhắn đã được cập nhật thành 'Người dùng này đã xóa tin nhắn' với ID: " + message.getMessagesID());

                ChatMessage response = new ChatMessage(
                        chatMessage.getSenderId(),
                        message.getRecipient().getId(), // Gửi lại cho recipient
                        message.getText(),
                        message.getDatetime().toString()
                );
                response.setAction("delete"); // Thêm action để frontend nhận diện
                response.setMessageId(String.valueOf(message.getMessagesID())); // Truyền messageId

                // Gửi thông báo tới người nhận
                messagingTemplate.convertAndSendToUser(
                        message.getRecipient().getId(),
                        "/queue/messages",
                        response
                );
                System.out.println("📤 Đã gửi thông báo xóa tới /user/" + message.getRecipient().getId() + "/queue/messages");

                // Gửi thông báo lại cho người gửi
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