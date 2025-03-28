package com.example.demo.ControllerPOST;

import com.example.demo.ModelOOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class StudentPost {
    private static final Logger log = LoggerFactory.getLogger(GiaoVienPost.class);
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/DangKyHocSinh")
    @Transactional(rollbackOn = Exception.class)
    public String dangKyHocSinh(
            @RequestParam("StudentID") String studentID,
            @RequestParam("FirstName") String firstName,
            @RequestParam("LastName") String lastName,
            @RequestParam("Email") String email,
            @RequestParam("PhoneNumber") String phoneNumber,
            @RequestParam("BirthDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @RequestParam(value = "MisId", required = false) String misId,
            @RequestParam("Password") String password,
            @RequestParam("ConfirmPassword") String confirmPassword,
            @RequestParam("Gender") Gender gender,
            @RequestParam(value = "Country", required = false) String country,
            @RequestParam(value = "Province", required = false) String province,
            @RequestParam(value = "District", required = false) String district,
            @RequestParam(value = "Ward", required = false) String ward,
            @RequestParam(value = "Street", required = false) String street,
            @RequestParam(value = "PostalCode", required = false) String postalCode,
            RedirectAttributes redirectAttributes) {

        System.out.println("Bắt đầu đăng ký học sinh...");

        // Kiểm tra mật khẩu khớp nhau
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Mật khẩu nhập lại không khớp!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra StudentID đã tồn tại chưa
        if (entityManager.find(Person.class, studentID) != null) {
            redirectAttributes.addFlashAttribute("errorStudentID", "Mã học sinh đã tồn tại!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra Email có tồn tại chưa
        boolean emailExists = !entityManager.createQuery(
                        "SELECT s FROM Person s WHERE s.email = :email", Person.class)
                .setParameter("email", email)
                .getResultList().isEmpty();
        if (emailExists) {
            redirectAttributes.addFlashAttribute("errorEmail", "Email đã tồn tại!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("^[0-9]+$")) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại chỉ được chứa chữ số!");
            return "redirect:/DangKyHocSinh";
        } else if (!phoneNumber.matches("^\\d{9,10}$")) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại phải có 9-10 chữ số!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra số điện thoại có tồn tại chưa
        boolean phoneExists = !entityManager.createQuery(
                        "SELECT s FROM Person s WHERE s.phoneNumber = :phoneNumber", Person.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList().isEmpty();
        if (phoneExists) {
            redirectAttributes.addFlashAttribute("errorPhone", "Số điện thoại đã được đăng ký!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra mật khẩu có đủ mạnh không
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{8,}$")) {
            redirectAttributes.addFlashAttribute("errorPassword", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra ngày sinh có hợp lệ không (phải trong quá khứ)
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            redirectAttributes.addFlashAttribute("errorBirthDate", "Ngày sinh không hợp lệ!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra độ tuổi hợp lệ (ít nhất 6 tuổi)
        if (ChronoUnit.YEARS.between(birthDate, today) < 6) {
            redirectAttributes.addFlashAttribute("errorBirthDate", "Học sinh phải từ 6 tuổi trở lên!");
            return "redirect:/DangKyHocSinh";
        }

        // Lấy Admin
        List<Admin> adminList = entityManager.createQuery("FROM Admin", Admin.class).getResultList();
        if (adminList.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorAdmin", "Không tìm thấy Admin!");
            return "redirect:/DangKyHocSinh";
        }
        Admin admin = adminList.get(0);

        // Tìm nhân viên có ít học sinh nhất
        Employees selectedEmployee = findEmployeeWithFewestStudents();
        if (selectedEmployee == null) {
            redirectAttributes.addFlashAttribute("errorEmployee", "Không tìm thấy nhân viên nào để phân bổ!");
            return "redirect:/DangKyHocSinh";
        }

        // Tạo đối tượng học sinh
        Students student = new Students();
        student.setId(studentID);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setBirthDate(birthDate);
        student.setMisId(misId);
        student.setPassword(password); // Mã hóa mật khẩu
        student.setGender(gender);
        student.setCountry(country); // Lưu quốc gia
        student.setProvince(province); // Lưu tỉnh/thành phố
        student.setDistrict(district); // Lưu quận/huyện
        student.setWard(ward); // Lưu xã/phường
        student.setStreet(street); // Lưu đường, số nhà
        student.setPostalCode(postalCode); // Lưu mã bưu điện
        student.setEmployee(selectedEmployee);
        student.setAdmin(admin);

        // Lưu vào database
        try {
            entityManager.persist(student);
            System.out.println("Đăng ký học sinh thành công! Gán cho nhân viên: " + selectedEmployee.getId());
        } catch (Exception e) {
            System.out.println("Lỗi khi lưu học sinh: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu dữ liệu: " + e.getMessage());
            return "redirect:/DangKyHocSinh";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/TrangChu";
    }

    // Phương thức phụ để tìm nhân viên có ít học sinh nhất
    private Employees findEmployeeWithFewestStudents() {
        // Lấy tất cả nhân viên
        List<Employees> employeesList = entityManager.createQuery("FROM Employees", Employees.class).getResultList();
        if (employeesList.isEmpty()) {
            return null;
        }

        Employees selectedEmployee = null;
        Long minStudentCount = Long.MAX_VALUE;

        // Duyệt qua từng nhân viên và đếm số học sinh họ quản lý
        for (Employees employee : employeesList) {
            Long studentCount = entityManager.createQuery(
                            "SELECT COUNT(s) FROM Students s WHERE s.employee = :employee", Long.class)
                    .setParameter("employee", employee)
                    .getSingleResult();

            if (studentCount < minStudentCount) {
                minStudentCount = studentCount;
                selectedEmployee = employee;
            }
        }

        return selectedEmployee;
    }

    @PostMapping("/GuiNhanXetGiaoVien")
    @Transactional
    public String guiNhanXetGiaoVien(@RequestParam("teacherId") String teacherId,
                                     @RequestParam("text") String text,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            log.info("📝 Nhận xét giáo viên với ID: {}", teacherId);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String studentId = authentication.getName();
            Person person = entityManager.find(Person.class, studentId);
            Students student = (Students) person; // Lấy ID học sinh từ session
            // Lấy thông tin giáo viên từ database
            Teachers teacher = entityManager.find(Teachers.class, teacherId);
            if (teacher == null) {
                throw new IllegalArgumentException("Không tìm thấy giáo viên với ID: " + teacherId);
            }
            // Tạo nhận xét mới
            Feedbacks feedback = new Feedbacks();
            feedback.setReviewer(student);
            feedback.setTeacher(teacher);
            feedback.setReceiver(student.getEmployee());
            feedback.setText(text);
            feedback.setCreatedAt(LocalDateTime.now());

            Events events = entityManager.find(Events.class, 3);
            feedback.setEvent(events);

            // Lưu vào database
            entityManager.persist(feedback);
            log.info("✅ Nhận xét đã được lưu thành công.");

            // Gửi thông báo về giao diện
            redirectAttributes.addFlashAttribute("message", "Nhận xét của bạn đã được gửi!");

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi nhận xét: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra, vui lòng thử lại.");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return "redirect:/TrangChuHocSinh";
    }

}
