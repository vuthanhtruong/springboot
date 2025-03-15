package com.example.demo.GET;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // AdminID đã đăng nhập

        // Tìm Admin trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

        model.addAttribute("employee", employee);
        return "TrangChuNhanVien";
    }

    @GetMapping("/TrangCaNhanNhanVien")
    public String TrangCaNhanNhanVien(HttpSession session, ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // AdminID đã đăng nhập

        // Tìm Admin trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);
        model.addAttribute("employee", employee);
        return "TrangCaNhanNhanVien";
    }

    @GetMapping("/DanhSachGiaoVienCuaBan")
    public String DanhSachGiaoVienCuaBan(
            ModelMap model,
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSizeParam
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // AdminID đã đăng nhập

        // Tìm Admin trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

        // Lấy pageSize từ session nếu chưa có thì mặc định 5
        Integer pageSize = (Integer) session.getAttribute("pageSize2");
        if (pageSizeParam != null) {
            pageSize = pageSizeParam;
            session.setAttribute("pageSize2", pageSize);
        }
        if (pageSize == null || pageSize < 1) pageSize = 5;

        // Đếm tổng số giáo viên
        Long totalTeachers = (Long) entityManager.createQuery(
                        "SELECT COUNT(t) FROM Teachers t WHERE t.employee.id = :employeeId")
                .setParameter("employeeId", employee.getId())
                .getSingleResult();

        // Tránh lỗi chia cho 0
        if (totalTeachers == 0) {
            model.addAttribute("teachers", new ArrayList<>());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 1);
            model.addAttribute("pageSize", pageSize);
            return "DanhSachGiaoVienCuaBan";
        }

        // Tính số trang hợp lệ
        int totalPages = (int) Math.ceil((double) totalTeachers / pageSize);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        // Vị trí bắt đầu
        int firstResult = (page - 1) * pageSize;

        // Lấy danh sách giáo viên có sắp xếp theo ID
        List<Teachers> teachers = entityManager.createQuery(
                        "FROM Teachers t WHERE t.employee.id = :employeeId ORDER BY t.id ASC", Teachers.class)
                .setParameter("employeeId", employee.getId())
                .setFirstResult(firstResult)
                .setMaxResults(pageSize)
                .getResultList();
        // Gửi dữ liệu sang view
        model.addAttribute("teachers", teachers);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", pageSize);

        return "DanhSachGiaoVienCuaBan";
    }


    @GetMapping("/ThemGiaoVienCuaBan")
    public String ThemGiaoVienCuaBan(HttpSession session, ModelMap model) {
        return "ThemGiaoVienCuaBan";
    }

    @GetMapping("/DanhSachHocSinhCuaBan")
    public String DanhSachHocSinhCuaBan(
            ModelMap model,
            HttpSession session,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) Integer pageSize // Cho phép null
    ) {
        // Lấy tên người dùng từ SecurityContext (sử dụng thông tin đã đăng nhập)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // EmployeeID đã đăng nhập

        // Tìm Employee trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

        // Nếu pageSize là null, lấy từ session hoặc đặt mặc định là 5
        if (pageSize == null) {
            pageSize = (Integer) session.getAttribute("pageSize");
            if (pageSize == null) {
                pageSize = 5; // Mặc định 5 nếu chưa có
            }
        }

        // Lưu pageSize vào session để dùng trong lần sau
        session.setAttribute("pageSize", pageSize);

        // Đếm tổng số học sinh thuộc nhân viên hiện tại
        Long totalStudents = (Long) entityManager.createQuery(
                        "SELECT COUNT(s) FROM Students s WHERE s.employee.id = :employeeId")
                .setParameter("employeeId", employee.getId())
                .getSingleResult();

        // Tránh lỗi khi không có học sinh
        if (totalStudents == 0) {
            model.addAttribute("students", new ArrayList<>());
            model.addAttribute("currentPage", 1);
            model.addAttribute("totalPages", 1);
            model.addAttribute("pageSize", pageSize);
            return "DanhSachHocSinhCuaBan";
        }

        // Tính tổng số trang
        int totalPages = (int) Math.ceil((double) totalStudents / pageSize);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        // Tính vị trí bắt đầu lấy dữ liệu (setFirstResult)
        int firstResult = (page - 1) * pageSize;

        // Lấy danh sách học sinh có phân trang
        List<Students> students = entityManager.createQuery(
                        "FROM Students s WHERE s.employee.id = :employeeId ORDER BY s.id ASC", Students.class)
                .setParameter("employeeId", employee.getId()) // Sử dụng employee.getId() thay vì session.getAttribute("EmployeeID")
                .setFirstResult(firstResult) // Thiết lập vị trí bắt đầu
                .setMaxResults(pageSize) // Thiết lập số lượng kết quả tối đa
                .getResultList();

        // Đưa dữ liệu lên giao diện
        model.addAttribute("students", students);
        model.addAttribute("currentPage", page); // Trang hiện tại
        model.addAttribute("totalPages", totalPages); // Tổng số trang
        model.addAttribute("pageSize", pageSize); // Kích thước trang

        return "DanhSachHocSinhCuaBan";
    }


    @GetMapping("/ThemHocSinhCuaBan")
    public String ThemHocSinhCuaBan(ModelMap model, HttpSession session) {

        return "ThemHocSinhCuaBan";
    }

    @GetMapping("/DanhSachNguoiDungHeThong")
    public String DanhSachNguoiDungHeThong(
            HttpSession session,
            ModelMap model,
            @RequestParam(defaultValue = "1") int pageEmployees,
            @RequestParam(defaultValue = "1") int pageTeachers,
            @RequestParam(defaultValue = "1") int pageStudents,
            @RequestParam(required = false) Integer pageSize // Cho phép null
    ) {
        if (pageSize == null) {
            pageSize = (Integer) session.getAttribute("pageSize4");
            if (pageSize == null) {
                pageSize = 5; // Mặc định là 5 nếu chưa có
            }
        }
        session.setAttribute("pageSize4", pageSize); // Lưu pageSize vào session

        // ========== PHÂN TRANG CHO EMPLOYEES ==========
        Long totalEmployees = (Long) entityManager.createQuery("SELECT COUNT(e) FROM Employees e")
                .getSingleResult();
        int totalPagesEmployees = (int) Math.ceil((double) totalEmployees / pageSize);
        totalPagesEmployees = Math.max(totalPagesEmployees, 1);
        pageEmployees = Math.max(1, Math.min(pageEmployees, totalPagesEmployees));

        int firstEmployeeResult = (pageEmployees - 1) * pageSize;
        List<Employees> employeeList = entityManager.createQuery("FROM Employees", Employees.class)
                .setFirstResult(firstEmployeeResult)
                .setMaxResults(pageSize)
                .getResultList();

        // ========== PHÂN TRANG CHO TEACHERS ==========
        Long totalTeachers = (Long) entityManager.createQuery("SELECT COUNT(t) FROM Teachers t")
                .getSingleResult();
        int totalPagesTeachers = (int) Math.ceil((double) totalTeachers / pageSize);
        totalPagesTeachers = Math.max(totalPagesTeachers, 1);
        pageTeachers = Math.max(1, Math.min(pageTeachers, totalPagesTeachers));

        int firstTeacherResult = (pageTeachers - 1) * pageSize;
        List<Teachers> teacherList = entityManager.createQuery("FROM Teachers", Teachers.class)
                .setFirstResult(firstTeacherResult)
                .setMaxResults(pageSize)
                .getResultList();

        // ========== PHÂN TRANG CHO STUDENTS ==========
        Long totalStudents = (Long) entityManager.createQuery("SELECT COUNT(s) FROM Students s")
                .getSingleResult();
        int totalPagesStudents = (int) Math.ceil((double) totalStudents / pageSize);
        totalPagesStudents = Math.max(totalPagesStudents, 1);
        pageStudents = Math.max(1, Math.min(pageStudents, totalPagesStudents));

        int firstStudentResult = (pageStudents - 1) * pageSize;
        List<Students> studentList = entityManager.createQuery("FROM Students", Students.class)
                .setFirstResult(firstStudentResult)
                .setMaxResults(pageSize)
                .getResultList();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // AdminID đã đăng nhập

        // Tìm Admin trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

        // Lấy thông tin admin từ EmployeeID
        Employees employee1 = entityManager.find(Employees.class, employee.getId());
        Admin admin = employee1.getAdmin();

        // ========== ĐƯA DỮ LIỆU LÊN GIAO DIỆN ==========
        model.addAttribute("employees", employeeList);
        model.addAttribute("teachers", teacherList);
        model.addAttribute("students", studentList);
        model.addAttribute("admin", admin);

        // Thông tin phân trang
        model.addAttribute("currentPageEmployees", pageEmployees);
        model.addAttribute("totalPagesEmployees", totalPagesEmployees);

        model.addAttribute("currentPageTeachers", pageTeachers);
        model.addAttribute("totalPagesTeachers", totalPagesTeachers);

        model.addAttribute("currentPageStudents", pageStudents);
        model.addAttribute("totalPagesStudents", totalPagesStudents);

        model.addAttribute("pageSize", pageSize);

        return "DanhSachNguoiDungHeThong";
    }

    @GetMapping("/XoaGiaoVienCuaBan/{id}")
    @Transactional
    public String XoaGiaoVienCuaBan(@PathVariable String id, ModelMap model, HttpSession session) {

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
        Teachers teachers = entityManager.find(Teachers.class, id);
        model.addAttribute("teachers", teachers);
        return "SuaGiaoVienCuaBan";
    }

    @GetMapping("/SuaHocSinhCuaBan/{id}")
    public String SuaHocSinhCuaBan(ModelMap model, @PathVariable("id") String id, HttpSession session) {
        Students students = entityManager.find(Students.class, id);
        model.addAttribute("student", students);
        return "SuaHocSinhCuaBan";
    }

    @GetMapping("/DanhSachPhongHoc")
    public String DanhSachPhongHoc(
            ModelMap model,
            HttpSession session,
            @RequestParam(defaultValue = "1") int pageOffline,
            @RequestParam(defaultValue = "1") int pageOnline,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortOrder
    ) {
        // Xử lý pageSize
        if (pageSize == null) {
            pageSize = (Integer) session.getAttribute("pageSize3");
            if (pageSize == null) {
                pageSize = 5; // Mặc định 5 nếu chưa có
            }
        }
        session.setAttribute("pageSize3", pageSize);

        // ====== XỬ LÝ PHÂN TRANG CHO PHÒNG HỌC OFFLINE ======
        Long totalOfflineRooms = (Long) entityManager.createQuery("SELECT COUNT(r) FROM Rooms r")
                .getSingleResult();
        int totalOfflinePages = Math.max(1, (int) Math.ceil((double) totalOfflineRooms / pageSize));
        pageOffline = Math.max(1, Math.min(pageOffline, totalOfflinePages));

        int firstOfflineResult = (pageOffline - 1) * pageSize;
        String offlineQuery = "FROM Rooms r" + (sortOrder != null ? " ORDER BY r.createdAt " + ("asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC") : "");
        List<Rooms> offlineRooms = entityManager.createQuery(offlineQuery, Rooms.class)
                .setFirstResult(firstOfflineResult)
                .setMaxResults(pageSize)
                .getResultList();

        // ====== XỬ LÝ PHÂN TRANG CHO PHÒNG HỌC ONLINE ======
        Long totalOnlineRooms = (Long) entityManager.createQuery("SELECT COUNT(r) FROM OnlineRooms r")
                .getSingleResult();
        int totalOnlinePages = Math.max(1, (int) Math.ceil((double) totalOnlineRooms / pageSize));
        pageOnline = Math.max(1, Math.min(pageOnline, totalOnlinePages));

        int firstOnlineResult = (pageOnline - 1) * pageSize;
        String onlineQuery = "FROM OnlineRooms r" + (sortOrder != null ? " ORDER BY r.createdAt " + ("asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC") : "");
        List<OnlineRooms> onlineRooms = entityManager.createQuery(onlineQuery, OnlineRooms.class)
                .setFirstResult(firstOnlineResult)
                .setMaxResults(pageSize)
                .getResultList();

        // ====== ĐƯA DỮ LIỆU LÊN GIAO DIỆN ======
        model.addAttribute("rooms", offlineRooms);
        model.addAttribute("roomsonline", onlineRooms);

        // Thông tin phân trang
        model.addAttribute("currentPageOffline", pageOffline);
        model.addAttribute("totalPagesOffline", totalOfflinePages);
        model.addAttribute("currentPageOnline", pageOnline);
        model.addAttribute("totalPagesOnline", totalOnlinePages);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("sortOrder", sortOrder);

        return "DanhSachPhongHoc";
    }


    @GetMapping("/ThemPhongHoc")
    public String ThemPhongHoc(ModelMap model, HttpSession session) {

        return "ThemPhongHoc";
    }

    @GetMapping("/ThemPhongHocOnline")
    public String ThemPhongHocOnline(ModelMap model, HttpSession session) {

        return "ThemPhongHocOnline";
    }

    @GetMapping("/SuaPhongHocOffline/{id}")
    public String SuaPhongHoc(ModelMap model, @PathVariable("id") String id, HttpSession session) {

        Rooms room = entityManager.find(Rooms.class, id);

        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }
        model.addAttribute("room", room);

        return "SuaPhongHoc";
    }

    @GetMapping("/SuaPhongHocOnline/{id}")
    public String SuaPhongHocOnline(@PathVariable("id") String roomId, ModelMap model, HttpSession session) {

        OnlineRooms room = entityManager.find(OnlineRooms.class, roomId);
        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }

        model.addAttribute("room", room);
        return "SuaPhongHocOnline";  // Trả về trang chỉnh sửa phòng online
    }

    @Transactional
    @GetMapping("/XoaPhongHoc/{id}")
    public String XoaPhongHoc(@PathVariable("id") String id) {
        // Tìm phòng học offline trước
        Object room = entityManager.find(Rooms.class, id);

        // Nếu không tìm thấy trong Rooms, thử tìm trong OnlineRooms
        if (room == null) {
            room = entityManager.find(OnlineRooms.class, id);
        }

        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }

        try {
            // Xóa documents liên quan đến posts trong phòng học này
            entityManager.createQuery("DELETE FROM Documents d WHERE d.post IN (SELECT p FROM Posts p WHERE p.room = :room)")
                    .setParameter("room", room)
                    .executeUpdate();

            // Xóa posts liên quan đến phòng học này
            entityManager.createQuery("DELETE FROM Posts p WHERE p.room = :room")
                    .setParameter("room", room)
                    .executeUpdate();

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

    @GetMapping("/BoTriLopHoc")
    public String BoTriLopHoc(ModelMap model, HttpSession session) {

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // AdminID đã đăng nhập

        // Tìm Admin trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

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
                recipientEmail = member.getEmail();
            } else if (member instanceof Teachers) {
                recipientEmail = member.getEmail();
            }

            if (recipientEmail != null) {
                // Xác định nội dung tin nhắn dựa vào loại phòng học
                String messageContent;
                if (room instanceof OnlineRooms onlineRoom) {
                    messageContent = "Lịch trình học Online của bạn bắt đầu từ " +
                            onlineRoom.getStartTime() + " đến hết " + onlineRoom.getEndTime() + "tại đường Link " + onlineRoom.getLink();
                } else {
                    Rooms rooms = (Rooms) room;
                    messageContent = "Lịch trình học của bạn bắt đầu từ " +
                            room.getStartTime() + " đến hết " + room.getEndTime() + "tại địa chỉ " + rooms.getAddress();
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

        List<Room> room = entityManager.createQuery("from Room r where r.employee=:employee", Room.class).
                setParameter("employee", employee).getResultList();

        List<Teachers> teachers = entityManager.createQuery("from Teachers t where t.employee=:employee", Teachers.class).
                setParameter("employee", employee).getResultList();

        List<Students> students = entityManager.createQuery("from Students s where s.employee=:employee", Students.class).
                setParameter("employee", employee).getResultList();
        model.addAttribute("room", room);
        model.addAttribute("teachers", teachers);
        model.addAttribute("students", students);
        model.addAttribute("employee", employee);
        return "BangDieuKhienNhanVien";
    }

    @GetMapping("/BangDieuKhienHocSinh/{id}")
    public String BangDieuKhienHocSinh(@PathVariable("id") String id, ModelMap model, HttpSession session) {

        Students student = entityManager.find(Students.class, id);
        if (student == null) {
            model.addAttribute("errorMessage", "Student not found");
            return "errorPage";
        }

        Employees employee = student.getEmployee();
        if (employee == null) {
            model.addAttribute("errorMessage", "No employee associated with the student.");
            return "errorPage";
        }

        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "from ClassroomDetails cd where cd.member = :student", ClassroomDetails.class)
                .setParameter("student", student)
                .getResultList();

        Set<Room> roomSet = new HashSet<>();
        Set<Teachers> teachers = new HashSet<>();
        Set<Rooms> rooms = new HashSet<>();
        Set<OnlineRooms> onlineRooms = new HashSet<>();


        for (ClassroomDetails classroomDetail : classroomDetails) {
            roomSet.add(classroomDetail.getRoom());
        }
        for (Room room1 : roomSet) {
            List<ClassroomDetails> classroomDetailsForRoom = entityManager.createQuery(
                            "from ClassroomDetails cd where cd.room = :room1", ClassroomDetails.class)
                    .setParameter("room1", room1)
                    .getResultList();

            for (ClassroomDetails classroomDetail : classroomDetailsForRoom) {
                Object member = classroomDetail.getMember();


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

        model.addAttribute("employee", employee);
        model.addAttribute("rooms", rooms);
        model.addAttribute("onlineRooms", onlineRooms);
        model.addAttribute("teachers", teachers);
        model.addAttribute("student", student);

        return "BangDieuKhienHocSinh";
    }

    @GetMapping("/BangDieuKhienGiaoVien/{id}")
    public String BangDieuKhienGiaoVien(@PathVariable("id") String id, ModelMap model, HttpSession session) {

        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher == null) {
            model.addAttribute("errorMessage", "Teacher not found");
            return "errorPage";
        }

        Employees employee = teacher.getEmployee();
        if (employee == null) {
            model.addAttribute("errorMessage", "No employee associated with the teacher.");
            return "errorPage";
        }


        List<ClassroomDetails> classroomDetails = entityManager.createQuery(
                        "from ClassroomDetails cd where cd.member = :teacher", ClassroomDetails.class)
                .setParameter("teacher", teacher)
                .getResultList();

        Set<Room> roomSet = new HashSet<>();
        Set<Students> students = new HashSet<>();
        Set<Rooms> rooms = new HashSet<>();
        Set<OnlineRooms> onlineRooms = new HashSet<>();

        for (ClassroomDetails classroomDetail : classroomDetails) {
            roomSet.add(classroomDetail.getRoom());
        }

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

    @GetMapping("/FeedbackHocSinh")
    public String FeedbackHocSinh(ModelMap model, HttpSession session) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // AdminID đã đăng nhập

        // Tìm Admin trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

        List<Feedbacks> feedbacks = entityManager.createQuery("from Feedbacks f where f.receiver=:receiver", Feedbacks.class).
                setParameter("receiver", employee).getResultList();

        model.addAttribute("feedbacks", feedbacks);

        return "FeedbackHocSinh";
    }

    @Transactional
    @GetMapping("/XoaTatCaPhongHocOffline")
    public String deleteAllOfflineRooms() {
        entityManager.createQuery("DELETE FROM Rooms").executeUpdate();
        return "redirect:/DanhSachPhongHoc"; // Chuyển hướng về danh sách phòng
    }

    /**
     * 🔹 Xóa tất cả phòng học online (GET)
     */
    @Transactional
    @GetMapping("/XoaTatCaPhongHocOnline")
    public String deleteAllOnlineRooms() {
        entityManager.createQuery("DELETE FROM OnlineRooms").executeUpdate();
        return "redirect:/DanhSachPhongHoc"; // Chuyển hướng về danh sách phòng
    }

    /**
     * 🔹 Xóa tất cả phòng học (Cả online & offline) (GET)
     */
    @Transactional
    @GetMapping("/XoaTatCaPhongHoc")
    public String deleteAllRooms() {
        entityManager.createQuery("DELETE FROM Room").executeUpdate();
        return "redirect:/DanhSachPhongHoc"; // Chuyển hướng về danh sách phòng
    }
}
