package com.example.demo.GET;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
    public String DangNhapGiaoVien(ModelMap model, HttpSession session) {
        if(session.getAttribute("TeacherID") == null) {
            return "DangNhapGiaoVien";
        }
        Teachers teacher = entityManager.find(Teachers.class, session.getAttribute("TeacherID"));
        model.addAttribute("teacher", teacher);
        return "TrangChuGiaoVien";
    }
    @GetMapping("/DangXuatGiaoVien")
    public String DangXuatGiaoVien(HttpSession session) {
        session.invalidate();
        return "redirect:/DangNhapGiaoVien";
    }
    @GetMapping("/DanhSachLopHocGiaoVien")
    public String DanhSachLopHocGiaoVien(HttpSession session, ModelMap model) {
        // Lấy TeacherID từ session
        String teacherId = (String) session.getAttribute("TeacherID");
        if (teacherId == null) {
            throw new IllegalArgumentException("Không tìm thấy TeacherID trong session.");
        }

        // Tìm đối tượng Person theo ID
        Person teacher = entityManager.find(Person.class, teacherId);
        if (teacher == null || !(teacher instanceof Teachers)) {
            throw new IllegalArgumentException("Không tìm thấy giáo viên với ID: " + teacherId);
        }

        // Lấy danh sách lớp mà giáo viên này tham gia
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "from ClassroomDetails where member = :teacher",
                        ClassroomDetails.class)
                .setParameter("teacher", teacher)
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

        return "DanhSachLopHocGiaoVien";
    }

    @GetMapping("ChiTietLopHocGiaoVien/{id}")
    public String ChiTietLopHocGiaoVien(@PathVariable String id, ModelMap model) {
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
            model.addAttribute("room", room);
        } else if (room instanceof OnlineRooms) {
            roomId = ((OnlineRooms) room).getRoomId();
            model.addAttribute("room", room);
        } else {
            throw new IllegalArgumentException("Loại phòng không hợp lệ!");
        }

        // Lấy bài đăng
        List<Posts> posts = entityManager.createQuery("SELECT p FROM Posts p WHERE p.room.roomId = :roomId", Posts.class)
                .setParameter("roomId", roomId)
                .getResultList();
        model.addAttribute("posts", posts);

        return "ChiTietLopHocGiaoVien";
    }


    @GetMapping("/ThanhVienTrongLopGiaoVien/{id}")
    public String ThanhVienTrongLopHoc(HttpSession session, @PathVariable String id, ModelMap model) {
        // Lấy đối tượng Room từ ID
        Room room = entityManager.find(Room.class, id);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng với ID: " + id);
        }

        // Truy vấn danh sách thành viên trong lớp
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "FROM ClassroomDetails WHERE room = :room", ClassroomDetails.class)
                .setParameter("room", room)
                .getResultList();

        List<Students> students = new ArrayList<>();
        List<Teachers> teachers = new ArrayList<>();
        for (ClassroomDetails classroomDetail : classroomDetails) {
            Person member = classroomDetail.getMember();  // Lấy đối tượng Person thay vì String ID

            if (member instanceof Students) {
                students.add((Students) member);
            } else if (member instanceof Teachers) {
                teachers.add((Teachers) member);
            }
        }

        // Log danh sách sinh viên để kiểm tra
        System.out.println("Students List: " + students);

        model.addAttribute("students", students);
        model.addAttribute("teachers", teachers);
        return "ThanhVienTrongLopGiaoVien";
    }


    @GetMapping("/TinNhanCuaGiaoVien")
    public String TinNhanCuaGiaoVien(HttpSession session, ModelMap model) {
        Person teacher = entityManager.find(Person.class, session.getAttribute("TeacherID"));

        if (teacher == null) {
            return "redirect:/DangNhapGiaoVien";
        }

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

        // Kiểm tra xem giáo viên đã đăng nhập chưa
        String teacherID = (String) session.getAttribute("TeacherID");
        if (teacherID == null) {
            return "redirect:/DangNhapGiaoVien";
        }

        // Tìm giáo viên
        Teachers teacher = entityManager.find(Teachers.class, teacherID);
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
