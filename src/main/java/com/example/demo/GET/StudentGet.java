package com.example.demo.GET;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import jdk.jfr.Event;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

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
    public String TrangChuHocSinh(HttpSession session, ModelMap model) {
        Students student = entityManager.find(Students.class, session.getAttribute("StudentID"));
        model.addAttribute("student", student);
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
        // Lấy thông tin sinh viên từ session
        String studentId = (String) session.getAttribute("StudentID");
        if (student == null) {
            throw new EntityNotFoundException("Không tìm thấy học sinh với ID: " + studentId);
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
        Collections.reverse(posts);

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

        return "TrangChuHocSinh";
    }
    @GetMapping("/DangXuatHocSinh")
    public String DangXuatGiaoVien(HttpSession session) {
        session.invalidate();
        return "redirect:/DangNhapHocSinh";
    }
    @GetMapping("/DanhSachLopHocHocSinh")
    public String DanhSachLopHocHocSinh(HttpSession session, ModelMap model) {
        // Lấy StudentID từ session
        String studentId = (String) session.getAttribute("StudentID");
        if (studentId == null) {
            throw new IllegalArgumentException("Không tìm thấy StudentID trong session.");
        }

        // Tìm đối tượng Person theo ID
        Person student = entityManager.find(Person.class, studentId);
        if (student == null || !(student instanceof Students)) {
            throw new IllegalArgumentException("Không tìm thấy học sinh với ID: " + studentId);
        }

        // Lấy danh sách lớp mà học sinh này tham gia
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "from ClassroomDetails where member = :student",
                        ClassroomDetails.class)
                .setParameter("student", student)
                .getResultList();

        List<Rooms> rooms = new ArrayList<>();
        List<OnlineRooms> onlineRooms = new ArrayList<>();

        for (ClassroomDetails classroomDetail : classroomDetails) {
            Room room = classroomDetail.getRoom(); // Lấy đối tượng Room trực tiếp
            if (room instanceof OnlineRooms) {
                onlineRooms.add((OnlineRooms) room);
            } else if (room instanceof Rooms) {
                rooms.add((Rooms) room);
            }
        }

        model.addAttribute("classroomDetails", classroomDetails);
        model.addAttribute("rooms", rooms);
        model.addAttribute("onlineRooms", onlineRooms);

        return "DanhSachLopHocHocSinh";
    }

    @GetMapping("ChiTietLopHocHocSinh/{id}")
    public String ChiTietLopHocHocSinh(@PathVariable String id, ModelMap model) {
        Object room = entityManager.find(Rooms.class, id);
        if (room == null) {
            room = entityManager.find(OnlineRooms.class, id);
        }

        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lớp học không tồn tại!");
        }
        String roomId;
        if (room instanceof Rooms) {
            roomId = ((Rooms) room).getRoomId();
        } else if (room instanceof OnlineRooms) {
            roomId = ((OnlineRooms) room).getRoomId();
        } else {
            throw new IllegalArgumentException("Loại phòng không hợp lệ!");
        }

        model.addAttribute("room", room);
        // Lấy danh sách bài đăng trong lớp
        List<Posts> posts = entityManager.createQuery("SELECT p FROM Posts p WHERE p.room.roomId = :roomId", Posts.class)
                .setParameter("roomId", roomId)
                .getResultList();

        // Lấy danh sách bình luận cho từng bài đăng
        for (Posts post : posts) {
            List<Comments> comments = entityManager.createQuery("SELECT c FROM Comments c WHERE c.post.postId = :postId", Comments.class)
                    .setParameter("postId", post.getPostId())
                    .getResultList();
            post.setComments(comments);  // Đảm bảo Posts có phương thức setComments()
        }
        model.addAttribute("posts", posts);
        return "ChiTietLopHocHocSinh";
    }

    @GetMapping("/ThanhVienTrongLopHocSinh/{id}")
    public String ThanhVienTrongLopHocSinh(HttpSession session, @PathVariable String id, ModelMap model) {
        // Lấy đối tượng Room từ ID
        Room room = entityManager.find(Room.class, id);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy lớp học với ID: " + id);
        }

        // Truy vấn danh sách thành viên trong lớp
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "FROM ClassroomDetails WHERE room = :room", ClassroomDetails.class)
                .setParameter("room", room)
                .getResultList();

        List<Students> students = new ArrayList<>();
        List<Teachers>  teachers = new ArrayList<>();
        for (ClassroomDetails classroomDetail : classroomDetails) {
            Person member = classroomDetail.getMember(); // Lấy đối tượng Person thay vì String ID

            if (member instanceof Students) {
                students.add((Students) member);
            }
            else{
                teachers.add((Teachers) member);
            }
        }

        model.addAttribute("students", students);
        model.addAttribute("teachers", teachers);
        return "ThanhVienTrongLopHocSinh";
    }
    @GetMapping("/TinNhanCuaHocSinh")
    public String TinNhanCuaHocSinh(HttpSession session, ModelMap model) {
        Person student = entityManager.find(Person.class, session.getAttribute("StudentID"));

        if (student == null) {
            return "redirect:/DangNhapHocSinh";
        }

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

        // Kiểm tra xem học sinh đã đăng nhập chưa
        String studentID = (String) session.getAttribute("StudentID");
        if (studentID == null) {
            return "redirect:/DangNhapHocSinh";
        }

        // Tìm học sinh
        Students student = entityManager.find(Students.class, studentID);
        if (student == null) {
            return "redirect:/TinNhanCuaHocSinh?error=StudentNotFound";
        }

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
    @GetMapping("/TrangCaNhanHocSinh")
    public String TrangCaNhanHocSinh(HttpSession session, ModelMap model) {
        if(session.getAttribute("StudentID") == null) {
            return "redirect:/DangNhapHocSinh?error=StudentNotFound";
        }
        Students student = entityManager.find(Students.class, session.getAttribute("StudentID"));
        model.addAttribute("student", student);

        return "TrangCaNhanHocSinh";
    }
}
