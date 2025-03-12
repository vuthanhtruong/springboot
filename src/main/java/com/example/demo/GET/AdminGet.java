package com.example.demo.GET;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class AdminGet {
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/TrangChuAdmin")
    public String trangChuAdmin(ModelMap model) {
        // Lấy thông tin AdminID từ Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName(); // AdminID đã đăng nhập

        // Tìm Admin trong database bằng EntityManager
        Admin admin = entityManager.find(Admin.class, adminId);
        if (admin == null) {
            throw new IllegalStateException("Admin không tồn tại");
        }
        List<Blogs> blogs = entityManager.createQuery("from Blogs  b where b.creator!=:creator", Blogs.class).
                setParameter("creator", admin).getResultList();
        Collections.reverse(blogs);
        model.addAttribute("blogs", blogs);

        model.addAttribute("admin", admin);
        return "TrangChuAdmin";
    }

    @GetMapping("/TrangCaNhanAdmin")
    public String trangCaNhanAdmin(ModelMap model) {
        // Lấy thông tin AdminID từ Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName(); // Lấy AdminID từ SecurityContext

        // Tìm Admin trong database bằng EntityManager
        Admin admin = entityManager.find(Admin.class, adminId);
        if (admin == null) {
            throw new IllegalStateException("Admin không tồn tại");
        }
        model.addAttribute("admin", admin);
        return "TrangCaNhanAdmin";
    }

    @GetMapping("/DanhSachGiaoVien")
    public String danhSachGiaoVien(
            @RequestParam(value = "order", required = false) String sortOrder,
            Model model) {

        String jpql = "SELECT t FROM Teachers t"; // Mặc định không sắp xếp

        if ("asc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY t.birthDate ASC"; // Sắp xếp tăng dần
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY t.birthDate DESC"; // Sắp xếp giảm dần
        }

        TypedQuery<Teachers> query = entityManager.createQuery(jpql, Teachers.class);
        List<Teachers> teachers = query.getResultList();

        model.addAttribute("teachers", teachers);
        model.addAttribute("order", sortOrder);

        return "DanhSachGiaoVien";
    }


    @GetMapping("/ThemGiaoVien")
    public String ThemGiaoVien(ModelMap model, HttpSession session) {

        List<Employees> employees = entityManager.createQuery("from Employees", Employees.class).getResultList();
        model.addAttribute("employees", employees);
        return "ThemGiaoVien";
    }

    @GetMapping("/DanhSachHocSinh")
    public String danhSachHocSinh(
            @RequestParam(value = "order", required = false) String sortOrder,
            Model model) {

        String jpql = "SELECT s FROM Students s"; // Mặc định: không sắp xếp

        if ("asc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY s.birthDate ASC"; // Sắp xếp tăng dần
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY s.birthDate DESC"; // Sắp xếp giảm dần
        }

        TypedQuery<Students> query = entityManager.createQuery(jpql, Students.class);
        List<Students> students = query.getResultList();

        model.addAttribute("students", students);
        model.addAttribute("order", sortOrder);

        return "DanhSachHocSinh";
    }

    @GetMapping("/ThemHocSinh")
    public String ThemHocSinh(ModelMap model, HttpSession session) {

        List<Employees> employees = entityManager.createQuery("from Employees", Employees.class).getResultList();
        model.addAttribute("employees", employees);
        return "ThemHocSinh";
    }

    @GetMapping("/DanhSachNhanVien")
    public String danhSachNhanVien(
            @RequestParam(value = "order", required = false) String sortOrder,
            Model model) {

        String jpql = "SELECT e FROM Employees e"; // Mặc định không sắp xếp

        if ("asc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY e.firstName ASC"; // Sắp xếp A → Z
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY e.firstName DESC"; // Sắp xếp Z → A
        }

        TypedQuery<Employees> query = entityManager.createQuery(jpql, Employees.class);
        List<Employees> employees = query.getResultList();

        model.addAttribute("employees", employees);
        model.addAttribute("order", sortOrder);

        return "DanhSachNhanVien";
    }

    @GetMapping("/ThemNhanVien")
    public String ThemNhanVien(HttpSession session, ModelMap model) {

        return "ThemNhanVien";
    }

    @GetMapping("/XoaGiaoVien/{id}")
    public String XoaGiaoVien(@PathVariable("id") String id, HttpSession session, ModelMap model) {

        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher != null) {
            entityManager.remove(teacher);
        }
        return "redirect:/DanhSachGiaoVien";
    }

    @GetMapping("/XoaHocSinh/{id}")
    public String XoaHocSinh(@PathVariable("id") String id, HttpSession session, ModelMap model) {

        Students student = entityManager.find(Students.class, id);
        entityManager.remove(student);
        return "redirect:/DanhSachHocSinh";
    }

    @Transactional
    @GetMapping("/XoaNhanVien/{id}")
    public String XoaNhanVien(@PathVariable("id") String id, HttpSession session, ModelMap model) {

        Employees employee = entityManager.find(Employees.class, id);
        if (employee != null) {
            // Cập nhật tất cả Students có EmployeeID = id thành null
            entityManager.createQuery("UPDATE Students s SET s.employee = NULL WHERE s.employee.id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            entityManager.createQuery("UPDATE Teachers s SET s.employee = NULL WHERE s.employee.id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
            // Sau đó xóa nhân viên
            entityManager.remove(employee);
        }
        return "redirect:/DanhSachNhanVien";
    }

    @GetMapping("/SuaHocSinh/{id}")
    public String SuaHocSinh(ModelMap model, @PathVariable("id") String id, HttpSession session) {
        Students student = entityManager.find(Students.class, id);
        List<Employees> employees = entityManager.createQuery("from Employees", Employees.class).getResultList();
        model.addAttribute("student", student);
        model.addAttribute("employees", employees);
        return "SuaHocSinh";
    }

    @GetMapping("/SuaGiaoVien/{id}")
    public String SuaGiaoVien(ModelMap model, @PathVariable("id") String id, HttpSession session) {

        List<Employees> employees = entityManager.createQuery("from Employees", Employees.class).getResultList();
        Teachers teachers = entityManager.find(Teachers.class, id);
        model.addAttribute("teachers", teachers);
        model.addAttribute("employees", employees);
        return "SuaGiaoVien";
    }

    @GetMapping("/SuaNhanVien/{id}")
    public String SuaAdmin(ModelMap model, @PathVariable("id") String id, HttpSession session) {

        Employees employee = entityManager.find(Employees.class, id);
        model.addAttribute("employees", employee);
        return "SuaNhanVien";
    }

    @GetMapping("/XoaTatCaHocSinh")
    public String xoaTatCaHocSinh() {
        entityManager.createQuery("DELETE FROM Students").executeUpdate();
        return "redirect:/DanhSachHocSinh";
    }

    @GetMapping("/XoaTatCaGiaoVien")
    public String xoaTatCaGiaoVien() {
        entityManager.createQuery("DELETE FROM Teachers").executeUpdate();
        return "redirect:/DanhSachGiaoVien";
    }

    @GetMapping("/XoaTatCaNhanVien")
    public String xoaTatCaNhanVien() {
        entityManager.createQuery("DELETE FROM Employees").executeUpdate();
        return "redirect:/DanhSachNhanVien";
    }

}