package com.example.demo.POST;

import com.example.demo.OOP.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
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

    @Autowired
    private JavaMailSender mailSender; // Không khai báo lại ở nơi khác


    @Transactional
    @PostMapping("/DangKyNhanVien")
    public String DangKyNhanVien(@RequestParam String EmployeeID,
                                 @RequestParam String FirstName,
                                 @RequestParam String LastName,
                                 @RequestParam String Email,
                                 @RequestParam String PhoneNumber,
                                 @RequestParam String Password,
                                 @RequestParam String ConfirmPassword,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate DateOfBirth,
                                 Model model) {

        System.out.println("Bắt đầu đăng ký nhân viên...");

        // Kiểm tra EmployeeID đã tồn tại chưa
        if (entityManager.find(Person.class, EmployeeID) != null) {
            model.addAttribute("employeeIDError", "Mã nhân viên đã tồn tại.");
            return "DangKyNhanVien";
        }

        // Kiểm tra Email có hợp lệ không
        if (!Email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            model.addAttribute("emailFormatError", "Email không hợp lệ.");
            return "DangKyNhanVien";
        }
        // Kiểm tra định dạng số điện thoại
        if (!PhoneNumber.matches("^[0-9]+$")) { // Kiểm tra toàn số
            model.addAttribute("phoneError", "Số điện thoại chỉ được chứa chữ số!");
            return "DangKyNhanVien";
        } else if (!PhoneNumber.matches("^\\d{9,10}$")) { // Kiểm tra độ dài
            model.addAttribute("phoneError", "Số điện thoại phải có 9-10 chữ số!");
            return "DangKyNhanVien";
        }

        // Kiểm tra Email đã tồn tại chưa
        List<Person> existingEmployeesByEmail = entityManager.createQuery(
                        "SELECT e FROM Person e WHERE e.email = :email", Person.class)
                .setParameter("email", Email)
                .getResultList();
        if (!existingEmployeesByEmail.isEmpty()) {
            model.addAttribute("emailError", "Email này đã được sử dụng.");
            return "DangKyNhanVien";
        }

        // Kiểm tra số điện thoại (chỉ chứa số, tối thiểu 10 chữ số)
        if (!PhoneNumber.matches("\\d{10,}")) {
            model.addAttribute("phoneError", "Số điện thoại không hợp lệ (phải có ít nhất 10 chữ số).");
            return "DangKyNhanVien";
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        List<Person> existingEmployeesByPhone = entityManager.createQuery(
                        "SELECT e FROM Person e WHERE e.phoneNumber = :phoneNumber", Person.class)
                .setParameter("phoneNumber", PhoneNumber)
                .getResultList();
        if (!existingEmployeesByPhone.isEmpty()) {
            model.addAttribute("phoneDuplicateError", "Số điện thoại này đã được sử dụng.");
            return "DangKyNhanVien";
        }

        // Kiểm tra mật khẩu có khớp không
        if (!Password.equals(ConfirmPassword)) {
            model.addAttribute("passwordError", "Mật khẩu không khớp.");
            return "DangKyNhanVien";
        }

        // Kiểm tra độ mạnh của mật khẩu (ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt)
        if (!isValidPassword(Password)) {
            model.addAttribute("passwordStrengthError", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return "DangKyNhanVien";
        }

        // Kiểm tra tuổi (phải >= 18 tuổi)
        LocalDate today = LocalDate.now();
        int age = Period.between(DateOfBirth, today).getYears();
        if (age < 18) {
            model.addAttribute("dobError", "Bạn phải từ 18 tuổi trở lên để đăng ký.");
            return "DangKyNhanVien";
        }

        // Lấy Admin (nếu không có admin -> lỗi)
        List<Admin> admins = entityManager.createQuery("FROM Admin", Admin.class).getResultList();
        if (admins.isEmpty()) {
            model.addAttribute("adminError", "Không tìm thấy Admin.");
            return "DangKyNhanVien";
        }
        Admin admin = admins.get(0);

        // Tạo nhân viên mới
        Employees employees = new Employees();
        employees.setId(EmployeeID);
        employees.setFirstName(FirstName);
        employees.setLastName(LastName);
        employees.setEmail(Email);
        employees.setPassword(Password); // Lưu mật khẩu đã mã hóa
        employees.setPhoneNumber(PhoneNumber);
        employees.setBirthDate(DateOfBirth);
        employees.setAdmin(admin);

        try {
            entityManager.persist(employees);
            System.out.println("Đăng ký nhân viên thành công!");
        } catch (Exception e) {
            System.out.println("Lỗi khi lưu nhân viên: " + e.getMessage());
            model.addAttribute("databaseError", "Lỗi khi lưu dữ liệu.");
            return "DangKyNhanVien";
        }

        return "redirect:/TrangChu";
    }


    // Hàm kiểm tra độ mạnh của mật khẩu
    private boolean isValidPassword(String password) {
        return password.length() >= 8 &&
                Pattern.compile("[0-9]").matcher(password).find() &&
                Pattern.compile("[!@#$%^&*(),.?\":{}|<>]").matcher(password).find();
    }

    @PostMapping("/ThemGiaoVienCuaBan")
    @Transactional(rollbackOn = Exception.class)
    public String themGiaoVienCuaBan(
            @RequestParam("teacherID") String teacherID,
            @RequestParam("password") String password,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "misID", required = false) String misId,
            RedirectAttributes redirectAttributes) {

        // Lấy thông tin nhân viên từ Authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        if (!(person instanceof Employees employee)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thêm giáo viên!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Lấy Admin
        Admin admin = entityManager.createQuery("SELECT a FROM Admin a", Admin.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (admin == null) {
            redirectAttributes.addFlashAttribute("errorAdmin", "Không tìm thấy Admin!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra TeacherID đã tồn tại chưa
        if (entityManager.find(Person.class, teacherID) != null) {
            redirectAttributes.addFlashAttribute("errorTeacherID", "Mã giáo viên đã tồn tại!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra Email hợp lệ
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            redirectAttributes.addFlashAttribute("errorEmail", "Email không hợp lệ!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra Email đã tồn tại chưa
        boolean emailExists = entityManager.createQuery(
                        "SELECT COUNT(t) > 0 FROM Person t WHERE t.email = :email", Boolean.class)
                .setParameter("email", email)
                .getSingleResult();
        if (emailExists) {
            redirectAttributes.addFlashAttribute("errorEmail", "Email này đã được sử dụng!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("^[0-9]+$")) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại chỉ được chứa chữ số!");
            return "redirect:/ThemGiaoVienCuaBan";
        } else if (!phoneNumber.matches("^\\d{9,10}$")) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại phải có 9-10 chữ số!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        boolean phoneExists = entityManager.createQuery(
                        "SELECT COUNT(t) > 0 FROM Person t WHERE t.phoneNumber = :phoneNumber", Boolean.class)
                .setParameter("phoneNumber", phoneNumber)
                .getSingleResult();
        if (phoneExists) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại này đã được sử dụng!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra mật khẩu
        if (!isValidPassword(password)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Kiểm tra tên chỉ chứa chữ cái
        String formattedFirstName = formatName(firstName);
        String formattedLastName = formatName(lastName);
        if (formattedFirstName == null || formattedLastName == null) {
            redirectAttributes.addFlashAttribute("errorName", "Họ và tên chỉ được chứa chữ cái!");
            return "redirect:/ThemGiaoVienCuaBan";
        }

        // Tạo đối tượng giáo viên
        Teachers teacher = new Teachers();
        teacher.setId(teacherID);
        teacher.setFirstName(formattedFirstName);
        teacher.setLastName(formattedLastName);
        teacher.setEmail(email);
        teacher.setPhoneNumber(phoneNumber); // Lưu số điện thoại đầy đủ với +84
        teacher.setMisID(misId);
        teacher.setPassword(password);
        teacher.setEmployee(employee);
        teacher.setAdmin(admin);

        // Lưu giáo viên vào database
        entityManager.persist(teacher);

        redirectAttributes.addFlashAttribute("successMessage", "Thêm giáo viên thành công!");
        return "redirect:/DanhSachGiaoVienCuaBan";
    }

    @PostMapping("/CapNhatSoSlot")
    public String capNhatSoSlot(@RequestParam("roomId") String roomId, @RequestParam("slotQuantity") Integer slotQuantity) {
        Room room = entityManager.find(Room.class, roomId);
        if (room != null) {
            room.setSlotQuantity(slotQuantity);
            entityManager.merge(room);
        }
        return "redirect:/BoTriLopHoc";
    }

    @PostMapping("/ThemHocSinhCuaBan")
    @Transactional(rollbackOn = Exception.class)
    public String ThemHocSinhCuaBan(
            @RequestParam("studentID") String studentID,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("phoneNumber") String phoneNumber,
            @RequestParam(value = "misID", required = false) String misId,
            RedirectAttributes redirectAttributes) {

        // Lấy thông tin nhân viên từ Authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        if (!(person instanceof Employees employee)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thêm học sinh!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Lấy Admin
        Admin admin = entityManager.createQuery("SELECT a FROM Admin a", Admin.class)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElse(null);
        if (admin == null) {
            redirectAttributes.addFlashAttribute("errorAdmin", "Không tìm thấy Admin!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Kiểm tra StudentID đã tồn tại chưa
        if (entityManager.find(Person.class, studentID) != null) {
            redirectAttributes.addFlashAttribute("errorStudentID", "Mã học sinh đã tồn tại!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Kiểm tra định dạng Email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            redirectAttributes.addFlashAttribute("errorEmail", "Email không hợp lệ!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Kiểm tra Email đã tồn tại chưa
        boolean emailExists = entityManager.createQuery(
                        "SELECT COUNT(s) > 0 FROM Person s WHERE s.email = :email", Boolean.class)
                .setParameter("email", email)
                .getSingleResult();
        if (emailExists) {
            redirectAttributes.addFlashAttribute("errorEmail", "Email này đã được sử dụng!");
            return "redirect:/ThemHocSinhCuaBan";
        }


        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("^[0-9]+$")) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại chỉ được chứa chữ số!");
            return "redirect:/ThemHocSinhCuaBan";
        } else if (!phoneNumber.matches("^\\d{9,10}$")) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại phải có 9-10 chữ số!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Kiểm tra số điện thoại đã tồn tại chưa
        boolean phoneExists = entityManager.createQuery(
                        "SELECT COUNT(s) > 0 FROM Person s WHERE s.phoneNumber = :phoneNumber", Boolean.class)
                .setParameter("phoneNumber", phoneNumber)
                .getSingleResult();
        if (phoneExists) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại này đã được sử dụng!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Kiểm tra mật khẩu
        if (!isValidPassword(password)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Chuẩn hóa họ tên
        String formattedFirstName = formatName(firstName);
        String formattedLastName = formatName(lastName);
        if (formattedFirstName == null || formattedLastName == null) {
            redirectAttributes.addFlashAttribute("errorName", "Họ và tên chỉ được chứa chữ cái!");
            return "redirect:/ThemHocSinhCuaBan";
        }

        // Tạo mới student
        Students student = new Students();
        student.setId(studentID);
        student.setFirstName(formattedFirstName);
        student.setLastName(formattedLastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber); // Lưu số điện thoại đầy đủ với +84
        student.setPassword(password); // Mã hóa mật khẩu
        student.setMisId(misId);
        student.setEmployee(employee);
        student.setAdmin(admin);

        // Lưu vào database
        entityManager.persist(student);

        // Chuyển hướng với thông báo thành công
        redirectAttributes.addFlashAttribute("successMessage", "Thêm học sinh thành công!");
        return "redirect:/DanhSachHocSinhCuaBan";
    }

    // Phương thức định dạng tên (chỉ giữ chữ cái)
    private String formatName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null; // Thay vì trả về "" để báo lỗi nếu tên rỗng
        }

        String[] words = name.trim().toLowerCase().split("\\s+");
        StringBuilder formattedName = new StringBuilder();

        for (String word : words) {
            if (!word.matches("[a-zA-ZÀ-Ỵà-ỵ]+")) { // Kiểm tra ký tự hợp lệ (bao gồm tiếng Việt)
                return null; // Tên không hợp lệ
            }
            formattedName.append(Character.toUpperCase(word.charAt(0))) // Viết hoa chữ cái đầu
                    .append(word.substring(1)) // Phần còn lại viết thường
                    .append(" ");
        }
        return formattedName.toString().trim();
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        String phoneRegex = "^\\+?[0-9]{10,15}$";
        return phoneNumber.matches(phoneRegex);
    }

    @PostMapping("/SuaGiaoVienCuaBan")
    public String SuaGiaoVienCuaBan(@RequestParam("teacherID") String teacherID,
                                    @RequestParam("firstName") String firstName,
                                    @RequestParam("lastName") String lastName,
                                    @RequestParam("email") String email,
                                    @RequestParam("phoneNumber") String phoneNumber,
                                    RedirectAttributes redirectAttributes) {

        // Kiểm tra định dạng email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            redirectAttributes.addFlashAttribute("error", "Email không hợp lệ!");
            return "redirect:/SuaGiaoVienCuaBan/" + teacherID;  // Redirect back to the edit page with error message
        }

        // Kiểm tra định dạng số điện thoại (chỉ gồm 10-11 chữ số)
        if (!phoneNumber.matches("^\\d{10,11}$")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ!");
            return "redirect:/SuaGiaoVienCuaBan/" + teacherID;  // Redirect back to the edit page with error message
        }

        // Kiểm tra định dạng tên (chỉ chứa chữ cái và khoảng trắng)
        if (!firstName.matches("^[A-Za-zÀ-ỹ\\s]+$") || !lastName.matches("^[A-Za-zÀ-ỹ\\s]+$")) {
            redirectAttributes.addFlashAttribute("error", "Họ và tên chỉ được chứa chữ cái!");
            return "redirect:/SuaGiaoVienCuaBan/" + teacherID;  // Redirect back to the edit page with error message
        }

        // Định dạng tên
        firstName = formatName(firstName);
        lastName = formatName(lastName);

        // Tìm giáo viên theo ID
        Person teacher = entityManager.find(Person.class, teacherID);
        if (teacher == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy giáo viên!");
            return "redirect:/DanhSachGiaoVienCuaBan";  // Redirect to the list page if teacher not found
        }

        // Kiểm tra trùng email với giáo viên khác
        List<Person> teachersWithEmail = entityManager.createQuery(
                        "SELECT t FROM Person t WHERE t.email = :email AND t.id <> :teacherID", Person.class)
                .setParameter("email", email)
                .setParameter("teacherID", teacherID)
                .getResultList();

        if (!teachersWithEmail.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng bởi giáo viên khác!");
            return "redirect:/SuaGiaoVienCuaBan/" + teacherID;  // Redirect back to the edit page with error message
        }

        // Kiểm tra trùng số điện thoại với giáo viên khác
        List<Person> teachersWithPhone = entityManager.createQuery(
                        "SELECT t FROM Person t WHERE t.phoneNumber = :phoneNumber AND t.id <> :teacherID", Person.class)
                .setParameter("phoneNumber", phoneNumber)
                .setParameter("teacherID", teacherID)
                .getResultList();

        if (!teachersWithPhone.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại này đã được sử dụng bởi giáo viên khác!");
            return "redirect:/SuaGiaoVienCuaBan/" + teacherID;  // Redirect back to the edit page with error message
        }

        // Cập nhật thông tin giáo viên
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(email);
        teacher.setPhoneNumber(phoneNumber);

        entityManager.merge(teacher); // Lưu vào database

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật giáo viên thành công!");
        return "redirect:/DanhSachGiaoVienCuaBan";  // Redirect to the list page after success
    }


    @PostMapping("/SuaHocSinhCuaBan")
    public String SuaHocSinhCuaBan(@RequestParam("studentID") String studentID,
                                   @RequestParam("firstName") String firstName,
                                   @RequestParam("lastName") String lastName,
                                   @RequestParam("email") String email,
                                   @RequestParam("phoneNumber") String phoneNumber,
                                   @RequestParam(value = "misId", required = false) String misId,
                                   RedirectAttributes redirectAttributes) {

        // Kiểm tra định dạng email
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            redirectAttributes.addFlashAttribute("error", "Email không hợp lệ!");
            return "redirect:/SuaHocSinhCuaBan/" + studentID;  // Redirect back to the edit page
        }

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("^\\d{10,11}$")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ!");
            return "redirect:/SuaHocSinhCuaBan/" + studentID;  // Redirect back to the edit page
        }

        // Kiểm tra định dạng tên (chỉ chứa chữ cái và khoảng trắng)
        if (!firstName.matches("^[A-Za-zÀ-ỹ\\s]+$") || !lastName.matches("^[A-Za-zÀ-ỹ\\s]+$")) {
            redirectAttributes.addFlashAttribute("error", "Họ và tên chỉ được chứa chữ cái!");
            return "redirect:/SuaHocSinhCuaBan/" + studentID;  // Redirect back to the edit page
        }

        // Định dạng tên
        firstName = formatName(firstName);
        lastName = formatName(lastName);

        // Tìm học sinh theo ID
        Students student = entityManager.find(Students.class, studentID);
        if (student == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy học sinh!");
            return "redirect:/DanhSachHocSinhCuaBan";  // Redirect to the list page if student not found
        }

        // Kiểm tra trùng email với học sinh khác
        List<Person> studentsWithEmail = entityManager.createQuery(
                        "SELECT s FROM Person s WHERE s.email = :email AND s.id <> :studentID", Person.class)
                .setParameter("email", email)
                .setParameter("studentID", studentID)
                .getResultList();

        if (!studentsWithEmail.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng bởi học sinh khác!");
            return "redirect:/SuaHocSinhCuaBan/" + studentID;  // Redirect back to the edit page
        }

        // Kiểm tra trùng số điện thoại với học sinh khác
        List<Person> studentsWithPhone = entityManager.createQuery(
                        "SELECT s FROM Person s WHERE s.phoneNumber = :phoneNumber AND s.id <> :studentID", Person.class)
                .setParameter("phoneNumber", phoneNumber)
                .setParameter("studentID", studentID)
                .getResultList();

        if (!studentsWithPhone.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại này đã được sử dụng bởi học sinh khác!");
            return "redirect:/SuaHocSinhCuaBan/" + studentID;  // Redirect back to the edit page
        }

        // Cập nhật thông tin học sinh
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setMisId(misId);

        entityManager.merge(student); // Lưu vào database

        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật học sinh thành công!");
        return "redirect:/DanhSachHocSinhCuaBan";  // Redirect to the list page after success
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
        newRoom.setCreatedAt(LocalDateTime.now());
        entityManager.persist(newRoom);

        redirectAttributes.addFlashAttribute("success", "Thêm phòng học thành công!");
        return "redirect:/DanhSachPhongHoc";
    }

    @PostMapping("/ThemPhongHocOnline")
    @Transactional
    public String LuuPhongHocOnline(
            @RequestParam("roomId") String roomId,
            @RequestParam("roomName") String roomName,
            RedirectAttributes redirectAttributes) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName();
        Person person = entityManager.find(Person.class, employeeId);
        if (!(person instanceof Employees employee)) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thêm phòng học.");
            return "redirect:/ThemPhongHocOnline";
        }

        // Kiểm tra ID phòng
        OnlineRooms existingRoomById = entityManager.find(OnlineRooms.class, roomId);
        if (existingRoomById != null) {
            redirectAttributes.addFlashAttribute("error", "ID phòng học đã tồn tại.");
            return "redirect:/ThemPhongHocOnline";
        }

        // Kiểm tra tên phòng
        TypedQuery<OnlineRooms> query = entityManager.createQuery(
                "SELECT r FROM OnlineRooms r WHERE r.roomName = :roomName", OnlineRooms.class);
        query.setParameter("roomName", roomName);
        if (!query.getResultList().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Tên phòng học đã tồn tại.");
            return "redirect:/ThemPhongHocOnline";
        }

        // Tạo link Google Meet
        String meetLink;
        try {
            GoogleCalendarService calendarService = new GoogleCalendarService();
            meetLink = calendarService.createGoogleMeetLink(roomName, LocalDateTime.now());
        } catch (IOException | GeneralSecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi tạo link Google Meet: " + e.getMessage());
            return "redirect:/ThemPhongHocOnline";
        }

        // Tạo và lưu phòng học
        OnlineRooms newRoom = new OnlineRooms();
        newRoom.setRoomId(roomId);
        newRoom.setRoomName(roomName);
        newRoom.setEmployee(employee);
        newRoom.setCreatedAt(LocalDateTime.now());
        newRoom.setLink(meetLink);

        entityManager.persist(newRoom);
        redirectAttributes.addFlashAttribute("success", "Thêm phòng học thành công với link: " + meetLink);

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
    public String ThemGiaoVienVaoLop(@RequestParam String roomId, @RequestParam(name = "teacherIds", required = false) List<String> teacherIds, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // EmployeeID đã đăng nhập

        // Tìm Employee trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);
        // Tìm Room theo roomId
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            throw new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId);
        }

        if (teacherIds == null || teacherIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("Note", "Vui lòng chọn ít nhất 1 giáo viên");
            return "redirect:/ChiTietLopHoc/" + roomId;
        }

        if (teacherIds.size() > 1) {
            redirectAttributes.addFlashAttribute("Note", "Chỉ được phép thêm duy nhất 1 giáo viên");
            return "redirect:/ChiTietLopHoc/" + roomId;
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
                // Thêm giáo viên vào lớp
                ClassroomDetails classroomDetail = new ClassroomDetails(room, teacher);
                Events event = entityManager.find(Events.class, 9); // Giả định event ID 9 là sự kiện mặc định
                classroomDetail.setEvent(event);
                entityManager.persist(classroomDetail);

                // Gửi email thông báo cho giáo viên
                String subject = "Chào Mừng Thầy/Cô Đến Với Nhiệm Vụ Cao Quý Tại " + room.getRoomName();

                String message = "<html><body style='font-family: Georgia, serif; line-height: 1.8; color: #333333; max-width: 700px; margin: 0 auto; background-color: #F9F9F9; padding: 20px;'>" +
                        "<p style='font-size: 18px; color: #1A3C5E;'><b>Kính thưa " + teacher.getFullName() + ", người dẫn đường đáng kính,</b></p>" +
                        "<p style='color: #4A4A4A;'>Trước hết, cho phép chúng tôi gửi tới Thầy/Cô những lời chào trân trọng nhất, như ánh trăng rằm soi sáng con đường tri thức, mang theo lòng biết ơn sâu sắc vì sự hiện diện của Thầy/Cô trong hành trình giáo dục của chúng tôi. Hôm nay là một dịp đặc biệt, khi Thầy/Cô chính thức gia nhập lớp học <i style='color: #D35400;'>" + room.getRoomName() + "</i> tại Hệ Thống Quản Lý Giáo Dục - xAI Education, mở ra một chương mới đầy ý nghĩa trong sứ mệnh cao cả của Thầy/Cô.</p>" +
                        "<p style='color: #4A4A4A;'>Sự góp mặt của Thầy/Cô không chỉ là một niềm vinh dự lớn lao đối với chúng tôi, mà còn là ngọn lửa thắp sáng những tâm hồn trẻ, là ngọn hải đăng dẫn lối cho những ước mơ bay xa. Đây là khoảnh khắc khởi đầu cho một hành trình nơi tài năng, tâm huyết và kinh nghiệm của Thầy/Cô sẽ trở thành nguồn cảm hứng bất tận, góp phần vẽ nên bức tranh giáo dục sống động và trọn vẹn.</p>" +
                        "<p style='color: #4A4A4A;'>Để Thầy/Cô có thể hình dung rõ hơn về nhiệm vụ mới này, chúng tôi xin phép ghi lại vài nét thông tin, như những dòng chữ được khắc trên tấm bia kỷ niệm của một chặng đường đáng nhớ:</p>" +
                        "<ul style='list-style-type: none; padding-left: 20px; color: #4A4A4A;'>" +
                        "   <li><b style='color: #2E7D32;'>✦ Mã lớp học:</b> " + roomId + "</li>" +
                        "   <li><b style='color: #2E7D32;'>✦ Tên lớp học:</b> " + room.getRoomName() + "</li>" +
                        "   <li><b style='color: #2E7D32;'>✦ Thời gian gia nhập:</b> " + new java.util.Date() + "</li>" +
                        "   <li><b style='color: #2E7D32;'>✦ Lời chào từ chúng tôi:</b> Trân trọng chào mừng Thầy/Cô đến với ngôi nhà tri thức mới, nơi mỗi bài giảng của Thầy/Cô sẽ là một viên gạch xây dựng tương lai.</li>" +
                        "</ul>" +
                        "<p style='color: #4A4A4A;'>Chúng tôi hiểu rằng, mỗi vai trò mới đều đi kèm với những kỳ vọng và trách nhiệm lớn lao. Vì vậy, nếu Thầy/Cô có bất kỳ câu hỏi nào về lớp học này, hay chỉ đơn giản muốn trao đổi để chuẩn bị cho hành trình phía trước, xin đừng ngần ngại liên hệ với chúng tôi qua <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none; font-weight: bold;'>vuthanhtruong1280@gmail.com</a> hoặc số điện thoại <b style='color: #C0392B;'>0394444107</b>. Đội ngũ của chúng tôi luôn sẵn sàng đồng hành cùng Thầy/Cô, với tất cả sự tận tâm và tôn kính.</p>" +
                        "<p style='color: #4A4A4A;'>Thưa Thầy/Cô, hành trình giáo dục là một bản giao hưởng dài, và Thầy/Cô chính là nhạc trưởng tài hoa, người sẽ dẫn dắt những nốt nhạc tri thức vang lên trong lòng học trò. Chúng tôi tin tưởng rằng, với sự tận tụy và lòng nhiệt thành của Thầy/Cô, lớp học này sẽ trở thành một mảnh đất màu mỡ, nơi những hạt giống tri thức được gieo trồng và nở hoa rực rỡ.</p>" +
                        "<p style='color: #4A4A4A;'>Trước khi khép lại lá thư này, chúng tôi xin gửi tới Thầy/Cô lời chúc tốt đẹp nhất: Chúc Thầy/Cô luôn dồi dào sức khỏe, tràn đầy cảm hứng, và tìm thấy niềm vui bất tận trong sứ mệnh cao quý của mình. Thầy/Cô không chỉ là một người giáo viên, mà còn là ngọn gió nâng cánh những ước mơ, là ánh sáng soi đường cho thế hệ trẻ.</p>" +
                        "<p style='margin-top: 30px; text-align: center; color: #7F8C8D;'><i>Trân trọng kính thư,</i></p>" +
                        "<p style='text-align: center; color: #34495E;'>" +
                        "<b>" + employee.getFirstName() + " " + employee.getLastName() + "</b><br>" +
                        "Quản Trị Viên Hệ Thống<br>" +
                        "Hệ Thống Quản Lý Giáo Dục - xAI Education<br>" +
                        "Email: <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none;'>support@xaiedu.com</a> | Hotline: <span style='color: #C0392B;'>0394444107</span></p>" +
                        "</body></html>";

                sendEmail(teacher.getEmail(), subject, message);
            }
        }

        return "redirect:/ChiTietLopHoc/" + roomId + "?success=updated";
    }

    @PostMapping("/ThemHocSinhVaoLop")
    @Transactional
    public String ThemHocSinhVaoLop(@RequestParam String roomId, @RequestParam(name = "studentIds", required = false) List<String> studentIds, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String employeeId = authentication.getName(); // EmployeeID đã đăng nhập

        // Tìm Employee trong database bằng EntityManager
        Employees employee = entityManager.find(Employees.class, employeeId);
        // Tìm Room theo roomId

        if (studentIds == null || studentIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("Note", "Vui lòng chọn 1 hoặc nhiều học sinh");
            return "redirect:/ChiTietLopHoc/" + roomId;
        }


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
                // Thêm học sinh vào lớp
                ClassroomDetails classroomDetail = new ClassroomDetails(room, student);
                Events event = entityManager.find(Events.class, 10); // Giả định event ID 10 là sự kiện mặc định
                classroomDetail.setEvent(event);
                entityManager.persist(classroomDetail);

                // Gửi email thông báo cho học sinh
                String subject = "Chào Mừng Bạn Đến Với Hành Trình Học Tập Mới - " + room.getRoomName();

                String message = "<html><body style='font-family: Georgia, serif; line-height: 1.8; color: #333333; max-width: 700px; margin: 0 auto; background-color: #F5F6F5; padding: 20px;'>" +
                        "<p style='font-size: 18px; color: #154360;'><b>Thân gửi " + student.getFullName() + ", người bạn đồng hành mới đáng quý,</b></p>" +
                        "<p style='color: #4A4A4A;'>Trước tiên, chúng tôi xin gửi tới bạn một lời chào ấm áp, như ngọn gió xuân khẽ lùa qua những cánh hoa đang hé nở, mang theo niềm vui và hy vọng. Hôm nay là một ngày đặc biệt, ngày mà bạn chính thức trở thành một phần của đại gia đình học tập tại lớp <i style='color: #D35400;'>" + room.getRoomName() + "</i> trong Hệ Thống Quản Lý Giáo Dục - xAI Education.</p>" +
                        "<p style='color: #4A4A4A;'>Đây không chỉ là một sự khởi đầu, mà là một cánh cửa rộng mở dẫn bạn đến những chân trời tri thức mới, nơi mỗi bước đi của bạn sẽ được ghi dấu bằng sự nỗ lực, đam mê và những ước mơ rực rỡ. Chúng tôi vô cùng vinh dự được chào đón bạn, một mảnh ghép quan trọng, để cùng nhau viết nên những trang sử đẹp đẽ trong hành trình giáo dục đầy ý nghĩa này.</p>" +
                        "<p style='color: #4A4A4A;'>Để bạn có thể hình dung rõ hơn về ngôi nhà học tập mới của mình, chúng tôi xin phép gửi tới bạn đôi dòng thông tin như những nét phác thảo đầu tiên trên hành trình khám phá:</p>" +
                        "<ul style='list-style-type: none; padding-left: 20px; color: #4A4A4A;'>" +
                        "   <li><b style='color: #2E7D32;'>✦ Mã lớp học:</b> " + roomId + "</li>" +
                        "   <li><b style='color: #2E7D32;'>✦ Tên lớp học:</b> " + room.getRoomName() + "</li>" +
                        "   <li><b style='color: #2E7D32;'>✦ Thời gian gia nhập:</b> " + new java.util.Date() + "</li>" +
                        "   <li><b style='color: #2E7D32;'>✦ Thông điệp từ chúng tôi:</b> Chào mừng bạn đến với một hành trình đầy cảm hứng, nơi bạn sẽ tỏa sáng theo cách riêng của mình.</li>" +
                        "</ul>" +
                        "<p style='color: #4A4A4A;'>Chúng tôi tin rằng, với tài năng, nhiệt huyết và khát khao học hỏi của bạn, lớp học này sẽ trở thành một mảnh đất màu mỡ để bạn gieo những hạt giống tri thức, chờ ngày nở rộ thành những bông hoa rực rỡ. Nếu bạn có bất kỳ thắc mắc nào về hành trình sắp tới, hay chỉ đơn giản muốn trò chuyện để làm quen, xin đừng ngần ngại liên hệ với chúng tôi qua <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none; font-weight: bold;'>vuthanhtruong1280@gmail.com</a> hoặc số điện thoại <b style='color: #C0392B;'>0394444107</b>. Đội ngũ của chúng tôi luôn sẵn sàng đồng hành, lắng nghe và hỗ trợ bạn như những người bạn thân thiết nhất.</p>" +
                        "<p style='color: #4A4A4A;'>Thân gửi bạn, con đường học tập phía trước là một bản nhạc dài, và bạn chính là nghệ sĩ tài hoa sẽ tạo nên những giai điệu độc đáo của riêng mình. Chúng tôi hy vọng rằng, tại ngôi nhà mới này, bạn sẽ tìm thấy niềm vui, sự tự tin và động lực để chinh phục những đỉnh cao tri thức mà bạn hằng mơ ước.</p>" +
                        "<p style='color: #4A4A4A;'>Trước khi khép lại lá thư này, chúng tôi xin gửi tới bạn lời chúc nồng nhiệt nhất: Chúc bạn luôn mạnh mẽ như ngọn sóng biển, rực rỡ như ánh bình minh, và tràn đầy năng lượng để viết tiếp câu chuyện tuyệt vời của tuổi trẻ. Chào mừng bạn, một lần nữa, đến với <i style='color: #D35400;'>" + room.getRoomName() + "</i>!</p>" +
                        "<p style='margin-top: 30px; text-align: center; color: #7F8C8D;'><i>Trân trọng gửi tới bạn,</i></p>" +
                        "<p style='text-align: center; color: #34495E;'>" +
                        "<b>" + employee.getFirstName() + " " + employee.getLastName() + "</b><br>" +
                        "Quản Trị Viên Hệ Thống<br>" +
                        "Hệ Thống Quản Lý Giáo Dục - xAI Education<br>" +
                        "Email: <a href='mailto:vuthanhtruong1280@gmail.com' style='color: #2980B9; text-decoration: none;'>support@xaiedu.com</a> | Hotline: <span style='color: #C0392B;'>0394444107</span></p>" +
                        "</body></html>";

                sendEmail(student.getEmail(), subject, message);
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

    @PostMapping("/TimKiemGiaoVienCuaBan")
    public String timKiemGiaoVienCuaBan(@RequestParam("searchType") String searchType,
                                        @RequestParam("keyword") String keyword,
                                        ModelMap model) {
        List<Teachers> searchResults;

        if (searchType.equalsIgnoreCase("name")) {
            searchResults = entityManager.createQuery(
                            "SELECT t FROM Teachers t " +
                                    "WHERE LOWER(t.firstName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(t.lastName) LIKE LOWER(:keyword) " +
                                    "OR LOWER(CONCAT(t.firstName, ' ', t.lastName)) LIKE LOWER(:keyword)", Teachers.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
        } else if (searchType.equalsIgnoreCase("id")) {
            Teachers teacher = entityManager.find(Teachers.class, keyword);
            searchResults = (teacher != null) ? List.of(teacher) : List.of();
        } else {
            searchResults = List.of();
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("teachers", searchResults);
        return "DanhSachTimKiemGiaoVienCuaBan";
    }

    @PostMapping("/TimKiemHocSinhCuaBan")
    public String timKiemHocSinhCuaBan(@RequestParam("searchType") String searchType,
                                       @RequestParam("keyword") String keyword,
                                       ModelMap model) {
        List<Students> searchResults = List.of(); // Mặc định danh sách rỗng

        if (keyword != null && !keyword.trim().isEmpty()) {
            if ("id".equalsIgnoreCase(searchType)) {
                Students student = entityManager.find(Students.class, keyword);
                if (student != null) {
                    searchResults = List.of(student);
                }
            } else if ("name".equalsIgnoreCase(searchType)) {
                searchResults = entityManager.createQuery("""
                                SELECT s FROM Students s 
                                WHERE LOWER(s.firstName) LIKE :keyword 
                                   OR LOWER(s.lastName) LIKE :keyword
                                   OR LOWER(CONCAT(s.firstName, ' ', s.lastName)) LIKE :keyword
                                """, Students.class)
                        .setParameter("keyword", "%" + keyword.toLowerCase() + "%")
                        .getResultList();
            }
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("students", searchResults);
        return "DanhSachTimKiemHocSinhCuaBan";
    }

    @PostMapping("/DanhSachTimKiemPhongHoc")
    public String searchRooms(
            @RequestParam("searchType") String searchType,
            @RequestParam("keyword") String keyword,
            Model model) {

        List<Rooms> roomsOffline;
        List<OnlineRooms> roomsOnline;

        if ("id".equals(searchType)) {
            roomsOffline = entityManager.createQuery(
                            "FROM Rooms r WHERE r.roomId = :keyword", Rooms.class)
                    .setParameter("keyword", keyword)
                    .getResultList();

            roomsOnline = entityManager.createQuery(
                            "FROM OnlineRooms r WHERE r.roomId = :keyword", OnlineRooms.class)
                    .setParameter("keyword", keyword)
                    .getResultList();
        } else {
            roomsOffline = entityManager.createQuery(
                            "FROM Rooms r WHERE r.roomName LIKE :keyword", Rooms.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();

            roomsOnline = entityManager.createQuery(
                            "FROM OnlineRooms r WHERE r.roomName LIKE :keyword", OnlineRooms.class)
                    .setParameter("keyword", "%" + keyword + "%")
                    .getResultList();
        }

        model.addAttribute("roomsOffline", roomsOffline);
        model.addAttribute("roomsOnline", roomsOnline);
        model.addAttribute("keyword", keyword);

        return "DanhSachTimKiemPhongHoc"; // Đảm bảo có file HTML này trong templates
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


}
