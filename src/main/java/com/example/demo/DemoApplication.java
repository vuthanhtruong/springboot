package com.example.demo;

import com.example.demo.OOP.Admin;
import com.example.demo.OOP.Events;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootApplication(scanBasePackages = "com.example.demo")
public class DemoApplication {
    public static void main(String[] args) {
        // Khởi động ứng dụng và lấy ApplicationContext
        ApplicationContext context = SpringApplication.run(DemoApplication.class, args);

        // Lấy EntityManagerFactory và PasswordEncoder từ context
        EntityManagerFactory entityManagerFactory = context.getBean(EntityManagerFactory.class);
        PasswordEncoder passwordEncoder = context.getBean(PasswordEncoder.class);

        // Tạo EntityManager duy nhất
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            // Bắt đầu transaction
            entityManager.getTransaction().begin();

            // Mã hóa mật khẩu Admin nếu cần
            processAdminPassword(entityManager, passwordEncoder);

            // Commit transaction
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            e.printStackTrace();
        }

        try {
            // Bắt đầu transaction để thêm sự kiện
            entityManager.getTransaction().begin();

            // Thêm danh sách sự kiện nếu chưa tồn tại
            addDefaultEvents(entityManager);

            // Commit transaction
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            // Đóng EntityManager sau khi tất cả các tác vụ hoàn thành
            entityManager.close();
        }
    }

    // Mã hóa mật khẩu admin nếu chưa mã hóa
    private static void processAdminPassword(EntityManager entityManager, PasswordEncoder passwordEncoder) {
        List<Admin> admins = entityManager.createQuery("SELECT a FROM Admin a", Admin.class)
                .setMaxResults(1)
                .getResultList();

        if (!admins.isEmpty()) {
            Admin firstAdmin = admins.get(0);
            String currentPassword = firstAdmin.getPassword();

            if (!isPasswordEncoded(currentPassword)) {
                String encodedPassword = passwordEncoder.encode(currentPassword);
                firstAdmin.setPassword(encodedPassword);
                entityManager.merge(firstAdmin);
                System.out.println("Mật khẩu của admin đầu tiên đã được mã hóa.");
            } else {
                System.out.println("Mật khẩu của admin đầu tiên đã được mã hóa sẵn.");
            }
        } else {
            System.out.println("Không tìm thấy admin nào trong cơ sở dữ liệu.");
        }
    }

    // Thêm các sự kiện mặc định vào database nếu chưa có
    private static void addDefaultEvents(EntityManager entityManager) {
        List<Events> eventsToAdd = List.of(
                new Events("Default Event", "Default event description", LocalDateTime.of(2025, 3, 3, 0, 0, 0), "DEFAULT"),
                new Events("New Message", "You have a new message from another user.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "MESSAGE"),
                new Events("New Feedback", "You have new feedback from a student.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "FEEDBACK"),
                new Events("New Post", "A new post has been made in your classroom.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "POST"),
                new Events("New Document", "A new document has been shared with you.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "DOCUMENT"),
                new Events("New Comment", "Someone has commented on your post.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "COMMENT"),
                new Events("New Blog Post", "A new blog post has been published.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "BLOG"),
                new Events("Schedule Notification", "You have a new notification related to your schedule.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "SCHEDULE_NOTIFICATION"),
                new Events("Added to Classroom", "You have been added to a new classroom.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "CLASSROOM_JOIN"),
                new Events("System Event", "Default system event.", LocalDateTime.of(2025, 3, 3, 0, 0, 0), "SYSTEM_EVENT")
        );

        for (Events event : eventsToAdd) {
            boolean exists = checkEventExists(entityManager, event.getTitle(), event.getDescription(), event.getEventDate(), event.getEventType());
            if (!exists) {
                entityManager.persist(event);
                System.out.println("Đã thêm sự kiện: " + event.getTitle());
            } else {
                System.out.println("Sự kiện đã tồn tại: " + event.getTitle());
            }
        }
    }

    // Kiểm tra xem mật khẩu đã được mã hóa hay chưa
    private static boolean isPasswordEncoded(String password) {
        return password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }

    // Kiểm tra xem sự kiện đã tồn tại trong cơ sở dữ liệu chưa
    private static boolean checkEventExists(EntityManager entityManager, String title, String description, LocalDateTime eventDate, String eventType) {
        try {
            entityManager.createQuery(
                            "SELECT e FROM Events e WHERE e.title = :title AND e.description = :description " +
                                    "AND e.eventDate = :eventDate AND e.eventType = :eventType", Events.class)
                    .setParameter("title", title)
                    .setParameter("description", description)
                    .setParameter("eventDate", eventDate)
                    .setParameter("eventType", eventType)
                    .getSingleResult();
            return true; // Sự kiện đã tồn tại
        } catch (NoResultException e) {
            return false; // Sự kiện chưa tồn tại
        }
    }
}
