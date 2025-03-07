package com.example.demo.POST;

import com.example.demo.OOP.*;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/")
@Transactional
public class NhanVienPost {

    @PersistenceContext
    private EntityManager entityManager;


    @PostMapping("/DangKyNhanVien")
    public String DangKyNhanVien(@RequestParam String EmployeeID,
                                 @RequestParam String FirstName,
                                 @RequestParam String LastName,
                                 @RequestParam String Email,
                                 @RequestParam String PhoneNumber,
                                 @RequestParam String Password,
                                 @RequestParam String ConfirmPassword,
                                 Model model) {

        // Kiểm tra EmployeeID đã tồn tại chưa
        if (entityManager.find(Employees.class, EmployeeID) != null) {
            model.addAttribute("employeeIDError", "Mã nhân viên đã tồn tại.");
            return "DangKyNhanVien";
        }

        // Kiểm tra Email đã tồn tại chưa
        List<Employees> existingEmployees = entityManager.createQuery(
                        "SELECT e FROM Employees e WHERE e.email = :email", Employees.class)
                .setParameter("email", Email)
                .getResultList();
        if (!existingEmployees.isEmpty()) {
            model.addAttribute("emailError", "Email này đã được sử dụng.");
            return "DangKyNhanVien";
        }

        // Kiểm tra số điện thoại (chỉ chứa số, tối thiểu 10 chữ số)
        if (!PhoneNumber.matches("\\d{10,}")) {
            model.addAttribute("phoneError", "Số điện thoại không hợp lệ (phải có ít nhất 10 chữ số).");
            return "DangKyNhanVien";
        }

        // Kiểm tra mật khẩu có khớp không
        if (!Password.equals(ConfirmPassword)) {
            model.addAttribute("passwordError", "Mật khẩu không khớp.");
            return "DangKyNhanVien";
        }

        // Kiểm tra độ mạnh của mật khẩu (8+ ký tự, chứa số và ký tự đặc biệt)
        if (!isValidPassword(Password)) {
            model.addAttribute("passwordStrengthError", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm số và ký tự đặc biệt.");
            return "DangKyNhanVien";
        }

        // Lấy Admin
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = admins.get(0);

        // Tạo nhân viên mới
        Employees employees = new Employees();
        employees.setId(EmployeeID);
        employees.setFirstName(FirstName);
        employees.setLastName(LastName);
        employees.setEmail(Email);
        employees.setPassword(Password);
        employees.setPhoneNumber(PhoneNumber);
        employees.setAdmin(admin);

        entityManager.persist(employees);

        return "redirect:/DangNhapNhanVien";
    }

    // Hàm kiểm tra độ mạnh của mật khẩu
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                Pattern.compile("[0-9]").matcher(password).find() &&
                Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find();
    }

    @Transactional
    @PostMapping("/ThemGiaoVienCuaBan")
    public String themGiaoVienCuaBan(
            @RequestParam("teacherID") String teacherID,
            @RequestParam("password") String password,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "misID", required = false) String misID,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        Employees employee = (Employees) person;


        // Kiểm tra TeacherID đã tồn tại chưa
        if (entityManager.find(Teachers.class, teacherID) != null) {
            redirectAttributes.addFlashAttribute("error", "Mã giáo viên đã tồn tại.");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra email đã tồn tại chưa
        Long emailCount = entityManager.createQuery(
                        "SELECT COUNT(t) FROM Teachers t WHERE t.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        if (emailCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Email đã tồn tại.");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra độ mạnh của mật khẩu
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$")) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Lấy Admin (giả sử hệ thống luôn có ít nhất một admin)
        List<Admin> admins = entityManager.createQuery("FROM Admin", Admin.class).getResultList();
        if (admins.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không thể xác định Admin.");
            return "redirect:/ThemGiaoVienCuaBan";
        }
        Admin admin = admins.get(0);

        try {
            // Tạo giáo viên mới
            Teachers teacher = new Teachers();
            teacher.setId(teacherID);
            teacher.setFirstName(firstName);
            teacher.setLastName(lastName);
            teacher.setEmail(email);
            teacher.setPhoneNumber(phoneNumber);
            teacher.setMisID(misID != null ? misID : "");
            teacher.setPassword(password);
            teacher.setEmployee(employee);
            teacher.setAdmin(admin);

            // Lưu vào database
            entityManager.persist(teacher);

            // Chuyển hướng với thông báo thành công
            redirectAttributes.addFlashAttribute("successMessage", "Thêm giáo viên thành công!");
            return "redirect:/DanhSachGiaoVienCuaBan";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi thêm giáo viên: " + e.getMessage());
            return "redirect:/ThemGiaoVienCuaBan";
        }
    }

    @PostMapping("/ThemHocSinhCuaBan")
    public String ThemHocSinhCuaBan(
            @RequestParam("studentID") String studentID,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "misID", required = false) String misId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        Employees employee = (Employees) person;

        // Lấy Admin (chỉ cần một admin)
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        if (admins.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy Admin!");
            return "redirect:/ThemHocSinhCuaBan";
        }
        Admin admin = admins.get(0);

        // Kiểm tra xem studentID đã tồn tại chưa
        if (entityManager.find(Students.class, studentID) != null) {
            redirectAttributes.addFlashAttribute("error", "Mã học sinh đã tồn tại!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Kiểm tra xem email đã tồn tại chưa
        Long emailCount = entityManager.createQuery(
                        "SELECT COUNT(s) FROM Students s WHERE s.email = :email", Long.class)
                .setParameter("email", email)
                .getSingleResult();
        if (emailCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Tạo mới student (KHÔNG mã hóa mật khẩu)
        Students student = new Students();
        student.setId(studentID);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setPassword(password); // LƯU MẬT KHẨU DƯỚI DẠNG THÔ
        student.setMisId(misId);
        student.setEmployee(employee);
        student.setAdmin(admin);

        // Lưu vào database
        entityManager.persist(student);

        // Chuyển hướng với thông báo thành công
        redirectAttributes.addFlashAttribute("successMessage", "Thêm học sinh thành công!");
        return "redirect:/DanhSachHocSinhCuaBan";
    }

    @PostMapping("/SuaGiaoVienCuaBan")
    public String SuaGiaoVienCuaBan(@RequestParam("teacherID") String id,
                                    @RequestParam("email") String email,
                                    @RequestParam("phoneNumber") String phoneNumber,
                                    @RequestParam("lastName") String lastName,
                                    @RequestParam("firstName") String firstName,
                                    ModelMap model) {

        // Kiểm tra giáo viên có tồn tại không
        Teachers teacher = entityManager.find(Teachers.class, id);
        if (teacher == null) {
            model.addAttribute("error", "Giáo viên không tồn tại!");
            return "SuaGiaoVienCuaBan"; // Trả về trang chỉnh sửa với thông báo lỗi
        }

        // Kiểm tra email đã được sử dụng bởi giáo viên khác chưa
        List<Teachers> teachersWithEmail = entityManager.createQuery(
                        "SELECT t FROM Teachers t WHERE t.email = :email AND t.id <> :id", Teachers.class)
                .setParameter("email", email)
                .setParameter("id", id)
                .getResultList();

        if (!teachersWithEmail.isEmpty()) {
            model.addAttribute("error", "Email này đã được sử dụng bởi giáo viên khác!");
            return "SuaGiaoVienCuaBan"; // Trả về trang chỉnh sửa với thông báo lỗi
        }

        // Cập nhật thông tin giáo viên
        teacher.setEmail(email);
        teacher.setPhoneNumber(phoneNumber);
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);

        entityManager.merge(teacher); // Lưu vào database

        model.addAttribute("successMessage", "Cập nhật giáo viên thành công!");
        return "redirect:/DanhSachGiaoVienCuaBan";
    }

    @PostMapping("/SuaHocSinhCuaBan")
    public String SuaHocSinhCuaBan(@RequestParam("studentID") String studentID,
                                   @RequestParam("firstName") String firstName,
                                   @RequestParam("lastName") String lastName,
                                   @RequestParam("email") String email,
                                   @RequestParam("phoneNumber") String phoneNumber,
                                   @RequestParam(value = "misId", required = false) String misId,
                                   RedirectAttributes redirectAttributes) {
        // Tìm học sinh theo ID
        Students student = entityManager.find(Students.class, studentID);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy học sinh!");
            return "redirect:/DanhSachHocSinhCuaBan";
        }

        // Kiểm tra trùng email với học sinh khác
        List<Students> studentsWithEmail = entityManager.createQuery(
                        "SELECT s FROM Students s WHERE s.email = :email AND s.id <> :studentID", Students.class)
                .setParameter("email", email)
                .setParameter("studentID", studentID)
                .getResultList();

        if (!studentsWithEmail.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng bởi học sinh khác!");
            return "redirect:/DanhSachHocSinhCuaBan";
        }

        // Cập nhật thông tin học sinh
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setMisId(misId);

        entityManager.merge(student); // Lưu vào database

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật học sinh thành công!");
        return "redirect:/DanhSachHocSinhCuaBan";
    }

    @PostMapping("/ThemPhongHoc")
    public String ThemPhongHoc(@RequestParam("roomId") String roomId,
                               @RequestParam("roomName") String roomName,

                               RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        Employees employee = (Employees) person;
        // Kiểm tra trùng ID
        Rooms existingRoomById = entityManager.find(Rooms.class, roomId);
        if (existingRoomById != null) {
            redirectAttributes.addFlashAttribute("error", "ID phòng đã tồn tại!");
            return "redirect:/ThemPhongHoc";
        }

        // Kiểm tra trùng tên phòng
        TypedQuery<Rooms> query = entityManager.createQuery(
                "SELECT r FROM Rooms r WHERE r.roomName = :roomName", Rooms.class);
        query.setParameter("roomName", roomName);
        List<Rooms> existingRoomsByName = query.getResultList();
        if (!existingRoomsByName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên phòng đã tồn tại!");
            return "redirect:/ThemPhongHoc";
        }

        // Thêm phòng học mới
        Rooms newRoom = new Rooms();
        newRoom.setRoomId(roomId);
        newRoom.setRoomName(roomName);
        newRoom.setEmployee(employee);
        entityManager.persist(newRoom);

        redirectAttributes.addFlashAttribute("success", "Thêm phòng học thành công!");
        return "redirect:/DanhSachPhongHoc";
    }

    @PostMapping("/ThemPhongHocOnline")
    public String LuuPhongHocOnline(
            @RequestParam("roomId") String roomId,
            @RequestParam("roomName") String roomName,
            @RequestParam("status") Boolean status,
            RedirectAttributes redirectAttributes) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        Employees employee = (Employees) person;

        // Kiểm tra ID phòng đã tồn tại chưa
        OnlineRooms existingRoomById = entityManager.find(OnlineRooms.class, roomId);
        if (existingRoomById != null) {
            redirectAttributes.addFlashAttribute("error", "ID phòng học đã tồn tại.");
            return "redirect:/ThemPhongHocOnline";
        }

        // Kiểm tra tên phòng đã tồn tại chưa
        TypedQuery<OnlineRooms> query = entityManager.createQuery(
                "SELECT r FROM OnlineRooms r WHERE r.roomName = :roomName", OnlineRooms.class);
        query.setParameter("roomName", roomName);
        List<OnlineRooms> existingRoomsByName = query.getResultList();
        if (!existingRoomsByName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên phòng học đã tồn tại.");
            return "redirect:/ThemPhongHocOnline";
        }

        // Tạo phòng học online mới
        OnlineRooms newRoom = new OnlineRooms();
        newRoom.setRoomId(roomId);
        newRoom.setRoomName(roomName);
        newRoom.setStatus(status);
        newRoom.setEmployee(employee);

        // Lưu vào database
        entityManager.persist(newRoom);
        redirectAttributes.addFlashAttribute("success", "Thêm phòng học thành công.");

        return "redirect:/DanhSachPhongHoc";
    }


    @PostMapping("/SuaPhongHocOffline")
    public String CapNhatPhongHoc(@RequestParam("roomId") String roomId,
                                  @RequestParam("roomName") String roomName) {
        // Lấy thông tin phòng từ database
        Rooms room = entityManager.find(Rooms.class, roomId);
        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        Employees employee = (Employees) person;

        // Kiểm tra xem tên phòng mới đã tồn tại chưa
        TypedQuery<Rooms> query = entityManager.createQuery(
                "SELECT r FROM Rooms r WHERE r.roomName = :roomName AND r.roomId <> :roomId",
                Rooms.class
        );
        query.setParameter("roomName", roomName);
        query.setParameter("roomId", roomId);

        List<Rooms> existingRooms = query.getResultList();
        if (!existingRooms.isEmpty()) {
            return "redirect:/SuaPhongHocOffline?roomId=" + roomId + "&error=RoomNameExists";
        }

        // Cập nhật thông tin phòng học
        room.setRoomName(roomName);
        room.setEmployee(employee);
        entityManager.merge(room);  // Lưu thay đổi

        return "redirect:/DanhSachPhongHoc?success=RoomUpdated";
    }

    @PostMapping("/SuaPhongHocOnline")
    public String CapNhatPhongHocOnline(
            @RequestParam("roomId") String roomId,
            @RequestParam("roomName") String roomName) {

        // Kiểm tra phòng học có tồn tại không
        OnlineRooms room = entityManager.find(OnlineRooms.class, roomId);
        if (room == null) {
            return "redirect:/DanhSachPhongHoc?error=RoomNotFound";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        Employees employee = (Employees) person;


        // Kiểm tra trùng tên phòng (trừ phòng hiện tại)
        TypedQuery<OnlineRooms> query = entityManager.createQuery(
                "SELECT r FROM OnlineRooms r WHERE r.roomName = :roomName AND r.roomId <> :roomId", OnlineRooms.class);
        query.setParameter("roomName", roomName);
        query.setParameter("roomId", roomId);

        List<OnlineRooms> duplicateRooms = query.getResultList();
        if (!duplicateRooms.isEmpty()) {
            return "redirect:/SuaPhongHocOnline?error=RoomNameExists&roomId=" + roomId + "&roomName=" + roomName;
        }

        // Cập nhật thông tin phòng học online
        room.setRoomName(roomName);
        room.setEmployee(employee);
        entityManager.merge(room);  // Lưu thay đổi

        return "redirect:/DanhSachPhongHoc?success=RoomUpdated";
    }

    @PostMapping("/ThemGiaoVienVaoLop")
    @Transactional
    public String ThemGiaoVienVaoLop(@RequestParam String roomId, @RequestParam List<String> teacherIds) {
        // Tìm Room theo roomId
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId);
        }

        for (String teacherId : teacherIds) {
            // Tìm Teacher theo teacherId
            Person teacher = entityManager.find(Person.class, teacherId);
            if (teacher == null || !(teacher instanceof Teachers)) {
                throw new IllegalArgumentException("Không tìm thấy giáo viên với ID: " + teacherId);
            }

            // Kiểm tra xem giáo viên đã có trong lớp chưa
            Long count = entityManager.createQuery(
                            "SELECT COUNT(cd) FROM ClassroomDetails cd WHERE cd.room = :room AND cd.member = :teacher",
                            Long.class)
                    .setParameter("room", room)
                    .setParameter("teacher", teacher)
                    .getSingleResult();

            if (count == 0) {
                ClassroomDetails classroomDetail = new ClassroomDetails(room, teacher);
                Events event = entityManager.find(Events.class, 9);
                classroomDetail.setEvent(event);
                entityManager.persist(classroomDetail);
            }
        }

        return "redirect:/ChiTietLopHoc/" + roomId + "?success=updated";
    }

    @PostMapping("/ThemHocSinhVaoLop")
    @Transactional
    public String ThemHocSinhVaoLop(@RequestParam String roomId, @RequestParam List<String> studentIds) {
        // Tìm Room theo roomId
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId);
        }

        for (String studentId : studentIds) {
            // Tìm Student theo studentId
            Person student = entityManager.find(Person.class, studentId);
            if (student == null || !(student instanceof Students)) {
                throw new IllegalArgumentException("Không tìm thấy học sinh với ID: " + studentId);
            }

            // Kiểm tra xem học sinh đã có trong lớp chưa
            Long count = entityManager.createQuery(
                            "SELECT COUNT(cd) FROM ClassroomDetails cd WHERE cd.room = :room AND cd.member = :student",
                            Long.class)
                    .setParameter("room", room)
                    .setParameter("student", student)
                    .getSingleResult();

            if (count == 0) {
                ClassroomDetails classroomDetail = new ClassroomDetails(room, student);
                Events event = entityManager.find(Events.class, 10);
                classroomDetail.setEvent(event);
                entityManager.persist(classroomDetail);
            }
        }

        return "redirect:/ChiTietLopHoc/" + roomId + "?success=updated";
    }

    @PostMapping("/CapNhatLichTrinh")
    @Transactional
    public String capNhatLichTrinh(
            @RequestParam("roomId") String roomId,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime) {

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            LocalDateTime thoiGianBatDau = LocalDateTime.parse(startTime, formatter);
            LocalDateTime thoiGianKetThuc = LocalDateTime.parse(endTime, formatter);

            Room room = entityManager.find(Room.class, roomId);
            if (room instanceof Rooms) {
                room.setStartTime(thoiGianBatDau);
                room.setEndTime(thoiGianKetThuc);
            } else if (room instanceof OnlineRooms) {
                room.setStartTime(thoiGianBatDau);
                room.setEndTime(thoiGianKetThuc);
            } else {
                return "redirect:/BoTriLopHoc?error=notfound";
            }

            entityManager.merge(room);
            return "redirect:/BoTriLopHoc?success=updated";

        } catch (DateTimeParseException e) {
            return "redirect:/BoTriLopHoc?error=invalid_datetime";
        }
    }

    @PostMapping("/CapNhatDiaChi")
    @Transactional
    public String capNhatDiaChi(
            @RequestParam("roomId") String roomId,
            @RequestParam("roomAddress") String diaChiMoi) {

        Rooms room = entityManager.find(Rooms.class, roomId);
        if (room != null) {
            room.setAddress(diaChiMoi);
            entityManager.merge(room);
            return "redirect:/BoTriLopHoc?success=updated";
        } else {
            return "redirect:/BoTriLopHoc?error=notfound";
        }
    }

    // Cập nhật link phòng học Online
    @PostMapping("/CapNhatLink")
    @Transactional
    public String capNhatLink(
            @RequestParam("roomId") String roomId,
            @RequestParam("roomLink") String linkMoi) {

        OnlineRooms room = entityManager.find(OnlineRooms.class, roomId);
        if (room != null) {
            room.setLink(linkMoi);
            entityManager.merge(room);
            return "redirect:/BoTriLopHoc?success=updated";
        } else {
            return "redirect:/BoTriLopHoc?error=notfound";
        }
    }

    @PostMapping("/DanhSachHocSinhCuaBan")
    public String DanhSachHocSinhCuaBan(@RequestParam("pageSize") int pageSize, HttpSession session) {
        session.setAttribute("pageSize", pageSize); // Đặt session với đúng key
        return "redirect:/DanhSachHocSinhCuaBan";
    }

    @PostMapping("/DanhSachGiaoVienCuaBan")
    public String DanhSachGiaoVienCuaBan(@RequestParam("pageSize") int pageSize, HttpSession session) {
        session.setAttribute("pageSize2", pageSize);
        return "redirect:/DanhSachGiaoVienCuaBan"; // Đúng trang cần quay lại
    }

    @PostMapping("/DanhSachPhongHoc")
    public String setPageSize(@RequestParam("pageSize") int pageSize, HttpSession session) {
        session.setAttribute("pageSize3", pageSize); // Thống nhất key với GET request
        return "redirect:/DanhSachPhongHoc";
    }

    @PostMapping("/DanhSachNguoiDungHeThong")
    public String DanhSachNguoiDungHeThong1(@RequestParam("pageSize") int pageSize, HttpSession session) {
        session.setAttribute("pageSize4", pageSize); // Thống nhất key với GET request
        return "redirect:/DanhSachNguoiDungHeThong";
    }

    @PostMapping("/CapNhatTrangCaNhan")
    public String CapNhatTrangCaNhan(
            @RequestParam String employeeId,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            HttpSession session,
            ModelMap model) {

        if (session.getAttribute("EmployeeID") == null) {
            return "redirect:/DangNhapNhanVien";
        }

        Employees employee = entityManager.find(Employees.class, employeeId);
        if (employee == null) {
            model.addAttribute("error", "Không tìm thấy nhân viên.");
            return "TrangCaNhanNhanVien";
        }

        // Kiểm tra dữ liệu đầu vào
        if (fullName.trim().isEmpty() || email.trim().isEmpty() || phoneNumber.trim().isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin.");
            return "TrangCaNhanNhanVien";
        }

        // Cập nhật thông tin
        String[] nameParts = fullName.split(" ", 2);
        employee.setFirstName(nameParts.length > 1 ? nameParts[0] : fullName);
        employee.setLastName(nameParts.length > 1 ? nameParts[1] : "");
        employee.setEmail(email);
        employee.setPhoneNumber(phoneNumber);

        entityManager.merge(employee);
        model.addAttribute("success", "Cập nhật thành công!");

        return "redirect:/TrangCaNhanNhanVien";
    }


}
