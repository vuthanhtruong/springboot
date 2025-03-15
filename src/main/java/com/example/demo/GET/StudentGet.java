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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/")
@Transactional
public class StudentGet {
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/DangKyHocSinh")
    public String DangKyHocSinh(ModelMap model) {
        List<Employees> employees = entityManager.createQuery("from Employees", Employees.class).getResultList();
        model.addAttribute("employees", employees);
        return "DangKyHocSinh";
    }

    @GetMapping("/TrangChuHocSinh")
    public String TrangChuHocSinh(ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String studentId = authentication.getName();
        Person person = entityManager.find(Person.class, studentId);
        Students student = (Students) person;
        // Lấy danh sách lớp học của học sinh
        List<ClassroomDetails> studentClasses = entityManager.createQuery(
                        "SELECT cd FROM ClassroomDetails cd WHERE cd.member = :studentId", ClassroomDetails.class)
                .setParameter("studentId", student)
                .getResultList();

// Tạo danh sách giáo viên
        Set<Teachers> teachers = new HashSet<>();
        for (ClassroomDetails cd : studentClasses) {
            Room room = cd.getRoom();
            List<ClassroomDetails> members = entityManager.createQuery(
                            "SELECT cd FROM ClassroomDetails cd WHERE cd.room.roomId = :roomId", ClassroomDetails.class)
                    .setParameter("roomId", room.getRoomId())
                    .getResultList();

            for (ClassroomDetails member : members) {
                if (member.getMember() instanceof Teachers) {
                    teachers.add((Teachers) member.getMember());
                }
            }
        }
// Lấy danh sách tài liệu từ cơ sở dữ liệu
        List<Documents> documents = entityManager.createQuery(
                        "SELECT d FROM Documents d " +
                                "WHERE d.creator != :student AND EXISTS ( " +
                                "   SELECT 1 FROM ClassroomDetails cd " +
                                "   WHERE cd.member = d.creator AND cd.room IN ( " +
                                "       SELECT cd2.room FROM ClassroomDetails cd2 WHERE cd2.member = :student " +
                                "   ) " +
                                ")", Documents.class)
                .setParameter("student", student)
                .getResultList();
        Collections.reverse(documents);
        List<Posts> posts = entityManager.createQuery(
                        "SELECT p FROM Posts p " +
                                "WHERE p.creator != :student AND EXISTS ( " +
                                "   SELECT 1 FROM ClassroomDetails cd " +
                                "   WHERE cd.member = p.creator AND cd.room IN ( " +
                                "       SELECT cd2.room FROM ClassroomDetails cd2 WHERE cd2.member = :student " +
                                "   ) " +
                                ")", Posts.class)
                .setParameter("student", student)
                .getResultList();

        List<Blogs> blogs = entityManager.createQuery("from Blogs  b where b.creator!=:creator", Blogs.class).
                setParameter("creator", student).getResultList();
        Collections.reverse(blogs);

        List<Messages> messagesList = entityManager.createQuery(
                        "SELECT m FROM Messages m " +
                                "WHERE m.sender != :student AND m.recipient = :student", Messages.class)
                .setParameter("student", student)
                .getResultList();
        Collections.reverse(messagesList);

        model.addAttribute("documents", documents);
        model.addAttribute("posts", posts);
        model.addAttribute("messages", messagesList);
        model.addAttribute("teachers", teachers);
        model.addAttribute("blogs", blogs);

        return "TrangChuHocSinh";
    }

    @GetMapping("/DangXuatHocSinh")
    public String DangXuatGiaoVien(HttpSession session) {
        return "redirect:/DangNhapHocSinh";
    }

    @GetMapping("/TinNhanCuaHocSinh")
    public String TinNhanCuaHocSinh(HttpSession session, ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String studentId = authentication.getName();
        Person person = entityManager.find(Person.class, studentId);
        Students student = (Students) person;

        // Truy vấn tin nhắn liên quan đến học sinh
        List<Messages> messages = entityManager.createQuery(
                        "FROM Messages m WHERE m.sender = :student OR m.recipient = :student", Messages.class)
                .setParameter("student", student)
                .getResultList();

        // Tập hợp các liên hệ mà học sinh đã trò chuyện
        Set<Person> contacts = new HashSet<>();
        for (Messages message : messages) {
            if (!message.getSender().equals(student)) {
                contacts.add(message.getSender());  // Người gửi khác học sinh
            }
            if (!message.getRecipient().equals(student)) {
                contacts.add(message.getRecipient());  // Người nhận khác học sinh
            }
        }
        model.addAttribute("contacts", contacts);  // Danh sách các liên hệ (người đã nhắn tin)
        return "TinNhanCuaHocSinh";  // Trả về view
    }

    @GetMapping("/ChiTietTinNhanCuaHocSinh/{id}")
    public String ChiTietTinNhanCuaHocSinh(HttpSession session, ModelMap model, @PathVariable("id") String id) {
        // Tìm giáo viên bằng ID dạng String
        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher == null) {
            return "redirect:/TinNhanCuaHocSinh?error=TeacherNotFound";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String studentId = authentication.getName();
        Person person = entityManager.find(Person.class, studentId);
        Students student = (Students) person;

        // Truy vấn tin nhắn giữa học sinh và giáo viên
        List<Messages> messages = entityManager.createQuery(
                        "FROM Messages m WHERE " +
                                "(m.sender = :student AND m.recipient = :teacher) " +
                                "OR (m.sender = :teacher AND m.recipient = :student) " +
                                "ORDER BY m.datetime ASC", Messages.class)
                .setParameter("student", student)
                .setParameter("teacher", teacher)
                .getResultList();

        model.addAttribute("student", student);
        model.addAttribute("teacher", teacher);
        model.addAttribute("messages", messages);

        return "ChiTietTinNhanCuaHocSinh";
    }
}
