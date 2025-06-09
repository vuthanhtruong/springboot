package com.example.demo.ControllerGET;

import com.example.demo.ModelOOP.*;
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

import java.util.ArrayList;
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize,
            Model model,
            HttpSession session
    ) {
        // Xử lý pageSize
        if (pageSize == null) {
            pageSize = (Integer) session.getAttribute("pageSize");
            if (pageSize == null) {
                pageSize = 5; // Mặc định 5 nếu chưa có
            }
        }
        session.setAttribute("pageSize", pageSize);

        // Đếm tổng số giáo viên
        Long totalTeachers = (Long) entityManager.createQuery("SELECT COUNT(t) FROM Teachers t")
                .getSingleResult();

        // Tính tổng số trang
        int totalPages = Math.max(1, (int) Math.ceil((double) totalTeachers / pageSize));
        page = Math.max(1, Math.min(page, totalPages));

        // Tính vị trí bắt đầu
        int firstResult = (page - 1) * pageSize;

        // Xử lý sắp xếp
        String jpql = "SELECT t FROM Teachers t";
        if ("asc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY t.birthDate ASC";
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY t.birthDate DESC";
        }

        // Lấy danh sách giáo viên với phân trang
        TypedQuery<Teachers> query = entityManager.createQuery(jpql, Teachers.class)
                .setFirstResult(firstResult)
                .setMaxResults(pageSize);
        List<Teachers> teachers = query.getResultList();

        // Truyền dữ liệu lên giao diện
        model.addAttribute("teachers", teachers);
        model.addAttribute("order", sortOrder);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

        return "DanhSachGiaoVien";
    }


    @GetMapping("/ThemGiaoVien")
    public String ThemGiaoVien(ModelMap model, HttpSession session) {

        List<Staffs> employees = entityManager.createQuery("from Staffs", Staffs.class).getResultList();
        model.addAttribute("employees", employees);
        return "ThemGiaoVien";
    }

    @GetMapping("/DanhSachHocSinh")
    public String danhSachHocSinh(
            @RequestParam(value = "order", required = false) String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize,
            Model model,
            HttpSession session
    ) {
        // Xử lý pageSize
        if (pageSize == null) {
            pageSize = (Integer) session.getAttribute("pageSize");
            if (pageSize == null) {
                pageSize = 5; // Mặc định 5 nếu chưa có
            }
        }
        session.setAttribute("pageSize", pageSize);

        // Đếm tổng số học sinh
        Long totalStudents = (Long) entityManager.createQuery("SELECT COUNT(s) FROM Students s")
                .getSingleResult();

        // Tính tổng số trang
        int totalPages = Math.max(1, (int) Math.ceil((double) totalStudents / pageSize));
        page = Math.max(1, Math.min(page, totalPages));

        // Tính vị trí bắt đầu
        int firstResult = (page - 1) * pageSize;

        // Xử lý sắp xếp
        String jpql = "SELECT s FROM Students s";
        if ("asc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY s.birthDate ASC";
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY s.birthDate DESC";
        }

        // Lấy danh sách học sinh với phân trang
        TypedQuery<Students> query = entityManager.createQuery(jpql, Students.class)
                .setFirstResult(firstResult)
                .setMaxResults(pageSize);
        List<Students> students = query.getResultList();

        // Truyền dữ liệu lên giao diện
        model.addAttribute("students", students);
        model.addAttribute("order", sortOrder);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

        return "DanhSachHocSinh";
    }

    @GetMapping("/ThemHocSinh")
    public String ThemHocSinh(ModelMap model, HttpSession session) {

        List<Staffs> employees = entityManager.createQuery("from Staffs", Staffs.class).getResultList();
        model.addAttribute("employees", employees);
        return "ThemHocSinh";
    }

    @GetMapping("/DanhSachNhanVien")
    public String danhSachNhanVien(
            @RequestParam(value = "order", required = false) String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize,
            Model model,
            HttpSession session
    ) {
        // Xử lý pageSize
        if (pageSize == null) {
            pageSize = (Integer) session.getAttribute("pageSize");
            if (pageSize == null) {
                pageSize = 5; // Mặc định 5 nếu chưa có
            }
        }
        session.setAttribute("pageSize", pageSize);

        // Đếm tổng số nhân viên
        Long totalEmployees = (Long) entityManager.createQuery("SELECT COUNT(e) FROM Staffs e")
                .getSingleResult();

        // Xử lý trường hợp không có nhân viên
        if (totalEmployees == 0) {
            model.addAttribute("employees", new ArrayList<>());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 1);
            model.addAttribute("pageSize", pageSize);
            model.addAttribute("order", sortOrder);
            return "DanhSachNhanVien";
        }

        // Tính tổng số trang
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEmployees / pageSize));
        page = Math.max(1, Math.min(page, totalPages));

        // Tính vị trí bắt đầu
        int firstResult = (page - 1) * pageSize;

        // Xử lý sắp xếp
        String jpql = "SELECT e FROM Staffs e";
        if ("asc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY e.firstName ASC";
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            jpql += " ORDER BY e.firstName DESC";
        }

        // Lấy danh sách nhân viên với phân trang
        TypedQuery<Staffs> query = entityManager.createQuery(jpql, Staffs.class)
                .setFirstResult(firstResult)
                .setMaxResults(pageSize);
        List<Staffs> employees = query.getResultList();

        // Truyền dữ liệu lên giao diện
        model.addAttribute("employees", employees);
        model.addAttribute("order", sortOrder);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

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
    public String XoaNhanVien(@PathVariable("id") String id) {
        Staffs employee = entityManager.find(Staffs.class, id);
        if (employee != null) {
            List<Room> rooms = entityManager.createQuery("SELECT r FROM Room r WHERE r.employee.id = :id", Room.class)
                    .setParameter("id", id)
                    .getResultList();

            // Xóa các bản ghi trong bảng `rooms` liên quan đến Room
            for (Room room : rooms) {
                entityManager.remove(room);
            }
            // Xóa Employees (sẽ tự động xóa Room nhờ CASCADE)
            entityManager.remove(employee);
        }
        return "redirect:/DanhSachNhanVien";
    }

    @GetMapping("/SuaHocSinh/{id}")
    public String SuaHocSinh(ModelMap model, @PathVariable("id") String id, HttpSession session) {
        Students student = entityManager.find(Students.class, id);
        List<Staffs> employees = entityManager.createQuery("from Staffs", Staffs.class).getResultList();
        model.addAttribute("student", student);
        model.addAttribute("employees", employees);
        return "SuaHocSinh";
    }

    @GetMapping("/SuaGiaoVien/{id}")
    public String SuaGiaoVien(ModelMap model, @PathVariable("id") String id, HttpSession session) {

        List<Staffs> employees = entityManager.createQuery("from Staffs", Staffs.class).getResultList();
        Teachers teachers = entityManager.find(Teachers.class, id);
        model.addAttribute("teachers", teachers);
        model.addAttribute("employees", employees);
        return "SuaGiaoVien";
    }

    @GetMapping("/SuaNhanVien/{id}")
    public String SuaAdmin(ModelMap model, @PathVariable("id") String id, HttpSession session) {

        Staffs employee = entityManager.find(Staffs.class, id);
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
        List<Staffs> employees = entityManager.createQuery("from Staffs", Staffs.class).getResultList();
        for (Staffs employee : employees) {
            XoaNhanVien(employee.getId());
        }
        return "redirect:/DanhSachNhanVien";
    }

}