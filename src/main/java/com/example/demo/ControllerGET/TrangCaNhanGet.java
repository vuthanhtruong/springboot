package com.example.demo.ControllerGET;

import com.example.demo.ModelOOP.Employees;
import com.example.demo.ModelOOP.Person;
import com.example.demo.ModelOOP.Students;
import com.example.demo.ModelOOP.Teachers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@Transactional
public class TrangCaNhanGet {
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/TrangCaNhan")
    public String TrangCaNhan(HttpSession session, ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Lấy thông tin người dùng từ EntityManager
        Person user = entityManager.find(Person.class, userId);

        if (user == null) {
            throw new IllegalStateException("Người dùng không tồn tại");
        }
        model.addAttribute("user", user);
        return "TrangCaNhan";
    }

    @GetMapping("/QuayVeTrangChu")
    public String QuayVeTrangChu(HttpSession session, ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Lấy thông tin người dùng từ EntityManager
        Person user = entityManager.find(Person.class, userId);

        if (user instanceof Students) {
            return "redirect:/TrangChuHocSinh";
        } else if (user instanceof Teachers) {
            return "redirect:/TrangChuGiaoVien";
        } else if (user instanceof Employees) {
            return "redirect:/TrangChuNhanVien";
        } else {
            return "redirect:/TrangChuAdmin";
        }
    }

}
