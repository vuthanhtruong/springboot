package com.example.demo.GET;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    @GetMapping("/ThoiKhoaBieu")
    public String thoiKhoaBieu(
            @RequestParam(defaultValue = "2025") int year,
            @RequestParam(defaultValue = "12") int week,
            Model model) {

        // Lấy giáo viên hiện tại từ SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String teacherId = authentication.getName();
        Person person = entityManager.find(Person.class, teacherId);
        Teachers currentTeacher = (Teachers) person;

        // Tính ngày bắt đầu và kết thúc của tuần được chọn
        LocalDate startOfWeek = LocalDate.of(year, 1, 1)
                .with(WeekFields.of(Locale.getDefault()).weekOfYear(), week)
                .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1); // Thứ 2
        LocalDate endOfWeek = startOfWeek.plusDays(6); // Chủ nhật

        LocalDateTime startDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endDateTime = endOfWeek.atTime(23, 59, 59);

        // Truy vấn JPQL để lấy danh sách thời khóa biểu
        String jpql = "SELECT t FROM Timetable t WHERE t.teacher = :teacher " +
                "AND t.startTime >= :start AND t.startTime <= :end " +
                "ORDER BY t.startTime ASC";

        TypedQuery<Timetable> query = entityManager.createQuery(jpql, Timetable.class);
        query.setParameter("teacher", currentTeacher);
        query.setParameter("start", startDateTime);
        query.setParameter("end", endDateTime);

        List<Timetable> timetables = query.getResultList();

        // Tạo danh sách năm và tuần
        List<Integer> years = List.of(2023, 2024, 2025);
        List<Integer> weeks = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52);

        // Tính toán ngày bắt đầu và kết thúc cho từng tuần
        List<String> weekLabels = new ArrayList<>();
        for (int w : weeks) {
            LocalDate weekStart = LocalDate.of(year, 1, 1)
                    .with(WeekFields.of(Locale.getDefault()).weekOfYear(), w)
                    .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1); // Thứ 2
            LocalDate weekEnd = weekStart.plusDays(6); // Chủ nhật
            String label = "Tuần " + w + " (" + weekStart.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + " đến " + weekEnd.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")) + ")";
            weekLabels.add(label);
        }

        // Truyền dữ liệu vào model
        model.addAttribute("timetables", timetables);
        model.addAttribute("year", year);
        model.addAttribute("week", week);
        model.addAttribute("startOfWeek", startOfWeek);
        model.addAttribute("years", years);
        model.addAttribute("weeks", weeks);
        model.addAttribute("weekLabels", weekLabels);

        return "ThoiKhoaBieu";
    }

}
