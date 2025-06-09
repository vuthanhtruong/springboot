package com.example.demo;

import com.example.demo.ModelOOP.Admin;
import com.example.demo.ModelOOP.Notifications;
import com.example.demo.ModelOOP.Slots;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalTime;
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

            // Thêm Admin mặc định nếu chưa tồn tại
            addDefaultAdmin(entityManager, passwordEncoder);

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
        }

        try {
            // Bắt đầu transaction để thêm slots
            entityManager.getTransaction().begin();

            // Thêm danh sách slots nếu chưa tồn tại
            addDefaultSlots(entityManager);

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

    // Thêm Admin mặc định nếu chưa tồn tại
    private static void addDefaultAdmin(EntityManager entityManager, PasswordEncoder passwordEncoder) {
        try {
            Admin existingAdmin = entityManager.createQuery("SELECT a FROM Admin a WHERE a.id = :id", Admin.class)
                    .setParameter("id", "admin")
                    .getSingleResult();
            System.out.println("Admin mặc định đã tồn tại.");
        } catch (NoResultException e) {
            String encodedPassword = "Admin123";
            Admin defaultAdmin = new Admin("admin", encodedPassword, "Default", "Admin", "admin@example.com", "0394444107");
            defaultAdmin.setBirthDate(LocalDateTime.of(1990, 1, 1, 0, 0).toLocalDate());
            entityManager.persist(defaultAdmin);
            System.out.println("Đã thêm Admin mặc định.");
        }
    }

    // Thêm các sự kiện mặc định vào database nếu chưa có
    private static void addDefaultEvents(EntityManager entityManager) {
        List<Notifications> eventsToAdd = List.of(
                new Notifications("Default Event", "Default event description", LocalDateTime.of(2025, 3, 3, 0, 0, 0), "DEFAULT"),
                new Notifications("New Message", "You have a new message from another user.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "MESSAGE"),
                new Notifications("New Feedback", "You have new feedback from a student.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "FEEDBACK"),
                new Notifications("New Post", "A new post has been made in your classroom.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "POST"),
                new Notifications("New Document", "A new document has been shared with you.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "DOCUMENT"),
                new Notifications("New Comment", "Someone has commented on your post.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "COMMENT"),
                new Notifications("New Blog Post", "A new blog post has been published.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "BLOG"),
                new Notifications("Schedule Notification", "You have a new notification related to your schedule.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "SCHEDULE_NOTIFICATION"),
                new Notifications("Added to Classroom", "You have been added to a new classroom.", LocalDateTime.of(2025, 3, 3, 20, 57, 47), "CLASSROOM_JOIN"),
                new Notifications("System Event", "Default system event.", LocalDateTime.of(2025, 3, 3, 0, 0, 0), "SYSTEM_EVENT")
        );

        for (Notifications event : eventsToAdd) {
            boolean exists = checkEventExists(entityManager, event.getTitle(), event.getDescription(), event.getEventDate(), event.getEventType());
            if (!exists) {
                entityManager.persist(event);
                System.out.println("Đã thêm sự kiện: " + event.getTitle());
            } else {
                System.out.println("Sự kiện đã tồn tại: " + event.getTitle());
            }
        }
    }

    // Kiểm tra xem sự kiện đã tồn tại trong cơ sở dữ liệu chưa
    private static boolean checkEventExists(EntityManager entityManager, String title, String description, LocalDateTime eventDate, String eventType) {
        try {
            entityManager.createQuery(
                            "SELECT e FROM Notifications e WHERE e.title = :title AND e.description = :description " +
                                    "AND e.eventDate = :eventDate AND e.eventType = :eventType", Notifications.class)
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

    // Thêm các slot mặc định vào database nếu chưa có
    private static void addDefaultSlots(EntityManager entityManager) {
        List<Slots> slotsToAdd = List.of(
                new Slots("Slot 1", LocalTime.of(7, 0), LocalTime.of(8, 40)),
                new Slots("Slot 2", LocalTime.of(8, 50), LocalTime.of(10, 20)),
                new Slots("Slot 3", LocalTime.of(10, 30), LocalTime.of(12, 0)),
                new Slots("Slot 4", LocalTime.of(12, 50), LocalTime.of(14, 20)),
                new Slots("Slot 5", LocalTime.of(14, 30), LocalTime.of(16, 0)),
                new Slots("Slot 6", LocalTime.of(16, 10), LocalTime.of(17, 40))
        );

        for (Slots slot : slotsToAdd) {
            boolean exists = checkSlotExists(entityManager, slot.getSlotName(), slot.getStartTime(), slot.getEndTime());
            if (!exists) {
                entityManager.persist(slot);
                System.out.println("Đã thêm slot: " + slot.getSlotName());
            } else {
                System.out.println("Slot đã tồn tại: " + slot.getSlotName());
            }
        }
    }

    // Kiểm tra xem slot đã tồn tại trong cơ sở dữ liệu chưa
    private static boolean checkSlotExists(EntityManager entityManager, String slotName, LocalTime startTime, LocalTime endTime) {
        try {
            entityManager.createQuery(
                            "SELECT s FROM Slots s WHERE s.slotName = :slotName AND s.startTime = :startTime AND s.endTime = :endTime", Slots.class)
                    .setParameter("slotName", slotName)
                    .setParameter("startTime", startTime)
                    .setParameter("endTime", endTime)
                    .getSingleResult();
            return true; // Slot đã tồn tại
        } catch (NoResultException e) {
            return false; // Slot chưa tồn tại
        }
    }
}