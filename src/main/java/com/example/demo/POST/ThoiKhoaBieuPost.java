package com.example.demo.POST;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
@Transactional

public class ThoiKhoaBieuPost {
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/LuuLichHoc")
    public String luuLichHoc(
            @RequestParam("slotId") Long slotId,
            @RequestParam("day") String day,
            @RequestParam("roomId") String roomId,
            @RequestParam("year") Integer year,
            @RequestParam("week") Integer week,
            RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();

        // Tìm Employee
        Employees employee = entityManager.find(Employees.class, employeeId);
        if (employee == null) {
            redirectAttributes.addAttribute("error", "EmployeeNotFound");
            return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
        }

        // Tìm Slot
        Slots slot = entityManager.find(Slots.class, slotId);
        if (slot == null) {
            redirectAttributes.addAttribute("error", "SlotNotFound");
            return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
        }

        // Tìm Room
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            redirectAttributes.addAttribute("error", "RoomNotFound");
            return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
        }

        // Tính ngày bắt đầu (thứ Hai của tuần được chọn)
        LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
        LocalDate monday = firstDayOfYear.with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                .with(WeekFields.ISO.dayOfWeek(), 1); // Thứ Hai
        LocalDate sunday = monday.plusDays(6); // Chủ Nhật

        // Tính ngày cụ thể dựa trên day (MONDAY, TUESDAY, ...)
        int dayOffset = Timetable.DayOfWeek.valueOf(day).ordinal();
        LocalDate date = monday.plusDays(dayOffset);

        // Kiểm tra xem lịch học đã tồn tại cho slot và ngày này chưa
        List<Timetable> existingTimetables = entityManager.createQuery(
                        "FROM Timetable t WHERE t.slot.slotId = :slotId AND t.dayOfWeek = :dayOfWeek AND t.date = :date",
                        Timetable.class)
                .setParameter("slotId", slotId)
                .setParameter("dayOfWeek", Timetable.DayOfWeek.valueOf(day))
                .setParameter("date", date)
                .getResultList();

        if (!existingTimetables.isEmpty()) {
            redirectAttributes.addAttribute("error", "ScheduleExists");
            return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
        }

        // Kiểm tra xem phòng đã được sử dụng trong slot này chưa
        List<Timetable> conflictingTimetables = entityManager.createQuery(
                        "FROM Timetable t WHERE t.room.roomId = :roomId AND t.slot.slotId = :slotId AND t.date = :date",
                        Timetable.class)
                .setParameter("roomId", roomId)
                .setParameter("slotId", slotId)
                .setParameter("date", date)
                .getResultList();

        if (!conflictingTimetables.isEmpty()) {
            redirectAttributes.addAttribute("error", "RoomAlreadyUsed");
            return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
        }

        // Đếm tổng số slot đã sử dụng cho phòng này
        List<Timetable> roomTimetables = entityManager.createQuery(
                        "FROM Timetable t WHERE t.room.roomId = :roomId",
                        Timetable.class)
                .setParameter("roomId", roomId)
                .getResultList();

        int usedSlots = roomTimetables.size();
        int slotQuantity = room.getSlotQuantity() != null ? room.getSlotQuantity() : Integer.MAX_VALUE;

        // Nếu đã đạt slotQuantity, xóa bớt slot cũ
        if (usedSlots >= slotQuantity) {
            // Sắp xếp theo ngày để xóa slot cũ nhất
            roomTimetables.sort(Comparator.comparing(Timetable::getDate));
            Timetable oldestTimetable = roomTimetables.get(0);
            entityManager.remove(oldestTimetable);
            usedSlots--; // Giảm số slot đã sử dụng
        }

        // Tạo lịch học cho tuần hiện tại
        Timetable timetable = new Timetable();
        timetable.setSlot(slot);
        timetable.setDayOfWeek(Timetable.DayOfWeek.valueOf(day));
        timetable.setDate(date);
        timetable.setRoom(room);
        timetable.setEditor(employee);
        entityManager.persist(timetable);
        usedSlots++; // Cập nhật số slot đã sử dụng

        // Lấy tất cả lịch học của Room trong tuần hiện tại (bao gồm lịch vừa thêm)
        List<Timetable> weeklyTimetables = entityManager.createQuery(
                        "FROM Timetable t WHERE t.room.roomId = :roomId AND t.date BETWEEN :startDate AND :endDate",
                        Timetable.class)
                .setParameter("roomId", roomId)
                .setParameter("startDate", monday)
                .setParameter("endDate", sunday)
                .getResultList();

        // Tính số lần lặp tối đa dựa trên slotQuantity
        int slotsPerWeek = weeklyTimetables.size();
        if (slotsPerWeek == 0) {
            slotsPerWeek = 1; // Đảm bảo không chia cho 0
        }
        int remainingSlots = slotQuantity - usedSlots;
        int maxWeeksToRepeat = remainingSlots / slotsPerWeek; // Số tuần tối đa có thể lặp lại
        int addedWeeks = 0; // Đếm số tuần đã thêm lịch

        // Tự động lặp lại toàn bộ lịch học của Room cho các tuần tiếp theo
        LocalDate currentMonday = monday;
        for (int i = 0; i < maxWeeksToRepeat; i++) {
            currentMonday = currentMonday.plusWeeks(1); // Tăng lên 1 tuần
            LocalDate currentSunday = currentMonday.plusDays(6);

            // Kiểm tra xem có lịch nào trong tuần này đã tồn tại cho Room chưa
            List<Timetable> futureTimetables = entityManager.createQuery(
                            "FROM Timetable t WHERE t.room.roomId = :roomId AND t.date BETWEEN :startDate AND :endDate",
                            Timetable.class)
                    .setParameter("roomId", roomId)
                    .setParameter("startDate", currentMonday)
                    .setParameter("endDate", currentSunday)
                    .getResultList();

            if (!futureTimetables.isEmpty()) {
                continue; // Bỏ qua nếu đã có lịch trong tuần này
            }

            // Lặp lại tất cả các slot của tuần hiện tại
            boolean canScheduleWeek = true;
            for (Timetable weeklyTimetable : weeklyTimetables) {
                LocalDate newDate = currentMonday.plusDays(weeklyTimetable.getDayOfWeek().ordinal());
                Long weeklySlotId = weeklyTimetable.getSlot().getSlotId();

                // Kiểm tra xem phòng đã được sử dụng trong slot này chưa
                List<Timetable> futureConflictingTimetables = entityManager.createQuery(
                                "FROM Timetable t WHERE t.room.roomId = :roomId AND t.slot.slotId = :slotId AND t.date = :date",
                                Timetable.class)
                        .setParameter("roomId", roomId)
                        .setParameter("slotId", weeklySlotId)
                        .setParameter("date", newDate)
                        .getResultList();

                if (!futureConflictingTimetables.isEmpty()) {
                    canScheduleWeek = false;
                    break; // Nếu có xung đột, bỏ qua tuần này
                }
            }

            if (!canScheduleWeek) {
                continue; // Bỏ qua tuần này nếu có xung đột
            }

            // Tạo lịch học cho tất cả slot trong tuần
            for (Timetable weeklyTimetable : weeklyTimetables) {
                LocalDate newDate = currentMonday.plusDays(weeklyTimetable.getDayOfWeek().ordinal());
                Timetable futureTimetable = new Timetable();
                futureTimetable.setSlot(weeklyTimetable.getSlot());
                futureTimetable.setDayOfWeek(weeklyTimetable.getDayOfWeek());
                futureTimetable.setDate(newDate);
                futureTimetable.setRoom(room);
                futureTimetable.setEditor(employee);
                entityManager.persist(futureTimetable);
                usedSlots++; // Cập nhật số slot đã sử dụng

                // Nếu vượt quá slotQuantity, xóa slot cũ
                if (usedSlots > slotQuantity) {
                    roomTimetables = entityManager.createQuery(
                                    "FROM Timetable t WHERE t.room.roomId = :roomId",
                                    Timetable.class)
                            .setParameter("roomId", roomId)
                            .getResultList();
                    roomTimetables.sort(Comparator.comparing(Timetable::getDate));
                    Timetable oldestTimetable = roomTimetables.get(0);
                    entityManager.remove(oldestTimetable);
                    usedSlots--;
                }
            }

            addedWeeks++; // Tăng số tuần đã thêm
        }

        redirectAttributes.addAttribute("success", "ScheduleSaved");
        if (addedWeeks > 0) {
            redirectAttributes.addAttribute("remainingSlots", addedWeeks);
        }
        return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
    }

    @PostMapping("/LuuLichHocNhieuSlot")
    @Transactional
    public String luuLichHocNhieuSlot(
            @RequestParam("roomId") String roomId,
            @RequestParam("year") Integer year,
            @RequestParam("week") Integer week,
            @RequestParam Map<String, String> allParams, // Capture all checkbox values
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();

        try {
            // Tìm Employee
            Employees employee = entityManager.find(Employees.class, employeeId);
            if (employee == null) {
                redirectAttributes.addAttribute("error", "EmployeeNotFound");
                return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
            }

            // Tìm Room
            Room room = entityManager.find(Room.class, roomId);
            if (room == null) {
                redirectAttributes.addAttribute("error", "RoomNotFound");
                return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
            }

            // Tính ngày bắt đầu của tuần đầu tiên
            LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
            LocalDate monday = firstDayOfYear.with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                    .with(WeekFields.ISO.dayOfWeek(), 1);

            // Lấy tất cả các slot-day được chọn từ checkbox
            List<Timetable> baseTimetables = new ArrayList<>();
            for (String key : allParams.keySet()) {
                if (key.startsWith("slotDay_")) {
                    String[] parts = allParams.get(key).split(",");
                    Long slotId = Long.parseLong(parts[0]);
                    String day = parts[1];

                    Slots slot = entityManager.find(Slots.class, slotId);
                    if (slot == null) {
                        continue; // Bỏ qua nếu slot không tồn tại
                    }

                    LocalDate date = monday.plusDays(Timetable.DayOfWeek.valueOf(day).ordinal());

                    // Kiểm tra xung đột cho tuần đầu tiên
                    List<Timetable> conflicts = entityManager.createQuery(
                                    "FROM Timetable t WHERE t.room.roomId = :roomId AND t.slot.slotId = :slotId AND t.date = :date",
                                    Timetable.class)
                            .setParameter("roomId", roomId)
                            .setParameter("slotId", slotId)
                            .setParameter("date", date)
                            .getResultList();

                    if (!conflicts.isEmpty()) {
                        continue; // Bỏ qua nếu có xung đột
                    }

                    // Tạo Timetable cho tuần đầu tiên
                    Timetable timetable = new Timetable();
                    timetable.setSlot(slot);
                    timetable.setDayOfWeek(Timetable.DayOfWeek.valueOf(day));
                    timetable.setDate(date);
                    timetable.setRoom(room);
                    timetable.setEditor(employee);
                    baseTimetables.add(timetable);
                }
            }

            // Kiểm tra slotQuantity
            List<Timetable> roomTimetables = entityManager.createQuery(
                            "FROM Timetable t WHERE t.room.roomId = :roomId",
                            Timetable.class)
                    .setParameter("roomId", roomId)
                    .getResultList();

            int usedSlots = roomTimetables.size();
            int slotQuantity = room.getSlotQuantity() != null ? room.getSlotQuantity() : Integer.MAX_VALUE;
            int slotsPerWeek = baseTimetables.size();

            if (slotsPerWeek == 0) {
                redirectAttributes.addAttribute("error", "NoSlotsSelected");
                return "redirect:/DieuChinhLichHoc?year=" + year + "&week=" + week;
            }

            // Tính số tuần tối đa có thể lặp lại dựa trên slotQuantity
            int remainingSlots = slotQuantity - usedSlots;
            int maxWeeksToRepeat = slotsPerWeek > 0 ? (remainingSlots / slotsPerWeek) + 1 : 0; // +1 để tính cả tuần đầu
            int addedWeeks = 0;
            int totalSlotsAdded = 0;

            // Danh sách để tính startTime và endTime
            List<Timetable> allNewTimetables = new ArrayList<>(baseTimetables);

            // Lặp lại lịch học cho đến khi hết slotQuantity
            LocalDate currentMonday = monday;
            for (int i = 0; i < maxWeeksToRepeat && usedSlots < slotQuantity; i++) {
                currentMonday = monday.plusWeeks(i); // Tuần hiện tại (bao gồm tuần đầu tiên khi i=0)
                LocalDate currentSunday = currentMonday.plusDays(6);

                // Kiểm tra xem tuần này đã có lịch nào cho room chưa
                List<Timetable> existingWeekTimetables = entityManager.createQuery(
                                "FROM Timetable t WHERE t.room.roomId = :roomId AND t.date BETWEEN :startDate AND :endDate",
                                Timetable.class)
                        .setParameter("roomId", roomId)
                        .setParameter("startDate", currentMonday)
                        .setParameter("endDate", currentSunday)
                        .getResultList();

                if (!existingWeekTimetables.isEmpty() && i > 0) { // Cho phép tuần đầu tiên dù có lịch
                    continue; // Bỏ qua nếu tuần sau đã có lịch
                }

                // Kiểm tra xung đột cho toàn bộ tuần
                boolean canScheduleWeek = true;
                for (Timetable baseTimetable : baseTimetables) {
                    LocalDate newDate = currentMonday.plusDays(baseTimetable.getDayOfWeek().ordinal());
                    Long slotId = baseTimetable.getSlot().getSlotId();

                    List<Timetable> conflicts = entityManager.createQuery(
                                    "FROM Timetable t WHERE t.room.roomId = :roomId AND t.slot.slotId = :slotId AND t.date = :date",
                                    Timetable.class)
                            .setParameter("roomId", roomId)
                            .setParameter("slotId", slotId)
                            .setParameter("date", newDate)
                            .getResultList();

                    if (!conflicts.isEmpty()) {
                        canScheduleWeek = false;
                        break;
                    }
                }

                if (!canScheduleWeek) {
                    continue; // Bỏ qua tuần này nếu có xung đột
                }

                // Lưu lịch cho tuần hiện tại
                for (Timetable baseTimetable : baseTimetables) {
                    if (usedSlots >= slotQuantity) {
                        break; // Dừng nếu đã hết slotQuantity
                    }

                    LocalDate newDate = currentMonday.plusDays(baseTimetable.getDayOfWeek().ordinal());
                    Timetable newTimetable = new Timetable();
                    newTimetable.setSlot(baseTimetable.getSlot());
                    newTimetable.setDayOfWeek(baseTimetable.getDayOfWeek());
                    newTimetable.setDate(newDate);
                    newTimetable.setRoom(room);
                    newTimetable.setEditor(employee);
                    entityManager.persist(newTimetable);

                    usedSlots++;
                    totalSlotsAdded++;
                    allNewTimetables.add(newTimetable); // Thêm vào danh sách để tính startTime/endTime
                }

                if (i > 0) { // Không đếm tuần đầu tiên là tuần lặp lại
                    addedWeeks++;
                }
            }

            // Cập nhật startTime và endTime cho Room
            if (!allNewTimetables.isEmpty()) {
                // Sắp xếp danh sách Timetable theo ngày và slot để tìm slot đầu tiên và cuối cùng
                allNewTimetables.sort((t1, t2) -> {
                    int dateCompare = t1.getDate().compareTo(t2.getDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    return t1.getSlot().getStartTime().compareTo(t2.getSlot().getStartTime());
                });

                // Slot đầu tiên (sớm nhất)
                Timetable firstTimetable = allNewTimetables.get(0);
                LocalDateTime startTime = LocalDateTime.of(firstTimetable.getDate(), firstTimetable.getSlot().getStartTime());
                room.setStartTime(startTime);

                // Slot cuối cùng (muộn nhất)
                Timetable lastTimetable = allNewTimetables.get(allNewTimetables.size() - 1);
                LocalDateTime endTime = LocalDateTime.of(lastTimetable.getDate(), lastTimetable.getSlot().getEndTime());
                room.setEndTime(endTime);

                // Cập nhật Room vào database
                entityManager.merge(room);
            }

            redirectAttributes.addAttribute("success", "ScheduleSaved");
            redirectAttributes.addAttribute("slotsAdded", totalSlotsAdded);
            if (addedWeeks > 0) {
                redirectAttributes.addAttribute("weeksRepeated", addedWeeks);
            }
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "SaveFailed");
            e.printStackTrace();
        }

        return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
    }

    @PostMapping("/XoaLichHoc")
    @Transactional
    public String xoaLichHoc(
            @RequestParam("timetableId") Long timetableId,
            @RequestParam("year") Integer year,
            @RequestParam("week") Integer week,
            RedirectAttributes redirectAttributes) {

        // Tìm timetable cụ thể theo timetableId
        Timetable timetable = entityManager.find(Timetable.class, timetableId);
        if (timetable == null) {
            redirectAttributes.addAttribute("error", "InvalidTimetable");
            return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
        }

        // Lấy Room trước khi xóa timetable
        Room room = timetable.getRoom();
        String roomId = room.getRoomId();

        // Xóa timetable
        entityManager.remove(timetable);

        // Lấy tất cả các timetable còn lại của Room
        List<Timetable> remainingTimetables = entityManager.createQuery(
                        "FROM Timetable t WHERE t.room.roomId = :roomId",
                        Timetable.class)
                .setParameter("roomId", roomId)
                .getResultList();

        // Cập nhật startTime và endTime của Room
        if (remainingTimetables.isEmpty()) {
            // Nếu không còn timetable nào, đặt startTime và endTime về null
            room.setStartTime(null);
            room.setEndTime(null);
        } else {
            // Sắp xếp danh sách Timetable theo ngày và slot để tìm slot đầu tiên và cuối cùng
            remainingTimetables.sort((t1, t2) -> {
                int dateCompare = t1.getDate().compareTo(t2.getDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return t1.getSlot().getStartTime().compareTo(t2.getSlot().getStartTime());
            });

            // Slot đầu tiên (sớm nhất)
            Timetable firstTimetable = remainingTimetables.get(0);
            LocalDateTime startTime = LocalDateTime.of(firstTimetable.getDate(), firstTimetable.getSlot().getStartTime());
            room.setStartTime(startTime);

            // Slot cuối cùng (muộn nhất)
            Timetable lastTimetable = remainingTimetables.get(remainingTimetables.size() - 1);
            LocalDateTime endTime = LocalDateTime.of(lastTimetable.getDate(), lastTimetable.getSlot().getEndTime());
            room.setEndTime(endTime);
        }

        // Cập nhật Room vào database
        entityManager.merge(room);

        redirectAttributes.addAttribute("success", "ScheduleDeleted");
        return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
    }

    @PostMapping("/DiemDanh")
    @Transactional
    public String saveAttendance(
            @RequestParam("timetableId") Long timetableId,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        // Lấy thông tin người dùng đăng nhập
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        try {
            // Tìm Timetable
            Timetable timetable = entityManager.find(Timetable.class, timetableId);
            if (timetable == null) {
                redirectAttributes.addAttribute("error", "TimetableNotFound");
                return "redirect:/ChiTietBuoiHoc?timetableId=" + timetableId;
            }

            // Lấy giáo viên từ ClassroomDetails
            List<Teachers> teachers = entityManager.createQuery(
                            "SELECT DISTINCT cd.member FROM ClassroomDetails cd WHERE cd.room.roomId = :roomId AND TYPE(cd.member) = Teachers",
                            Teachers.class)
                    .setParameter("roomId", timetable.getRoom().getRoomId())
                    .getResultList();

            Teachers teacher = teachers.isEmpty() ? null : teachers.get(0);
            if (teacher == null) {
                redirectAttributes.addAttribute("error", "TeacherNotFound");
                return "redirect:/ChiTietBuoiHoc?timetableId=" + timetableId;
            }

            // Kiểm tra quyền của người dùng (Teachers hoặc Employees)
            Teachers markingTeacher = entityManager.find(Teachers.class, userId);
            Employees markingEmployee = entityManager.find(Employees.class, userId);
            if (markingTeacher == null && markingEmployee == null) {
                redirectAttributes.addAttribute("error", "UserNotFound");
                return "redirect:/ChiTietBuoiHoc?timetableId=" + timetableId;
            }

            // Lấy danh sách học sinh trong phòng
            List<Students> students = entityManager.createQuery(
                            "SELECT DISTINCT cd.member FROM ClassroomDetails cd WHERE cd.room.roomId = :roomId AND TYPE(cd.member) = Students",
                            Students.class)
                    .setParameter("roomId", timetable.getRoom().getRoomId())
                    .getResultList();

            // Lấy danh sách điểm danh hiện tại để cập nhật hoặc xóa
            List<Attendances> existingAttendances = entityManager.createQuery(
                            "FROM Attendances a WHERE a.timetable.timetableId = :timetableId",
                            Attendances.class)
                    .setParameter("timetableId", timetableId)
                    .getResultList();

            // Xóa các bản ghi điểm danh cũ (ghi đè toàn bộ)
            for (Attendances attendance : existingAttendances) {
                entityManager.remove(attendance);
            }

            // Lưu điểm danh mới cho từng học sinh
            for (Students student : students) {
                String statusKey = "status_" + student.getId();
                String noteKey = "note_" + student.getId();
                String status = allParams.getOrDefault(statusKey, "Absent");
                String note = allParams.get(noteKey);

                // Nếu nhân viên điểm danh hộ, thêm ghi chú với tên nhân viên
                if (markingEmployee != null) {
                    String employeeName = markingEmployee.getLastName() + " " + markingEmployee.getFirstName(); // Giả sử có các trường này
                    String employeeNote = "Nhân viên " + employeeName + " điểm danh hộ vì giáo viên quên điểm danh";
                    if (note == null || note.trim().isEmpty()) {
                        note = employeeNote; // Nếu không có ghi chú, dùng ghi chú của nhân viên
                    } else {
                        note = note + " - " + employeeNote; // Nếu đã có ghi chú, nối thêm
                    }
                }

                // Tạo bản ghi Attendance mới
                Attendances attendance = new Attendances(
                        teacher, // Giáo viên từ ClassroomDetails
                        student,
                        timetable.getSlot(),
                        status,
                        note,
                        LocalDateTime.now()
                );
                attendance.setTimetable(timetable);
                entityManager.persist(attendance);
            }

            redirectAttributes.addAttribute("success", "AttendanceSaved");
        } catch (Exception e) {
            redirectAttributes.addAttribute("error", "SaveFailed");
            e.printStackTrace();
        }

        return "redirect:/ChiTietBuoiHoc?timetableId=" + timetableId;
    }
}
