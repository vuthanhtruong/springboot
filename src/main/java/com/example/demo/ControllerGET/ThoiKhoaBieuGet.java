package com.example.demo.ControllerGET;

import com.example.demo.ModelOOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ThoiKhoaBieuGet {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/ThoiKhoaBieu")
    public String DieuChinhLichHoc(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "week", required = false) Integer week,
            ModelMap model,
            HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // EmployeeID đã đăng nhập

        // Tìm Employee trong database
        Employees employee = entityManager.find(Employees.class, employeeId);
        if (employee == null) {
            model.addAttribute("error", "Không tìm thấy nhân viên!");
            return "errorPage";
        }

        // Xử lý year và week
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentWeek = today.get(WeekFields.ISO.weekOfWeekBasedYear());

        if (year == null) year = currentYear;
        if (week == null) week = currentWeek;

        // Tạo danh sách years và weeks cho form lọc
        List<Integer> years = new ArrayList<>();
        for (int i = currentYear - 5; i <= currentYear + 5; i++) {
            years.add(i);
        }
        List<Integer> weeks = new ArrayList<>();
        for (int i = 1; i <= 53; i++) {
            weeks.add(i);
        }

        // Tính toán ngày đầu tuần (thứ Hai) của tuần được chọn
        LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
        LocalDate monday = firstDayOfYear.with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                .with(WeekFields.ISO.dayOfWeek(), 1); // Thứ Hai
        LocalDate sunday = monday.plusDays(6); // Chủ Nhật

        // Tạo danh sách ngày trong tuần (dd/MM)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        List<String> weekDates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            weekDates.add(monday.plusDays(i).format(formatter));
        }

        // Tạo chuỗi ngày tháng đầy đủ cho khoảng thời gian (dd/MM/yyyy - dd/MM/yyyy)
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String weekRangeFull = monday.format(fullFormatter) + " - " + sunday.format(fullFormatter);

        // Tạo chuỗi ngày đầu tuần (thứ Hai) với định dạng dd/MM
        String mondayDate = monday.format(formatter);

        // Lưu weekDates vào session để sử dụng trong NhanVienPost
        session.setAttribute("weekDates", weekDates);

        // Lấy danh sách tất cả Slots (Slot 1 đến Slot 6)
        List<Slots> slots = entityManager.createQuery("FROM Slots ORDER BY slotId ASC", Slots.class)
                .getResultList();

        // Lấy danh sách tất cả Rooms (bao gồm cả Rooms và OnlineRooms)
        List<Room> allRooms = entityManager.createQuery("FROM Room", Room.class).getResultList();

        // Lấy danh sách Timetable của nhân viên này trong tuần được chọn
        List<Timetable> timetables = entityManager.createQuery(
                        "FROM Timetable t WHERE t.editor.id = :employeeId AND t.date BETWEEN :startDate AND :endDate",
                        Timetable.class)
                .setParameter("employeeId", employeeId)
                .setParameter("startDate", monday)
                .setParameter("endDate", sunday)
                .getResultList();

        // Tạo danh sách các ngày trong tuần (MONDAY đến SUNDAY)
        List<String> daysOfWeek = Arrays.asList("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");

        // Đưa dữ liệu vào model để hiển thị trong view
        model.addAttribute("employee", employee);
        model.addAttribute("slots", slots);
        model.addAttribute("timetables", timetables);
        model.addAttribute("allRooms", allRooms);
        model.addAttribute("weekDates", weekDates);
        model.addAttribute("daysOfWeek", daysOfWeek);
        model.addAttribute("years", years);
        model.addAttribute("weeks", weeks);
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedWeek", week);
        model.addAttribute("weekRange", monday.format(formatter) + " TO " + sunday.format(formatter));
        model.addAttribute("weekRangeFull", weekRangeFull);
        model.addAttribute("mondayDate", mondayDate); // Thêm ngày đầu tuần

        return "DieuChinhLichHoc";
    }

    @GetMapping("/ChiTietBuoiHoc")
    public String chiTietBuoiHoc(@RequestParam("timetableId") Long timetableId, Model model) {
        Timetable timetable = entityManager.find(Timetable.class, timetableId);
        if (timetable == null) {
            return "redirect:/ThoiKhoaBieu?error=TimetableNotFound";
        }
        if (timetable.getRoom() == null) {
            return "redirect:/ThoiKhoaBieu?error=RoomNotFound";
        }

        List<Teachers> teachers = entityManager.createQuery(
                        "SELECT DISTINCT cd.member FROM ClassroomDetails cd WHERE cd.room.roomId = :roomId AND TYPE(cd.member) = Teachers",
                        Teachers.class)
                .setParameter("roomId", timetable.getRoom().getRoomId())
                .getResultList();

        List<Students> students = entityManager.createQuery(
                        "SELECT DISTINCT cd.member FROM ClassroomDetails cd WHERE cd.room.roomId = :roomId AND TYPE(cd.member) = Students",
                        Students.class)
                .setParameter("roomId", timetable.getRoom().getRoomId())
                .getResultList();

        List<Attendances> existingAttendances = entityManager.createQuery(
                        "FROM Attendances a WHERE a.timetable.timetableId = :timetableId",
                        Attendances.class)
                .setParameter("timetableId", timetableId)
                .getResultList();

        Map<String, Attendances> attendanceMap = new HashMap<>();
        for (Attendances attendance : existingAttendances) {
            Students student = attendance.getStudent();
            if (student != null) {
                attendanceMap.put(student.getId(), attendance);
            }
        }

        // Đảm bảo teacher không null hoặc xử lý trường hợp rỗng
        Teachers teacher = !teachers.isEmpty() ? teachers.get(0) : null;
        if (teacher == null) {
            model.addAttribute("errorMessage", "Không tìm thấy giáo viên cho buổi học này.");
        }

        // Kiểm tra vai trò người dùng
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isStudent = false;
        boolean isTeacher = false;
        boolean isEmployee = false;

        if (authentication != null) {
            isStudent = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_STUDENT"));
            isTeacher = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TEACHER"));
            isEmployee = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_EMPLOYEE"));
        }

        // Xác định đường dẫn "Quay lại" dựa trên vai trò
        String backUrl;
        if (isStudent || isTeacher) {
            backUrl = "/ThoiKhoaBieuNguoiDung";
        } else {
            backUrl = "/ThoiKhoaBieu"; // Mặc định cho nhân viên hoặc trường hợp không xác định
        }

        // Thêm các thuộc tính vào model
        model.addAttribute("teacher", teacher);
        model.addAttribute("timetable", timetable);
        model.addAttribute("students", students);
        model.addAttribute("attendanceMap", attendanceMap);
        model.addAttribute("isStudent", isStudent); // Để kiểm soát giao diện điểm danh
        model.addAttribute("backUrl", backUrl); // Lưu đường dẫn "Quay lại"

        return "ChiTietBuoiHoc";
    }

    @GetMapping("/ThoiKhoaBieuNguoiDung")
    public String getUserTimetable(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "week", required = false) Integer week,
            Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        // Tìm Employee trong database
        Person person = entityManager.find(Person.class, userId);
        if (person instanceof Teachers) {
            model.addAttribute("URL", "/TrangChuGiaoVien");
        } else {
            model.addAttribute("URL", "/TrangChuHocSinh");
        }

        // Xử lý giá trị mặc định trong phương thức
        LocalDate now = LocalDate.now();
        if (year == null) {
            year = now.getYear();
        }
        if (week == null) {
            week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        }

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