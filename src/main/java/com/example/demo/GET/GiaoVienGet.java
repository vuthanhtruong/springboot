package com.example.demo.GET;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/")
@Transactional
public class GiaoVienGet {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/DangKyGiaoVien")
    public String DangKyGiaoVien(ModelMap model) {
        // Sử dụng EntityManager để thực thi truy vấn
        List<Employees> employees = entityManager.createQuery("from Employees", Employees.class).getResultList();
        model.addAttribute("employees", employees);
        return "DangKyGiaoVien";
    }

    @GetMapping("/TrangChuGiaoVien")
    public String TrangChuGiaoVien(
            @RequestParam(defaultValue = "1") int pageDocs,
            @RequestParam(defaultValue = "1") int pagePosts,
            @RequestParam(defaultValue = "1") int pageMessages,
            @RequestParam(defaultValue = "1") int pageBlogs,
            @RequestParam(defaultValue = "5") int pageSize,
            ModelMap model,
            HttpSession session
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String teacherId = authentication.getName();
        Person person = entityManager.find(Person.class, teacherId);
        Teachers teacher = (Teachers) person;

        // Lưu pageSize vào session
        session.setAttribute("pageSize", pageSize);

        // Lấy danh sách phòng học của giáo viên
        List<Room> teacherRooms = entityManager.createQuery(
                        "SELECT cd.room FROM ClassroomDetails cd WHERE cd.member = :teacher", Room.class)
                .setParameter("teacher", teacher)
                .getResultList();

        // 1. Phân trang cho Documents
        Long totalDocs = (Long) entityManager.createQuery(
                        "SELECT COUNT(d) FROM Documents d " +
                                "WHERE d.creator != :teacher " + // Loại bỏ tài liệu do giáo viên tạo
                                "AND d.post.room IN :teacherRooms") // Chỉ lấy từ các phòng của giáo viên
                .setParameter("teacher", teacher)
                .setParameter("teacherRooms", teacherRooms)
                .getSingleResult();
        int totalPagesDocs = Math.max(1, (int) Math.ceil((double) totalDocs / pageSize));
        pageDocs = Math.max(1, Math.min(pageDocs, totalPagesDocs));
        int firstResultDocs = (pageDocs - 1) * pageSize;
        List<Documents> documents = entityManager.createQuery(
                        "SELECT d FROM Documents d " +
                                "WHERE d.creator != :teacher " +
                                "AND d.post.room IN :teacherRooms " +
                                "ORDER BY d.event.eventDate DESC", Documents.class) // Sắp xếp theo ngày sự kiện
                .setParameter("teacher", teacher)
                .setParameter("teacherRooms", teacherRooms)
                .setFirstResult(firstResultDocs)
                .setMaxResults(pageSize)
                .getResultList();

        // 2. Phân trang cho Posts
        Long totalPosts = (Long) entityManager.createQuery(
                        "SELECT COUNT(p) FROM Posts p " +
                                "WHERE p.creator != :teacher " + // Loại bỏ bài đăng do giáo viên tạo
                                "AND p.room IN :teacherRooms") // Chỉ lấy từ các phòng của giáo viên
                .setParameter("teacher", teacher)
                .setParameter("teacherRooms", teacherRooms)
                .getSingleResult();
        int totalPagesPosts = Math.max(1, (int) Math.ceil((double) totalPosts / pageSize));
        pagePosts = Math.max(1, Math.min(pagePosts, totalPagesPosts));
        int firstResultPosts = (pagePosts - 1) * pageSize;
        List<Posts> posts = entityManager.createQuery(
                        "SELECT p FROM Posts p " +
                                "WHERE p.creator != :teacher " +
                                "AND p.room IN :teacherRooms " +
                                "ORDER BY p.event.eventDate DESC", Posts.class)
                .setParameter("teacher", teacher)
                .setParameter("teacherRooms", teacherRooms)
                .setFirstResult(firstResultPosts)
                .setMaxResults(pageSize)
                .getResultList();

        // 3. Phân trang cho Messages
        Long totalMessages = (Long) entityManager.createQuery(
                        "SELECT COUNT(m) FROM Messages m " +
                                "WHERE m.sender != :teacher AND m.recipient = :teacher " + // Tin nhắn gửi đến giáo viên
                                "AND m.event IN (SELECT cd.event FROM ClassroomDetails cd WHERE cd.member = :teacher)") // Chỉ từ sự kiện trong lớp
                .setParameter("teacher", teacher)
                .getSingleResult();
        int totalPagesMessages = Math.max(1, (int) Math.ceil((double) totalMessages / pageSize));
        pageMessages = Math.max(1, Math.min(pageMessages, totalPagesMessages));
        int firstResultMessages = (pageMessages - 1) * pageSize;
        List<Messages> messagesList = entityManager.createQuery(
                        "SELECT m FROM Messages m " +
                                "WHERE m.sender != :teacher AND m.recipient = :teacher " +
                                "AND m.event IN (SELECT cd.event FROM ClassroomDetails cd WHERE cd.member = :teacher) " +
                                "ORDER BY m.event.eventDate DESC", Messages.class)
                .setParameter("teacher", teacher)
                .setFirstResult(firstResultMessages)
                .setMaxResults(pageSize)
                .getResultList();

        // 4. Phân trang cho Blogs
        Long totalBlogs = (Long) entityManager.createQuery(
                        "SELECT COUNT(b) FROM Blogs b " +
                                "WHERE b.creator != :teacher " + // Loại bỏ blog do giáo viên tạo
                                "AND b.event IN (SELECT cd.event FROM ClassroomDetails cd WHERE cd.member = :teacher)") // Chỉ từ sự kiện trong lớp
                .setParameter("teacher", teacher)
                .getSingleResult();
        int totalPagesBlogs = Math.max(1, (int) Math.ceil((double) totalBlogs / pageSize));
        pageBlogs = Math.max(1, Math.min(pageBlogs, totalPagesBlogs));
        int firstResultBlogs = (pageBlogs - 1) * pageSize;
        List<Blogs> blogs = entityManager.createQuery(
                        "SELECT b FROM Blogs b " +
                                "WHERE b.creator != :teacher " +
                                "AND b.event IN (SELECT cd.event FROM ClassroomDetails cd WHERE cd.member = :teacher) " +
                                "ORDER BY b.event.eventDate DESC", Blogs.class)
                .setParameter("teacher", teacher)
                .setFirstResult(firstResultBlogs)
                .setMaxResults(pageSize)
                .getResultList();

        // Truyền dữ liệu lên giao diện
        model.addAttribute("teacher", teacher);
        model.addAttribute("documents", documents);
        model.addAttribute("posts", posts);
        model.addAttribute("messagesList", messagesList);
        model.addAttribute("blogs", blogs);

        // Thông tin phân trang
        model.addAttribute("currentPageDocs", pageDocs);
        model.addAttribute("totalPagesDocs", totalPagesDocs);
        model.addAttribute("currentPagePosts", pagePosts);
        model.addAttribute("totalPagesPosts", totalPagesPosts);
        model.addAttribute("currentPageMessages", pageMessages);
        model.addAttribute("totalPagesMessages", totalPagesMessages);
        model.addAttribute("currentPageBlogs", pageBlogs);
        model.addAttribute("totalPagesBlogs", totalPagesBlogs);
        model.addAttribute("pageSize", pageSize);

        return "TrangChuGiaoVien";
    }

    @GetMapping("/TinNhanCuaGiaoVien")
    public String TinNhanCuaGiaoVien(HttpSession session, ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String teacherId = authentication.getName();
        Person person = entityManager.find(Person.class, teacherId);
        Teachers teacher = (Teachers) person;

        // Truy vấn tin nhắn liên quan đến giáo viên
        List<Messages> messages = entityManager.createQuery(
                        "FROM Messages m WHERE m.sender = :teacher OR m.recipient = :teacher", Messages.class)
                .setParameter("teacher", teacher)
                .getResultList();

        // Tập hợp các liên hệ mà giáo viên đã trò chuyện
        Set<Person> contacts = new HashSet<>();
        for (Messages message : messages) {
            if (!message.getSender().equals(teacher)) {
                contacts.add(message.getSender());  // Người gửi khác giáo viên
            }
            if (!message.getRecipient().equals(teacher)) {
                contacts.add(message.getRecipient());  // Người nhận khác giáo viên
            }
        }
        model.addAttribute("contacts", contacts);  // Danh sách các liên hệ (người đã nhắn tin)
        return "TinNhanCuaGiaoVien";  // Trả về view
    }

    @GetMapping("/ChiTietTinNhanCuaGiaoVien/{id}")
    public String ChiTietTinNhanCuaGiaoVien(HttpSession session, ModelMap model, @PathVariable("id") String id) {
        // Tìm sinh viên bằng ID dạng String
        Students student = entityManager.find(Students.class, id);
        if (student == null) {
            return "redirect:/TinNhanCuaGiaoVien?error=StudentNotFound";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String teacherId = authentication.getName();
        Person person = entityManager.find(Person.class, teacherId);
        Teachers teacher = (Teachers) person;

        if (teacher == null) {
            return "redirect:/TinNhanCuaGiaoVien?error=TeacherNotFound";
        }

        // Truy vấn tin nhắn giữa giáo viên và học sinh
        List<Messages> messages = entityManager.createQuery(
                        "FROM Messages m WHERE " +
                                "(m.sender = :teacher AND m.recipient = :student) " +
                                "OR (m.sender = :student AND m.recipient = :teacher) " +
                                "ORDER BY m.datetime ASC", Messages.class)
                .setParameter("teacher", teacher)
                .setParameter("student", student)
                .getResultList();

        model.addAttribute("student", student);
        model.addAttribute("teacher", teacher);
        model.addAttribute("messages", messages);

        return "ChiTietTinNhanCuaGiaoVien";
    }
}
