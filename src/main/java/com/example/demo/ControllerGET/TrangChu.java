package com.example.demo.ControllerGET;

import com.example.demo.ModelOOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
@Transactional
public class TrangChu {
    private final PasswordEncoder passwordEncoder;
    @PersistenceContext
    private EntityManager entityManager;

    public TrangChu(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String ChayVaoTrangChu(HttpSession session) {
        return "redirect:/TrangChu";
    }

    @GetMapping("/TrangChu")
    public String TrangChu(HttpSession session) {
        return "TrangChu";
    }

    @GetMapping("/DoiMatKhau")
    public String DoiMatKhau(HttpSession session) {

        return "DoiMatKhau";
    }

    @PostMapping("/XuLyDoiMatKhau")
    public String XuLyDoiMatKhau(HttpSession session,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            return "redirect:/DoiMatKhau?error=notfound";
        }

        String storedPassword = null;
        if (person instanceof Students student) {
            storedPassword = student.getPassword();
        } else if (person instanceof Teachers teacher) {
            storedPassword = teacher.getPassword();
        } else if (person instanceof Employees employee) {
            storedPassword = employee.getPassword();
        } else if (person instanceof Admin admin) {
            storedPassword = admin.getPassword();
        } else {
            return "redirect:/DoiMatKhau?error=invaliduser";
        }

        if (!passwordEncoder.matches(currentPassword, storedPassword)) {
            return "redirect:/DoiMatKhau?error=wrongpassword";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/DoiMatKhau?error=mismatch";
        }

        if (person instanceof Students student) {
            student.setPassword(newPassword);
            entityManager.merge(student);
            System.out.println("Redirecting to: /TrangChuHocSinh");
            return "redirect:/TrangChuHocSinh";
        } else if (person instanceof Teachers teacher) {
            teacher.setPassword(newPassword);
            entityManager.merge(teacher);
            System.out.println("Redirecting to: /TrangChuGiaoVien");
            return "redirect:/TrangChuGiaoVien";
        } else if (person instanceof Employees employee) {
            employee.setPassword(newPassword);
            entityManager.merge(employee);
            System.out.println("Redirecting to: /TrangChuNhanVien");
            return "redirect:/TrangChuNhanVien";
        } else if (person instanceof Admin admin) {
            admin.setPassword(newPassword);
            entityManager.merge(admin);
            System.out.println("Redirecting to: /TrangChuAdmin");
            return "redirect:/TrangChuAdmin";
        }

        System.out.println("Redirecting to default: /TrangChu");
        return "redirect:/TrangChu";
    }


}
