package com.example.demo.POST;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
@Transactional

public class TrangCaNhanPost {

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/LuuThongTinCaNhan")
    public String luuThongTinCaNhan(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            HttpSession session,
            ModelMap model) {

        // Lấy ID từ Authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Lấy thông tin từ database
        Person person = entityManager.find(Person.class, userId);

        // Kiểm tra và cập nhật thông tin theo loại tài khoản cụ thể
        if (person instanceof Teachers teacher) {
            teacher.setFirstName(firstName);
            teacher.setLastName(lastName);
            teacher.setEmail(email);
            teacher.setPhoneNumber(phoneNumber);
            entityManager.merge(teacher);
            return "redirect:/TrangChuGiaoVien";
        } else if (person instanceof Students student) {
            student.setFirstName(firstName);
            student.setLastName(lastName);
            student.setEmail(email);
            student.setPhoneNumber(phoneNumber);
            entityManager.merge(student);
            return "redirect:/TrangChuHocSinh";
        } else if (person instanceof Admin admin) {
            admin.setFirstName(firstName);
            admin.setLastName(lastName);
            admin.setEmail(email);
            admin.setPhoneNumber(phoneNumber);
            entityManager.merge(admin);
            return "redirect:/TrangChuAdmin";
        } else if (person instanceof Employees employee) {
            employee.setFirstName(firstName);
            employee.setLastName(lastName);
            employee.setEmail(email);
            employee.setPhoneNumber(phoneNumber);
            entityManager.merge(employee);
            return "redirect:/TrangChuNhanVien";
        }

        return "redirect:/TrangChu"; // Mặc định về trang chủ nếu không xác định được loại user
    }

}
