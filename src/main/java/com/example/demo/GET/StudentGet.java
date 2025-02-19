package com.example.demo.GET;

import com.example.demo.OOP.Employees;
import com.example.demo.OOP.Students;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

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
        return "TrangChuHocSinh";
    }
    @GetMapping("/DangXuatHocSinh")
    public String DangXuatGiaoVien(HttpSession session) {
        session.invalidate();
        return "redirect:/DangNhapHocSinh";
    }



}
