package com.example.demo.GET;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/")
@Transactional
public class NhanVienGet {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private JavaMailSender mailSender; // Không khai báo lại ở nơi khác


    @GetMapping("/DangKyNhanVien")
    public String DangKyNhanVien() {
        return "DangKyNhanVien";
    }
    @GetMapping("/TrangChuNhanVien")
    public String TrangChuNhanVien(ModelMap model, HttpSession session) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        Employees employee = entityManager.find(Employees.class, session.getAttribute("EmployeeID"));
        model.addAttribute("employee", employee);
        return "TrangChuNhanVien";
    }
    @GetMapping("/DangXuatNhanVien")
    public String DangXuatGiaoVien(HttpSession session) {
        session.invalidate();
        return "redirect:/DangNhapNhanVien";
    }
    @GetMapping("/DanhSachGiaoVienCuaBan")
    public String DanhSachGiaoVienCuaBan(
            ModelMap model,
            HttpSession session,
            @RequestParam(defaultValue = "1") int page // Trang mặc định là 1
    ) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Lấy pageSize từ session, nếu chưa có thì mặc định là 5
        Integer pageSize = (Integer) session.getAttribute("pageSize");
        if (pageSize == null || pageSize < 1) {
            pageSize = 5;
        }

        // Đếm tổng số giáo viên
        Long totalTeachers = (Long) entityManager.createQuery("SELECT COUNT(t) FROM Teachers t")
                .getSingleResult();

        // Tính số trang mà KHÔNG dùng Math.ceil
        int totalPages = 1;
        if (totalTeachers != null && totalTeachers > 0) {
            totalPages = (int) (totalTeachers / pageSize);
            if (totalTeachers % pageSize != 0) { // Nếu còn dư thì cần thêm một trang nữa
                totalPages++;
            }
        }

        // Đảm bảo `page` nằm trong phạm vi hợp lệ mà không dùng `Math`
        int pageCount = 1;
        for (int i = 1; i <= totalPages; i++) {
            if (page == i) {
                pageCount = i;
            }
        }

        // Xác định vị trí bắt đầu của dữ liệu (không dùng công thức `Math`)
        int firstResult = 0;
        int count = 1;
        for (int i = pageSize; i <= totalTeachers; i += pageSize) {
            if (count == (pageCount - 1)) {
                firstResult = i;
                break;
            }
            count++;
        }

        // Lấy danh sách giáo viên theo phân trang
        List<Teachers> teachers = entityManager.createQuery("FROM Teachers", Teachers.class)
                .setFirstResult(firstResult)
                .setMaxResults(pageSize)
                .getResultList();

        // Gửi dữ liệu qua model
        model.addAttribute("teachers", teachers);
        model.addAttribute("currentPage", pageCount);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

        return "DanhSachGiaoVienCuaBan";
    }




    @GetMapping("/ThemGiaoVienCuaBan")
    public String ThemGiaoVienCuaBan(HttpSession session, ModelMap model) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        return "ThemGiaoVienCuaBan";
    }
    @GetMapping("/DanhSachHocSinhCuaBan")
    public String DanhSachHocSinhCuaBan(
            ModelMap model,
            HttpSession session,
            @RequestParam(defaultValue = "1") int page
    ) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Lấy pageSize từ session, nếu không có thì dùng giá trị mặc định là 5
        Integer sessionPageSize = (Integer) session.getAttribute("pageSize");
        int pageSize = (sessionPageSize != null) ? sessionPageSize : 5;

        // Đếm tổng số học sinh
        Long totalStudents = (Long) entityManager.createQuery("SELECT COUNT(s) FROM Students s")
                .getSingleResult();

        if (totalStudents == null || totalStudents == 0) {
            model.addAttribute("students", List.of());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 1);
            model.addAttribute("pageSize", pageSize);
            return "DanhSachHocSinhCuaBan";
        }

        int totalPages = totalStudents.intValue() / pageSize;
        if (totalStudents % pageSize != 0) {
            totalPages++;
        }
        int startIndex = 0, endIndex = 0;
        if (page > totalPages) {
            startIndex = (page - 1) * pageSize + 1;
            endIndex = totalStudents.intValue();
        } else {
            int count = 0;
            for (int i = pageSize; i < totalStudents; i += pageSize) {
                if (count == page - 1) {
                    startIndex = i - pageSize + 1;
                    endIndex = i;
                    break;
                } else {
                    count++;
                }
            }
        }
        if (startIndex <= 0) startIndex = 1;
        if (endIndex > totalStudents.intValue()) endIndex = totalStudents.intValue();

        List<Students> students = entityManager.createQuery("FROM Students", Students.class)
                .setFirstResult(startIndex - 1)  // Vì database index bắt đầu từ 0
                .setMaxResults(pageSize)
                .getResultList();

        // Gửi dữ liệu qua model
        model.addAttribute("students", students);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

        return "DanhSachHocSinhCuaBan";
    }


    @GetMapping("/ThemHocSinhCuaBan")
    public String ThemHocSinhCuaBan(ModelMap model, HttpSession session) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        return "ThemHocSinhCuaBan";
    }
    @GetMapping("/DanhSachNguoiDungHeThong")
    public String DanhSachNguoiDungHeThong(HttpSession session, ModelMap model) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        List<Employees> employee = entityManager.createQuery("from Employees ").getResultList();
        List<Teachers> teachers = entityManager.createQuery("from Teachers ").getResultList();
        List<Students> students = entityManager.createQuery("from Students ").getResultList();
        Employees employee1 = entityManager.find(Employees.class, session.getAttribute("EmployeeID"));
        Admin admin = employee1.getAdmin();
        model.addAttribute("employee", employee);
        model.addAttribute("teachers", teachers);
        model.addAttribute("students", students);
        model.addAttribute("employee", employee);
        model.addAttribute("admin", admin);
        return "DanhSachNguoiDungHeThong";
    }
    @GetMapping("/XoaGiaoVienCuaBan/{id}")
    @Transactional
    public String XoaGiaoVienCuaBan(@PathVariable String id, ModelMap model, HttpSession session) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher == null) {
            model.addAttribute("error", "Không tìm thấy giáo viên cần xóa!");
            return "DanhSachGiaoVienCuaBan";
        }

        // Xóa giáo viên sau khi đã xóa dữ liệu liên quan
        entityManager.remove(teacher);

        return "redirect:/DanhSachGiaoVienCuaBan";
    }

    @GetMapping("/XoaHocSinhCuaBan/{id}")
    @Transactional
    public String XoaHocSinhCuaBan(@PathVariable String id, ModelMap model, HttpSession session) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        Students student = entityManager.find(Students.class, id);
        if (student == null) {
            model.addAttribute("error", "Không tìm thấy học sinh cần xóa!");
            return "DanhSachHocSinhCuaBan";
        }

        // Xóa tin nhắn liên quan trước
        entityManager.createQuery("DELETE FROM Messages m WHERE m.recipient.id = :studentID OR m.sender.id = :studentID")
                .setParameter("studentID", id)
                .executeUpdate();

        // Sau đó mới xóa học sinh
        entityManager.remove(student);

        return "redirect:/DanhSachHocSinhCuaBan";
    }


    @GetMapping("/SuaGiaoVienCuaBan/{id}")
    public String SuaGiaoVienCuaBan(ModelMap model, @PathVariable("id") String id, HttpSession session) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        Teachers teachers=entityManager.find(Teachers.class, id);
        model.addAttribute("teachers", teachers);
        return "SuaGiaoVienCuaBan";
    }
    @GetMapping("/SuaHocSinhCuaBan/{id}")
    public String SuaHocSinhCuaBan(ModelMap model, @PathVariable("id") String id, HttpSession session) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        Students students=entityManager.find(Students.class, id);
        model.addAttribute("students", students);
        return "SuaHocSinhCuaBan";
    }
    @GetMapping("/DanhSachPhongHoc")
    public String DanhSachPhongHoc(ModelMap model, HttpSession session) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Lấy danh sách phòng học offline
        List<Rooms> offlineRooms = entityManager.createQuery("from Rooms", Rooms.class).getResultList();

        // Lấy danh sách phòng học online
        List<OnlineRooms> onlineRooms = entityManager.createQuery("from OnlineRooms", OnlineRooms.class).getResultList();


        model.addAttribute("rooms", offlineRooms);
        model.addAttribute("roomsonline", onlineRooms);
        return "DanhSachPhongHoc";
    }

    @GetMapping("/ThemPhongHoc")
    public String ThemPhongHoc(ModelMap model, HttpSession session) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        return "ThemPhongHoc";
    }
    @GetMapping("/ThemPhongHocOnline")
    public String ThemPhongHocOnline(ModelMap model, HttpSession session) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        return "ThemPhongHocOnline";
    }
    @GetMapping("/SuaPhongHocOffline/{id}")
    public String SuaPhongHoc(ModelMap model, @PathVariable("id") String id, HttpSession session) {
        if(session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }
        Rooms room = entityManager.find(Rooms.class, id);

        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }
        model.addAttribute("room", room);

        return "SuaPhongHoc";
    }
    @GetMapping("/SuaPhongHocOnline/{id}")
    public String SuaPhongHocOnline(@PathVariable("id") String roomId, ModelMap model, HttpSession session) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Tìm phòng học online theo ID
        OnlineRooms room = entityManager.find(OnlineRooms.class, roomId);
        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }

        model.addAttribute("room", room);
        return "SuaPhongHocOnline";  // Trả về trang chỉnh sửa phòng online
    }

    @Transactional
    @GetMapping("/XoaPhongHocOffline/{id}")
    public String XoaPhongHocOffline(@PathVariable("id") String id) {
        // Tìm phòng học theo ID
        Rooms room = entityManager.find(Rooms.class, id);

        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }

        try {
            // Xóa tất cả ClassroomDetails liên quan đến phòng học này
            int deletedDetails = entityManager.createQuery("DELETE FROM ClassroomDetails c WHERE c.room = :room")
                    .setParameter("room", room)
                    .executeUpdate();
            System.out.println("Deleted " + deletedDetails + " ClassroomDetails records.");

            // Xóa tất cả ScheduleNotifications liên quan đến phòng học này
            int deletedNotifications = entityManager.createQuery("DELETE FROM ScheduleNotifications s WHERE s.room = :room")
                    .setParameter("room", room)
                    .executeUpdate();
            System.out.println("Deleted " + deletedNotifications + " ScheduleNotifications records.");

            // Xóa phòng học
            entityManager.remove(room);

            return "redirect:/DanhSachPhongHoc?success=RoomDeleted";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/DanhSachPhongHoc?error=DeleteFailed";
        }
    }



    @Transactional
    @GetMapping("/XoaPhongHocOnline/{id}")
    public String XoaPhongHocOnline(@PathVariable("id") String id) {
        // Tìm phòng học online theo ID
        OnlineRooms room = entityManager.find(OnlineRooms.class, id);

        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }

        try {
            // Xóa tất cả ClassroomDetails liên quan đến phòng học này
            int deletedDetails = entityManager.createQuery("DELETE FROM ClassroomDetails c WHERE c.room = :room")
                    .setParameter("room", room)
                    .executeUpdate();

            System.out.println("Deleted " + deletedDetails + " ClassroomDetails records.");

            // Xóa tất cả ScheduleNotifications liên quan đến phòng học này
            int deletedNotifications = entityManager.createQuery("DELETE FROM ScheduleNotifications s WHERE s.room = :room")
                    .setParameter("room", room)
                    .executeUpdate();

            System.out.println("Deleted " + deletedNotifications + " ScheduleNotifications records.");

            // Xóa phòng học online
            entityManager.remove(room);

            return "redirect:/DanhSachPhongHoc?success=RoomDeleted";
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/DanhSachPhongHoc?error=DeleteFailed";
        }
    }


    @GetMapping("/BoTriLopHoc")
    public String BoTriLopHoc(ModelMap model, HttpSession session) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Lấy danh sách phòng học offline
        List<Rooms> offlineRooms = entityManager.createQuery("FROM Rooms", Rooms.class).getResultList();

        // Lấy danh sách phòng học online
        List<OnlineRooms> onlineRooms = entityManager.createQuery("FROM OnlineRooms", OnlineRooms.class).getResultList();

        // Đưa từng danh sách vào model
        model.addAttribute("offlineRooms", offlineRooms);
        model.addAttribute("onlineRooms", onlineRooms);

        return "BoTriLopHoc";
    }
    @GetMapping("/ChiTietLopHoc/{id}")
    public String ChiTietLopHoc(ModelMap model, @PathVariable("id") String id, HttpSession session) {
        // Kiểm tra xem EmployeeID có trong session không
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Xác định loại phòng (Online hoặc Offline)
        Room room = entityManager.find(Room.class, id);
        if (room == null) {
            return "redirect:/DanhSachLopHoc?error=notfound";
        }

        model.addAttribute("room", room);

        // Lấy danh sách ClassroomDetails liên kết với phòng học này
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "FROM ClassroomDetails WHERE room = :room", ClassroomDetails.class)
                .setParameter("room", room)
                .getResultList();

        // Danh sách Teachers và Students
        List<Teachers> teachersInClass = new ArrayList<>();
        List<Students> studentsInClass = new ArrayList<>();

        // Phân loại thành viên thành Teachers và Students
        for (ClassroomDetails detail : classroomDetails) {
            Person member = detail.getMember();
            if (member instanceof Teachers) {
                teachersInClass.add((Teachers) member);
            } else if (member instanceof Students) {
                studentsInClass.add((Students) member);
            }
        }

        // Lấy danh sách tất cả giáo viên và học sinh trong hệ thống
        List<Teachers> allTeachers = entityManager.createQuery("FROM Teachers", Teachers.class).getResultList();
        List<Students> allStudents = entityManager.createQuery("FROM Students", Students.class).getResultList();

        // Đưa dữ liệu vào ModelMap để hiển thị trong View
        model.addAttribute("teachersInClass", teachersInClass);
        model.addAttribute("studentsInClass", studentsInClass);
        model.addAttribute("allTeachers", allTeachers);
        model.addAttribute("allStudents", allStudents);
        model.addAttribute("classroomDetails", classroomDetails);

        return "ChiTietLopHoc";
    }


    @Transactional
    @GetMapping("/XoaGiaoVienTrongLop")
    public String XoaGiaoVienTrongLop(@RequestParam String teacherId, @RequestParam String roomId, HttpSession session) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Tìm đối tượng Room theo ID
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            return "redirect:/ChiTietLopHoc/" + roomId + "?error=RoomNotFound";
        }

        // Tìm đối tượng Teacher theo ID
        Person teacher = entityManager.find(Person.class, teacherId);
        if (teacher == null || !(teacher instanceof Teachers)) {
            return "redirect:/ChiTietLopHoc/" + roomId + "?error=TeacherNotFound";
        }

        try {
            int deletedCount = entityManager.createQuery(
                            "DELETE FROM ClassroomDetails WHERE room = :room AND member = :teacher")
                    .setParameter("room", room)  // Truyền object Room thay vì roomId
                    .setParameter("teacher", teacher)  // Truyền object Teacher thay vì teacherId
                    .executeUpdate();

            if (deletedCount == 0) {
                return "redirect:/ChiTietLopHoc/" + roomId + "?error=TeacherNotFound";
            }

            return "redirect:/ChiTietLopHoc/" + roomId + "?success=TeacherRemoved";
        } catch (Exception e) {
            return "redirect:/ChiTietLopHoc/" + roomId + "?error=DeleteFailed";
        }
    }


    @Transactional
    @GetMapping("/XoaHocSinhTrongLop")
    public String XoaHocSinhTrongLop(@RequestParam String studentId, @RequestParam String roomId, HttpSession session) {
        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            return "redirect:/ChiTietLopHoc/" + roomId + "?error=RoomNotFound";
        }

        // Tìm đối tượng Student theo ID
        Person student = entityManager.find(Person.class, studentId);
        if (student == null || !(student instanceof Students)) {
            return "redirect:/ChiTietLopHoc/" + roomId + "?error=StudentNotFound";
        }

        try {
            int deletedCount = entityManager.createQuery(
                            "DELETE FROM ClassroomDetails WHERE room = :room AND member = :student")
                    .setParameter("room", room)  // Dùng object Room thay vì roomId
                    .setParameter("student", student)  // Dùng object Student thay vì studentId
                    .executeUpdate();

            if (deletedCount == 0) {
                return "redirect:/ChiTietLopHoc/" + roomId + "?error=StudentNotFound";
            }

            return "redirect:/ChiTietLopHoc/" + roomId + "?success=StudentRemoved";
        } catch (Exception e) {
            return "redirect:/ChiTietLopHoc/" + roomId + "?error=DeleteFailed";
        }
    }


    @GetMapping("/GuiThongBao/{id}")
    public String GuiThongBao(@PathVariable("id") String id, HttpSession session) {
        // Kiểm tra đăng nhập
        Employees employee = entityManager.find(Employees.class, session.getAttribute("EmployeeID"));
        if (employee == null) {
            return "redirect:/DangNhapNhanVien";
        }

        // Lấy phòng học theo ID
        Room room = entityManager.find(Room.class, id);
        if (room == null) {
            return "redirect:/BoTriLopHoc?error=RoomNotFound";
        }

        // Lấy danh sách thành viên trong lớp
        List<ClassroomDetails> classMembers = entityManager.createQuery(
                        "FROM ClassroomDetails c WHERE c.room = :room", ClassroomDetails.class)
                .setParameter("room", room)
                .getResultList();

        for (ClassroomDetails detail : classMembers) {
            Person member = detail.getMember();
            if (member == null) continue;

            // Xác định email người nhận
            String recipientEmail = null;
            if (member instanceof Students) {
                recipientEmail = ((Students) member).getEmail();
            } else if (member instanceof Teachers) {
                recipientEmail = ((Teachers) member).getEmail();
            }

            if (recipientEmail != null) {
                // Xác định nội dung tin nhắn dựa vào loại phòng học
                String messageContent;
                if (room instanceof OnlineRooms) {
                    OnlineRooms onlineRoom = (OnlineRooms) room;
                    messageContent = "Lịch trình học Online của bạn bắt đầu từ " +
                            onlineRoom.getStartTime() + " đến hết " + onlineRoom.getEndTime();
                } else {
                    messageContent = "Lịch trình học của bạn bắt đầu từ " +
                            room.getStartTime() + " đến hết " + room.getEndTime();
                }

                // Gửi thông báo
                sendNotification(member.getId(), room.getRoomId(), messageContent, employee, recipientEmail);
            }
        }

        return "redirect:/BoTriLopHoc";
    }


    private void sendNotification(String memberId, String roomId, String message, Employees sender, String email) {
        // Tìm đối tượng Person từ memberId
        Person member = entityManager.find(Person.class, memberId);
        if (member == null) {
            throw new IllegalArgumentException("Không tìm thấy thành viên với ID: " + memberId);
        }

        // Tìm đối tượng Room từ roomId
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId);
        }

        // Tạo thông báo mới
        ScheduleNotifications scheduleNotifications = new ScheduleNotifications();
        scheduleNotifications.setMember(member);
        scheduleNotifications.setRoom(room);
        scheduleNotifications.setMessage(message);
        scheduleNotifications.setSender(sender);

        // Lưu vào database
        entityManager.persist(scheduleNotifications);

        // Gửi email
        sendEmail(email, "Thông báo lịch trình học", message);
    }


    private void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    @GetMapping("/BangDieuKhienNhanVien/{id}")
    public String BangDieuKhienNhanVien(@PathVariable("id") String id, ModelMap model) {
        Employees employee = entityManager.find(Employees.class, id);

        List<Room> room= entityManager.createQuery("from Room r where r.employee=:employee", Room.class).
                setParameter("employee", employee).getResultList();

        List<Teachers> teachers=entityManager.createQuery("from Teachers t where t.employee=:employee", Teachers.class).
                setParameter("employee", employee).getResultList();

        List<Students> students=entityManager.createQuery("from Students s where s.employee=:employee", Students.class).
                setParameter("employee", employee).getResultList();
        model.addAttribute("room", room);
        model.addAttribute("teachers", teachers);
        model.addAttribute("students", students);
        model.addAttribute("employee", employee);
        return "BangDieuKhienNhanVien";
    }
    @GetMapping("/BangDieuKhienHocSinh/{id}")
    public String BangDieuKhienHocSinh(@PathVariable("id") String id, ModelMap model) {
        // Find the student using the provided ID
        Students student = entityManager.find(Students.class, id);
        if (student == null) {
            model.addAttribute("errorMessage", "Student not found");
            return "errorPage"; // Return a custom error page
        }

        // Ensure the employee is fetched correctly using the student's employee ID
        Employees employee = student.getEmployee();  // This should already be an Employees object
        if (employee == null) {
            model.addAttribute("errorMessage", "No employee associated with the student.");
            return "errorPage";  // Return an error page view
        }

        // Retrieve classroom details for the student
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "from ClassroomDetails cd where cd.member = :student", ClassroomDetails.class)
                .setParameter("student", student)
                .getResultList();

        Set<Room> roomSet = new HashSet<>();
        Set<Teachers> teachers = new HashSet<>();
        Set<Rooms> rooms = new HashSet<>();
        Set<OnlineRooms> onlineRooms = new HashSet<>();

        // Process each classroom detail to group rooms and teachers
        for (ClassroomDetails classroomDetail : classroomDetails) {
            roomSet.add(classroomDetail.getRoom());
        }

        // Process the rooms and classify them as either physical rooms or online rooms
        // Process the rooms and classify them as either physical rooms or online rooms
        for (Room room1 : roomSet) {
            List<ClassroomDetails> classroomDetailsForRoom = entityManager.createQuery(
                            "from ClassroomDetails cd where cd.room = :room1", ClassroomDetails.class)
                    .setParameter("room1", room1)
                    .getResultList();

            for (ClassroomDetails classroomDetail : classroomDetailsForRoom) {
                Object member = classroomDetail.getMember();

                // Check if the member is a teacher and cast accordingly
                if (member instanceof Teachers) {
                    teachers.add((Teachers) member);
                }
            }

            if (room1 instanceof Rooms) {
                rooms.add((Rooms) room1);
            } else if (room1 instanceof OnlineRooms) {
                onlineRooms.add((OnlineRooms) room1);
            }
        }


        // Add necessary attributes to the model for the view
        model.addAttribute("employee", employee);
        model.addAttribute("rooms", rooms);
        model.addAttribute("onlineRooms", onlineRooms);
        model.addAttribute("teachers", teachers);
        model.addAttribute("student", student);

        return "BangDieuKhienHocSinh"; // Ensure this matches the actual view file name
    }
    @GetMapping("/BangDieuKhienGiaoVien/{id}")
    public String BangDieuKhienGiaoVien(@PathVariable("id") String id, ModelMap model) {
        // Find the teacher using the provided ID
        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher == null) {
            model.addAttribute("errorMessage", "Teacher not found");
            return "errorPage"; // Return a custom error page if teacher is not found
        }

        // Ensure the employee is fetched correctly using the teacher's employee ID
        Employees employee = teacher.getEmployee();  // This should already be an Employees object
        if (employee == null) {
            model.addAttribute("errorMessage", "No employee associated with the teacher.");
            return "errorPage";  // Return an error page if employee is not associated
        }

        // Retrieve classroom details for the teacher
        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "from ClassroomDetails cd where cd.member = :teacher", ClassroomDetails.class)
                .setParameter("teacher", teacher)
                .getResultList();

        Set<Room> roomSet = new HashSet<>();
        Set<Students> students = new HashSet<>();
        Set<Rooms> rooms = new HashSet<>();
        Set<OnlineRooms> onlineRooms = new HashSet<>();

        // Process each classroom detail to group rooms and students
        for (ClassroomDetails classroomDetail : classroomDetails) {
            roomSet.add(classroomDetail.getRoom());

            // Check if the member is a Student before adding to the students set
            Object member = classroomDetail.getMember();
            if (member instanceof Students) {
                students.add((Students) member);  // Add the student to the set if the member is a student
            }
        }

        // Process the rooms and classify them as either physical rooms or online rooms
        for (Room room1 : roomSet) {
            List<ClassroomDetails> classroomDetailsForRoom = entityManager.createQuery(
                            "from ClassroomDetails cd where cd.room = :room1", ClassroomDetails.class)
                    .setParameter("room1", room1)
                    .getResultList();

            for (ClassroomDetails classroomDetail : classroomDetailsForRoom) {
                Object member = classroomDetail.getMember();

                // Check if the member is a student and add them to the students list
                if (member instanceof Students) {
                    students.add((Students) member);
                }
            }

            // Classify rooms as either physical rooms or online rooms
            if (room1 instanceof Rooms) {
                rooms.add((Rooms) room1);
            } else if (room1 instanceof OnlineRooms) {
                onlineRooms.add((OnlineRooms) room1);
            }
        }

        // Add necessary attributes to the model for the view
        model.addAttribute("employee", employee);
        model.addAttribute("rooms", rooms);
        model.addAttribute("onlineRooms", onlineRooms);
        model.addAttribute("students", students); // Include the list of students for the teacher
        model.addAttribute("teacher", teacher); // Include teacher details

        return "BangDieuKhienGiaoVien"; // Ensure this matches the actual view file name
    }


}
