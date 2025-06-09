package com.example.demo.ControllerPOST;

import com.example.demo.ModelOOP.*;
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
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
@Transactional
public class ThoiKhoaBieuPost {
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/LuuLichHocNhieuSlot")
    @Transactional
    public String luuLichHocNhieuSlot(
            @RequestParam("roomId") String roomId,
            @RequestParam("year") Integer year,
            @RequestParam("week") Integer week,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        Room roomtest = entityManager.find(Room.class, roomId);
        if (roomtest.getSlotQuantity() == null) {
            redirectAttributes.addFlashAttribute("error", "Slot Quantity is null");
            return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();

        try {
            Staffs employee = entityManager.find(Staffs.class, employeeId);
            if (employee == null) {
                redirectAttributes.addAttribute("error", "EmployeeNotFound");
                return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
            }

            Room room = entityManager.find(Room.class, roomId);
            if (room == null) {
                redirectAttributes.addAttribute("error", "RoomNotFound");
                return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
            }

            LocalDate firstDayOfYear = LocalDate.of(year, 1, 1);
            LocalDate monday = firstDayOfYear.with(WeekFields.ISO.weekOfWeekBasedYear(), week)
                    .with(WeekFields.ISO.dayOfWeek(), 1);

            List<Timetable> baseTimetables = new ArrayList<>();
            for (String key : allParams.keySet()) {
                if (key.startsWith("slotDay_")) {
                    String[] parts = allParams.get(key).split(",");
                    Long slotId = Long.parseLong(parts[0]);
                    String day = parts[1];

                    Slots slot = entityManager.find(Slots.class, slotId);
                    if (slot == null) {
                        continue;
                    }

                    LocalDate date = monday.plusDays(DayOfWeek.valueOf(day).ordinal());

                    List<Timetable> roomConflicts = entityManager.createQuery(
                                    "FROM Timetable t WHERE t.room.roomId = :roomId AND t.slot.slotId = :slotId AND t.date = :date",
                                    Timetable.class)
                            .setParameter("roomId", roomId)
                            .setParameter("slotId", slotId)
                            .setParameter("date", date)
                            .getResultList();

                    if (!roomConflicts.isEmpty()) {
                        redirectAttributes.addAttribute("error", "ScheduleExists");
                        return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
                    }

                    List<Timetable> slotConflicts = entityManager.createQuery(
                                    "FROM Timetable t WHERE t.slot.slotId = :slotId AND t.date = :date AND t.room.roomId != :roomId",
                                    Timetable.class)
                            .setParameter("slotId", slotId)
                            .setParameter("date", date)
                            .setParameter("roomId", roomId)
                            .getResultList();

                    if (!slotConflicts.isEmpty()) {
                        redirectAttributes.addAttribute("error", "SlotAlreadyUsedByAnotherRoom");
                        return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
                    }

                    Timetable timetable = new Timetable();
                    timetable.setSlot(slot);
                    timetable.setDayOfWeek(DayOfWeek.valueOf(day));
                    timetable.setDate(date);
                    timetable.setRoom(room);
                    timetable.setEditor(employee);
                    baseTimetables.add(timetable);
                }
            }

            if (baseTimetables.isEmpty()) {
                redirectAttributes.addAttribute("error", "NoSlotsSelected");
                return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
            }

            List<Timetable> roomTimetables = entityManager.createQuery(
                            "FROM Timetable t WHERE t.room.roomId = :roomId",
                            Timetable.class)
                    .setParameter("roomId", roomId)
                    .getResultList();

            int usedSlots = roomTimetables.size();
            int slotQuantity = room.getSlotQuantity() != null ? room.getSlotQuantity() : Integer.MAX_VALUE;
            int slotsPerWeek = baseTimetables.size();
            int remainingSlots = slotQuantity - usedSlots;
            int maxWeeksToRepeat = slotsPerWeek > 0 ? (remainingSlots / slotsPerWeek) + 1 : 0;
            int addedWeeks = 0;
            int totalSlotsAdded = 0;
            List<Timetable> allNewTimetables = new ArrayList<>(baseTimetables);

            LocalDate currentMonday = monday;
            for (int i = 0; i < maxWeeksToRepeat && usedSlots < slotQuantity; i++) {
                currentMonday = monday.plusWeeks(i);
                LocalDate currentSunday = currentMonday.plusDays(6);

                List<Timetable> existingWeekTimetables = entityManager.createQuery(
                                "FROM Timetable t WHERE t.room.roomId = :roomId AND t.date BETWEEN :startDate AND :endDate",
                                Timetable.class)
                        .setParameter("roomId", roomId)
                        .setParameter("startDate", currentMonday)
                        .setParameter("endDate", currentSunday)
                        .getResultList();

                if (!existingWeekTimetables.isEmpty() && i > 0) {
                    continue;
                }

                for (Timetable baseTimetable : baseTimetables) {
                    LocalDate newDate = currentMonday.plusDays(baseTimetable.getDayOfWeek().ordinal());
                    Long slotId = baseTimetable.getSlot().getSlotId();

                    List<Timetable> roomConflicts = entityManager.createQuery(
                                    "FROM Timetable t WHERE t.room.roomId = :roomId AND t.slot.slotId = :slotId AND t.date = :date",
                                    Timetable.class)
                            .setParameter("roomId", roomId)
                            .setParameter("slotId", slotId)
                            .setParameter("date", newDate)
                            .getResultList();

                    if (!roomConflicts.isEmpty()) {
                        redirectAttributes.addAttribute("error", "ScheduleExists");
                        return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
                    }

                    List<Timetable> slotConflicts = entityManager.createQuery(
                                    "FROM Timetable t WHERE t.slot.slotId = :slotId AND t.date = :date AND t.room.roomId != :roomId",
                                    Timetable.class)
                            .setParameter("slotId", slotId)
                            .setParameter("date", newDate)
                            .setParameter("roomId", roomId)
                            .getResultList();

                    if (!slotConflicts.isEmpty()) {
                        redirectAttributes.addAttribute("error", "SlotAlreadyUsedByAnotherRoom");
                        return "redirect:/ThoiKhoaBieu?year=" + year + "&week=" + week;
                    }
                }

                for (Timetable baseTimetable : baseTimetables) {
                    if (usedSlots >= slotQuantity) {
                        break;
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
                    allNewTimetables.add(newTimetable);
                }

                if (i > 0) {
                    addedWeeks++;
                }
            }

            if (!allNewTimetables.isEmpty()) {
                allNewTimetables.sort((t1, t2) -> {
                    int dateCompare = t1.getDate().compareTo(t2.getDate());
                    if (dateCompare != 0) {
                        return dateCompare;
                    }
                    return t1.getSlot().getStartTime().compareTo(t2.getSlot().getStartTime());
                });

                Timetable firstTimetable = allNewTimetables.get(0);
                LocalDateTime startTime = LocalDateTime.of(firstTimetable.getDate(), firstTimetable.getSlot().getStartTime());
                room.setStartTime(startTime);

                Timetable lastTimetable = allNewTimetables.get(allNewTimetables.size() - 1);
                LocalDateTime endTime = LocalDateTime.of(lastTimetable.getDate(), lastTimetable.getSlot().getEndTime());
                room.setEndTime(endTime);

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

    @PostMapping("/DiemDanh")
    @Transactional
    public String saveAttendance(
            @RequestParam("timetableId") Long timetableId,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        try {
            Timetable timetable = entityManager.find(Timetable.class, timetableId);
            if (timetable == null) {
                redirectAttributes.addAttribute("error", "TimetableNotFound");
                return "redirect:/ChiTietBuoiHoc?timetableId=" + timetableId;
            }

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

            Teachers markingTeacher = entityManager.find(Teachers.class, userId);
            Staffs markingEmployee = entityManager.find(Staffs.class, userId);
            if (markingTeacher == null && markingEmployee == null) {
                redirectAttributes.addAttribute("error", "UserNotFound");
                return "redirect:/ChiTietBuoiHoc?timetableId=" + timetableId;
            }

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

            for (Attendances attendance : existingAttendances) {
                entityManager.remove(attendance);
            }

            for (Students student : students) {
                String statusKey = "status_" + student.getId();
                String noteKey = "note_" + student.getId();
                String status = allParams.getOrDefault(statusKey, "Absent");
                String note = allParams.get(noteKey); // Lấy trực tiếp từ form, không tự động thêm

                Attendances attendance = new Attendances(
                        teacher,
                        student,
                        timetable.getSlot(),
                        status,
                        note, // Giữ nguyên giá trị từ form, null nếu không nhập
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