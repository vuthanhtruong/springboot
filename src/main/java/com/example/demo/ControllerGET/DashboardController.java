package com.example.demo.ControllerGET;


import com.example.demo.ModelOOP.Admin;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/")
public class DashboardController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/Dashboard")
    public String dashboard(Model model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName();

        Admin admin = entityManager.find(Admin.class, adminId);
        if (admin == null) {
            throw new IllegalStateException("Admin không tồn tại");
        }

        // Tổng số học viên
        Long totalStudents = (Long) entityManager.createQuery("SELECT COUNT(s) FROM Students s").getSingleResult();

        // Tổng số giáo viên
        Long totalTeachers = (Long) entityManager.createQuery("SELECT COUNT(t) FROM Teachers t").getSingleResult();

        // Tổng số nhân viên
        Long totalEmployees = (Long) entityManager.createQuery("SELECT COUNT(e) FROM Staffs e").getSingleResult();

        Long totalBlog = (Long) entityManager.createQuery("SELECT COUNT(b) FROM Blogs b").getSingleResult();

        Long OnRoom = (Long) entityManager.createQuery("SELECT COUNT(o) FROM OnlineRooms o").getSingleResult();

        Long OffRoom = (Long) entityManager.createQuery("SELECT COUNT(o) FROM Rooms o").getSingleResult();

        Long totalMessages = (Long) entityManager.createQuery("SELECT COUNT(m) FROM Messages m").getSingleResult();

        // Truyền dữ liệu tổng số vào model
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalTeachers", totalTeachers);
        model.addAttribute("totalEmployees", totalEmployees);
        model.addAttribute("OnRoom", OnRoom);
        model.addAttribute("OffRoom", OffRoom);
        model.addAttribute("totalBlog", totalBlog);
        model.addAttribute("totalMessages", totalMessages);

        return "Dashboard"; // Trả về view Dashboard
    }

    @GetMapping("/api/Dashboard/stats")
    @ResponseBody
    public Map<String, Object> getDashboardStats() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String adminId = authentication.getName(); // AdminID đã đăng nhập

        Admin admin = entityManager.find(Admin.class, adminId);
        if (admin == null) {
            throw new IllegalStateException("Admin không tồn tại");
        }
        // Tổng số học viên
        Long totalStudents = (Long) entityManager.createQuery("SELECT COUNT(s) FROM Students s").getSingleResult();

        // Tổng số giáo viên
        Long totalTeachers = (Long) entityManager.createQuery("SELECT COUNT(t) FROM Teachers t").getSingleResult();

        // Tổng số nhân viên
        Long totalEmployees = (Long) entityManager.createQuery("SELECT COUNT(e) FROM Staffs e").getSingleResult();

        Long totalBlog = (Long) entityManager.createQuery("SELECT COUNT(b) FROM Blogs b").getSingleResult();

        Long OnRoom = (Long) entityManager.createQuery("SELECT COUNT(o) FROM OnlineRooms o").getSingleResult();

        Long OffRoom = (Long) entityManager.createQuery("SELECT COUNT(o) FROM Rooms o").getSingleResult();

        Long totalMessages = (Long) entityManager.createQuery("SELECT COUNT(m) FROM Messages m").getSingleResult();

        LocalDate now = LocalDate.now();
        LocalDate last24Hours = now.minusDays(1);

        Long visitor = (Long) entityManager
                .createQuery("SELECT COUNT(v) FROM Dashboard v WHERE v.visitor >=:last24Hours")
                .setParameter("last24Hours", last24Hours)
                .getSingleResult();
        // Tổng số học viên mới (trong 24 giờ)
        Long newStudents = (Long) entityManager.createQuery(
                        "SELECT COUNT(s) FROM Students s WHERE s.createdDate >= :last24Hours")
                .setParameter("last24Hours", last24Hours)
                .getSingleResult();

        // Tổng số giáo viên mới (trong 24 giờ)
        Long newTeachers = (Long) entityManager.createQuery(
                        "SELECT COUNT(t) FROM Teachers t WHERE t.createdDate >= :last24Hours")
                .setParameter("last24Hours", last24Hours)
                .getSingleResult();

        // Tổng số nhân viên mới (trong 24 giờ)
        Long newEmployees = (Long) entityManager.createQuery(
                        "SELECT COUNT(e) FROM Staffs e WHERE e.createdDate >= :last24Hours")
                .setParameter("last24Hours", last24Hours)
                .getSingleResult();

        // Trả về dữ liệu dưới dạng JSON
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStudents", totalStudents);
        stats.put("totalTeachers", totalTeachers);
        stats.put("totalEmployees", totalEmployees);
        stats.put("totalBlog", totalBlog);
        stats.put("OnRoom", OnRoom);
        stats.put("OffRoom", OffRoom);
        stats.put("totalMessages", totalMessages);
        stats.put("newStudents", newStudents);
        stats.put("newTeachers", newTeachers);
        stats.put("newEmployees", newEmployees);
        stats.put("visitor", visitor);
        return stats;
    }
}