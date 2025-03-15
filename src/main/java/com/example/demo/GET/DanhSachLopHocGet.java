package com.example.demo.GET;


import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class DanhSachLopHocGet {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/DanhSachLopHoc")
    public String DanhSachLopHoc(HttpSession session, ModelMap model) {
        // Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);

        // Kiểm tra nếu người dùng không tồn tại
        if (person == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ hoặc không tồn tại.");
        }

        // Lấy danh sách lớp mà người dùng này tham gia
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "from ClassroomDetails where member = :user",
                        ClassroomDetails.class)
                .setParameter("user", person)
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
        if (person instanceof Students) {
            model.addAttribute("homePage", "/TrangChuHocSinh");
        } else {
            model.addAttribute("homePage", "/TrangChuGiaoVien");
        }

        model.addAttribute("classroomDetails", classroomDetails);
        model.addAttribute("rooms", rooms);
        model.addAttribute("onlineRooms", onlineRooms);

        return "/DanhSachLopHoc";
    }

    @GetMapping("ChiTietLopHocBanThamGia/{id}")
    public String ChiTietLopHocBanThamGia(@PathVariable String id, Model model, Authentication authentication) {
        String username = authentication.getName();

        Person member = entityManager.find(Person.class, username);
        if (member instanceof Students) {
            model.addAttribute("homePage", "/TrangChuHocSinh");
        } else {
            model.addAttribute("homePage", "/TrangChuGiaoVien");
        }

        Room room = entityManager.find(Room.class, id);
        Boolean roomMode = true;

        if (room instanceof Rooms) {
            roomMode = false;
        }
        List<Posts> posts = entityManager.createQuery(
                        "SELECT p FROM Posts p WHERE p.room.roomId = :roomId", Posts.class)
                .setParameter("roomId", room.getRoomId())
                .getResultList();

        model.addAttribute("posts", posts);
        model.addAttribute("room", room);
        model.addAttribute("roomMode", roomMode);

        return "ChiTietLopHocBanThamGia";
    }


    @GetMapping("/ThanhVienTrongLopHocCuaBan/{id}")
    public String danhSachThanhVienTrongLop(
            @PathVariable String id,
            ModelMap model,
            Authentication authentication) {  // Thay HttpSession bằng Authentication

        Room room = entityManager.find(Room.class, id);
        if (room == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy lớp học với ID: " + id);
        }

        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "FROM ClassroomDetails WHERE room = :room", ClassroomDetails.class)
                .setParameter("room", room)
                .getResultList();

        List<Students> students = new ArrayList<>();
        List<Teachers> teachers = new ArrayList<>();

        for (ClassroomDetails classroomDetail : classroomDetails) {
            Person member = classroomDetail.getMember();
            if (member instanceof Students) {
                students.add((Students) member);
            } else if (member instanceof Teachers) {
                teachers.add((Teachers) member);
            }
        }

        model.addAttribute("students", students);
        model.addAttribute("teachers", teachers);

        // Lấy user hiện tại từ Spring Security
        String username = authentication.getName();

        Person member = entityManager.find(Person.class, username);
        if (member instanceof Students) {
            model.addAttribute("homePage", "/TrangChuHocSinh");
        } else {
            model.addAttribute("homePage", "/TrangChuGiaoVien");
        }
        model.addAttribute("roomId", room.getRoomId());
        return "ThanhVienTrongLopHocCuaBan";
    }

}
