package com.example.demo.GET;

import com.example.demo.OOP.Slots;
import com.example.demo.OOP.Students;
import com.example.demo.OOP.Teachers;
import com.example.demo.OOP.Timetable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ThoiKhoaBieuGet {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/ThoiKhoaBieuNguoiDung")
    public String getUserTimetable(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "week", required = false) Integer week,
            Model model) {

        // Xử lý giá trị mặc định trong phương thức
        LocalDate now = LocalDate.now();
        if (year == null) {
            year = now.getYear();
        }
        if (week == null) {
            week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        }

        // Lấy thông tin người dùng đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Kiểm tra xem người dùng là giáo viên hay học sinh
        Teachers teacher = entityManager.find(Teachers.class, userId);
        Students student = entityManager.find(Students.class, userId);

        if (teacher == null && student == null) {
            model.addAttribute("errorMessage", "Không tìm thấy thông tin người dùng.");
            return "ThoiKhoaBieuNguoiDung";
        }

        // Xác định loại người dùng và tên
        String userType = teacher != null ? "Teacher" : "Student";
        String userName = teacher != null ?
                (teacher.getLastName() + " " + teacher.getFirstName()) :
                (student.getLastName() + " " + student.getFirstName());

        // Tính toán ngày đầu tuần từ năm và tuần
        LocalDate startOfWeek = LocalDate.ofYearDay(year, 1)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(DayOfWeek.MONDAY);

        // Danh sách ngày trong tuần
        List<LocalDate> weekDates = Arrays.asList(
                startOfWeek, startOfWeek.plusDays(1), startOfWeek.plusDays(2),
                startOfWeek.plusDays(3), startOfWeek.plusDays(4), startOfWeek.plusDays(5),
                startOfWeek.plusDays(6)
        );
        List<String> daysOfWeek = Arrays.stream(DayOfWeek.values())
                .map(DayOfWeek::name)
                .collect(Collectors.toList());

        // Lấy tất cả slot
        List<Slots> slots = entityManager.createQuery("SELECT s FROM Slots s", Slots.class)
                .getResultList();

        // Lấy thời khóa biểu của người dùng trong tuần
        List<Timetable> timetables;
        if (teacher != null) {
            timetables = entityManager.createQuery(
                            "SELECT t FROM Timetable t " +
                                    "JOIN ClassroomDetails cd ON cd.room.roomId = t.room.roomId " +
                                    "WHERE cd.member = :teacher " +
                                    "AND t.date BETWEEN :startDate AND :endDate",
                            Timetable.class)
                    .setParameter("teacher", teacher)
                    .setParameter("startDate", startOfWeek)
                    .setParameter("endDate", startOfWeek.plusDays(6))
                    .getResultList();
        } else {
            timetables = entityManager.createQuery(
                            "SELECT t FROM Timetable t " +
                                    "JOIN ClassroomDetails cd ON cd.room.roomId = t.room.roomId " +
                                    "WHERE cd.member = :student " +
                                    "AND t.date BETWEEN :startDate AND :endDate",
                            Timetable.class)
                    .setParameter("student", student)
                    .setParameter("startDate", startOfWeek)
                    .setParameter("endDate", startOfWeek.plusDays(6))
                    .getResultList();
        }

        // Thêm dữ liệu vào model
        model.addAttribute("userType", userType);
        model.addAttribute("userName", userName);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedWeek", week);
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("daysOfWeek", daysOfWeek);
        model.addAttribute("slots", slots);
        model.addAttribute("timetables", timetables);
        model.addAttribute("weekRange", String.format("%s - %s",
                weekDates.get(0).toString(), weekDates.get(6).toString()));

        return "ThoiKhoaBieuNguoiDung";
    }
}