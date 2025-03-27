package com.example.demo.ControllerGET;

import com.example.demo.ModelOOP.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
        // Xóa tin nhắn liên quan trước
        entityManager.createQuery("DELETE FROM Messages m WHERE m.recipient.id = :teacherID OR m.sender.id = :teacherID")
                .setParameter("teacherID", id)
                .executeUpdate();

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
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // EmployeeID đã đăng nhập

        // Tìm Employee trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

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


            String subject = "Thông Báo Quan Trọng Về Sự Điều Chỉnh Thành Viên Lớp Học - Một Hành Trình Mới";

            String message = "<html><body style='font-family: Georgia, serif; line-height: 1.8; color: #333333; max-width: 700px; margin: 0 auto; background-color: #F9F9F9; padding: 20px;'>" +
                    "<p style='font-size: 18px; color: #1A3C5E;'><b>Kính thưa " + teacher.getFullName() + ", người đồng hành đáng kính,</b></p>" +
                    "<p style='color: #4A4A4A;'>Trước hết, cho phép chúng tôi được gửi tới Thầy/Cô những lời chào trân trọng nhất, những lời tri ân sâu sắc nhất, như một khúc nhạc mở đầu cho bản giao hưởng của sự biết ơn. Chính sự hiện diện của Thầy/Cô trong hành trình giáo dục của chúng tôi đã tô điểm thêm những gam màu rực rỡ, những giá trị cao quý mà không ngôn từ nào có thể diễn tả trọn vẹn. Chúng tôi luôn trân quý từng khoảnh khắc Thầy/Cô đồng hành, từng nỗ lực Thầy/Cô đã dành cho hệ thống quản lý lớp học của xAI Education.</p>" +
                    "<p style='color: #4A4A4A;'>Hôm nay, với tất cả sự kính cẩn và chân thành, chúng tôi xin được phép chia sẻ một cập nhật quan trọng – một bước ngoặt nhỏ trong hành trình dài mà chúng ta cùng nhau vun đắp. Theo định hướng chiến lược và kế hoạch tối ưu hóa hệ thống, tài khoản của Thầy/Cô, với sự cân nhắc kỹ lưỡng, đã được điều chỉnh để không còn nằm trong danh sách thành viên của lớp học <i style='color: #D35400;'>" + room.getRoomName() + "</i>. Đây không phải là một sự kết thúc, mà là một chương mới, một sự thay đổi mang tính xây dựng, nhằm đảm bảo rằng mọi mảnh ghép trong bức tranh giáo dục đều được đặt đúng chỗ, đúng thời điểm, vì lợi ích lớn lao của tất cả chúng ta.</p>" +
                    "<p style='color: #4A4A4A;'>Để Thầy/Cô có thể hình dung rõ hơn về sự thay đổi này, chúng tôi xin được trình bày đôi nét thông tin chi tiết, như những dòng chữ được khắc trên tấm bia kỷ niệm của một chặng đường:</p>" +
                    "<ul style='list-style-type: none; padding-left: 20px; color: #4A4A4A;'>" +
                    "   <li><b style='color: #2E7D32;'>✦ Mã lớp học:</b> " + roomId + "</li>" +
                    "   <li><b style='color: #2E7D32;'>✦ Tên lớp học:</b> " + room.getRoomName() + "</li>" +
                    "   <li><b style='color: #2E7D32;'>✦ Thời gian cập nhật:</b> " + new java.util.Date() + "</li>" +
                    "   <li><b style='color: #2E7D32;'>✦ Nguyên nhân sâu xa:</b> Một nỗ lực không ngừng nghỉ để tái cấu trúc và tối ưu hóa hệ thống, hướng tới mục tiêu cao cả của sự hài hòa và hiệu quả.</li>" +
                    "</ul>" +
                    "<p style='color: #4A4A4A;'>Chúng tôi hiểu rằng, mỗi sự thay đổi, dù nhỏ bé đến đâu, cũng có thể gợi lên những cảm xúc, những câu hỏi trong lòng người nhận tin. Vì vậy, chúng tôi xin được bày tỏ sự thấu hiểu và mong mỏi rằng Thầy/Cô sẽ đón nhận điều này với sự bao dung và thông cảm – những phẩm chất cao đẹp mà chúng tôi luôn trân trọng ở Thầy/Cô. Nếu có bất kỳ điều gì khiến Thầy/Cô trăn trở, hay chỉ đơn giản là một mong muốn được lắng nghe, xin đừng ngần ngại kết nối với chúng tôi qua <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none; font-weight: bold;'>vuthanhtruong1280@gmail.com</a> hoặc đường dây nóng <b style='color: #C0392B;'>0394444107</b>. Đội ngũ của chúng tôi, với tất cả sự tận tâm, luôn sẵn sàng đồng hành cùng Thầy/Cô, như những người bạn chân thành trên con đường tri thức.</p>" +
                    "<p style='color: #4A4A4A;'>Thưa Thầy/Cô, hành trình của chúng ta không dừng lại ở đây. Sự điều chỉnh này chỉ là một nốt trầm nhẹ trong bản nhạc dài của sự hợp tác. Chúng tôi tin tưởng rằng, với tài năng, tâm huyết và lòng nhiệt thành của Thầy/Cô, cánh cửa của những cơ hội mới sẽ sớm rộng mở, và chúng tôi hy vọng sẽ tiếp tục được đồng hành cùng Thầy/Cô trong những chương tiếp theo của câu chuyện giáo dục đầy cảm hứng này.</p>" +
                    "<p style='color: #4A4A4A;'>Trước khi khép lại lá thư này, cho phép chúng tôi được gửi tới Thầy/Cô lời chúc tốt đẹp nhất – không chỉ là sức khỏe dồi dào, thành công rực rỡ, mà còn là niềm vui bất tận, sự an nhiên trong tâm hồn, và những ngày tháng tràn đầy ý nghĩa. Thầy/Cô không chỉ là một người giáo viên, mà còn là một ngọn hải đăng soi sáng hành trình của chúng tôi.</p>" +
                    "<p style='margin-top: 30px; text-align: center; color: #7F8C8D;'><i>Trân trọng kính thư,</i></p>" +
                    "<p style='text-align: center; color: #34495E;'>" +
                    "<b>" + employee.getFirstName() + " " + employee.getLastName() + "</b><br>" +
                    "Quản Trị Viên Hệ Thống<br>" +
                    "Hệ Thống Quản Lý Giáo Dục - xAI Education<br>" +
                    "Email: <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none;'>support@xaiedu.com</a> | Hotline: <span style='color: #C0392B;'>0394444107</span></p>" +
                    "</body></html>";

            sendEmail(teacher.getEmail(), subject, message);

            return "redirect:/ChiTietLopHoc/" + roomId + "?success=TeacherRemoved";
        } catch (Exception e) {
            return "redirect:/ChiTietLopHoc/" + roomId + "?error=DeleteFailed";
        }
    }


    @Transactional
    @GetMapping("/XoaHocSinhTrongLop")
    public String XoaHocSinhTrongLop(@RequestParam String studentId, @RequestParam String roomId, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // EmployeeID đã đăng nhập

        // Tìm Employee trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);

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

            String subject = "Thông Báo Quan Trọng Về Hành Trình Học Tập - Một Chương Mới Dành Cho Bạn";

            String message = "<html><body style='font-family: Georgia, serif; line-height: 1.8; color: #333333; max-width: 700px; margin: 0 auto; background-color: #F5F6F5; padding: 20px;'>" +
                    "<p style='font-size: 18px; color: #154360;'><b>Thân gửi " + student.getFullName() + ", người bạn đồng hành đáng quý,</b></p>" +
                    "<p style='color: #4A4A4A;'>Trước tiên, cho phép chúng tôi gửi tới bạn một lời chào nồng ấm, như ánh nắng ban mai khẽ chạm vào những giấc mơ tươi đẹp của tuổi trẻ. Hành trình học tập của bạn tại Hệ Thống Quản Lý Giáo Dục - xAI Education luôn là một câu chuyện đầy cảm hứng, nơi mỗi bước chân của bạn đều góp phần viết nên những trang sử rực rỡ cho chính bản thân và cộng đồng.</p>" +
                    "<p style='color: #4A4A4A;'>Hôm nay, với tất cả sự trân trọng và niềm tin vào tương lai của bạn, chúng tôi xin được chia sẻ một cập nhật nhỏ nhưng mang ý nghĩa lớn lao. Theo kế hoạch tối ưu hóa và định hướng chiến lược của hệ thống, tài khoản của bạn đã được điều chỉnh để tạm thời không còn nằm trong danh sách thành viên của lớp học <i style='color: #D35400;'>" + room.getRoomName() + "</i>. Đây không phải là dấu chấm hết, mà là một dấu phẩy – một khoảnh khắc dừng lại để mở ra cánh cửa cho những cơ hội mới, những trải nghiệm mới, phù hợp hơn với hành trình phía trước của bạn.</p>" +
                    "<p style='color: #4A4A4A;'>Để bạn có thể hình dung rõ hơn về sự thay đổi này, chúng tôi xin phép được ghi lại vài nét phác thảo như những dòng chữ trên cuốn sổ tay của một nhà thám hiểm:</p>" +
                    "<ul style='list-style-type: none; padding-left: 20px; color: #4A4A4A;'>" +
                    "   <li><b style='color: #2E7D32;'>✦ Mã lớp học:</b> " + roomId + "</li>" +
                    "   <li><b style='color: #2E7D32;'>✦ Tên lớp học:</b> " + room.getRoomName() + "</li>" +
                    "   <li><b style='color: #2E7D32;'>✦ Thời gian cập nhật:</b> " + new java.util.Date() + "</li>" +
                    "   <li><b style='color: #2E7D32;'>✦ Lý do:</b> Một bước điều chỉnh cần thiết để tái định hình hệ thống, mang lại giá trị tốt nhất cho hành trình học tập của bạn và tất cả mọi người.</li>" +
                    "</ul>" +
                    "<p style='color: #4A4A4A;'>Chúng tôi hiểu rằng, mỗi thay đổi đều có thể mang theo những cảm xúc lẫn lộn – có thể là chút ngỡ ngàng, chút tò mò, hay thậm chí là chút tiếc nuối. Nhưng xin bạn hãy tin rằng, đằng sau quyết định này là cả một tấm lòng và sự tận tụy của chúng tôi, với mong muốn bạn luôn được đặt trong những điều kiện tốt nhất để tỏa sáng. Nếu bạn có bất kỳ câu hỏi nào, hay chỉ đơn giản muốn chia sẻ suy nghĩ của mình, đừng ngần ngại liên lạc với chúng tôi qua <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none; font-weight: bold;'>vuthanhtruong1280@gmail.com</a> hoặc số điện thoại <b style='color: #C0392B;'>0394444107</b>. Chúng tôi luôn ở đây, sẵn sàng lắng nghe bạn như những người bạn đồng hành chân thành.</p>" +
                    "<p style='color: #4A4A4A;'>Thân mến gửi đến bạn, hành trình học tập không bao giờ là một đường thẳng. Nó là những khúc quanh, những ngã rẽ, và những chân trời mới đang chờ bạn khám phá. Sự thay đổi này chỉ là một nốt nhạc trầm trong bản giao hưởng tuổi trẻ của bạn – một nốt nhạc mở đường cho những giai điệu rực rỡ hơn. Chúng tôi tin rằng, với tài năng, nhiệt huyết và khát vọng của bạn, bầu trời rộng lớn phía trước sẽ là nơi bạn vẽ nên những ước mơ của mình.</p>" +
                    "<p style='color: #4A4A4A;'>Trước khi khép lại lá thư này, chúng tôi muốn gửi tới bạn lời chúc nồng nhiệt nhất: Chúc bạn luôn mạnh mẽ như ngọn gió, rực sáng như ánh sao, và hạnh phúc trong từng khoảnh khắc của cuộc sống. Bạn không chỉ là một sinh viên, mà là một phần không thể thiếu trong câu chuyện lớn lao mà chúng tôi cùng nhau xây dựng.</p>" +
                    "<p style='margin-top: 30px; text-align: center; color: #7F8C8D;'><i>Trân trọng gửi tới bạn,</i></p>" +
                    "<p style='text-align: center; color: #34495E;'>" +
                    "<b>" + employee.getFirstName() + " " + employee.getLastName() + "</b><br>" +
                    "Quản Trị Viên Hệ Thống<br>" +
                    "Hệ Thống Quản Lý Giáo Dục - xAI Education<br>" +
                    "Email: <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none;'>support@xaiedu.com</a> | Hotline: <span style='color: #C0392B;'>0394444107</span></p>" +
                    "</body></html>";

            sendEmail(student.getEmail(), subject, message);

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


    public void sendEmail(String recipientEmail, String subject, String htmlMessage) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlMessage, true); // true = nội dung là HTML
            helper.setFrom("vuthanhtruong1280@gmail.com");

            mailSender.send(mimeMessage);
            System.out.println("Email HTML đã được gửi thành công!");
        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Gửi email thất bại: " + e.getMessage());
        }
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
