package com.example.demo.POST;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/")
@Transactional
public class StudentPost {
    private static final Logger log = LoggerFactory.getLogger(GiaoVienPost.class);
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/DangKyHocSinh")
    public String DangKyHocSinh(
            @RequestParam("EmployeeID") String employeeID,
            @RequestParam("StudentID") String studentID,
            @RequestParam("FirstName") String firstName,
            @RequestParam("LastName") String lastName,
            @RequestParam("Email") String email,
            @RequestParam("PhoneNumber") String phoneNumber,
            @RequestParam("BirthDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate, // Thêm ngày sinh
            @RequestParam(value = "MisId", required = false) String misId,
            @RequestParam("Password") String password,
            @RequestParam("ConfirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes) {

        // Kiểm tra mật khẩu khớp nhau
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorPassword", "Mật khẩu nhập lại không khớp!");
            return "redirect:/DangKyHocSinh";
        }

        // Kiểm tra nhân viên có tồn tại không
        Employees employee = entityManager.find(Employees.class, employeeID);
        if (employee == null) {
            redirectAttributes.addFlashAttribute("errorEmployee", "Mã nhân viên không hợp lệ!");
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

        // Kiểm tra độ tuổi hợp lệ (ví dụ: ít nhất 6 tuổi)
        LocalDate today = LocalDate.now();
        if (ChronoUnit.YEARS.between(birthDate, today) < 6) {
            redirectAttributes.addFlashAttribute("errorBirthDate", "Học sinh phải từ 6 tuổi trở lên!");
            return "redirect:/DangKyHocSinh";
        }

        // Lấy admin (giả sử chỉ có một admin)
        Admin admin = entityManager.createQuery("FROM Admin", Admin.class)
                .setMaxResults(1)
                .getSingleResult();

        // Tạo đối tượng học sinh
        Students student = new Students();
        student.setId(studentID);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setBirthDate(birthDate); // Thêm ngày sinh
        student.setPassword(password);
        student.setMisId(misId);
        student.setEmployee(employee);
        student.setAdmin(admin);

        // Lưu vào database
        entityManager.persist(student);

        // Chuyển hướng đến trang đăng nhập với thông báo thành công
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/TrangChu";
    }

    @PostMapping("/DangNhapHocSinh")
    public String DangNhapHocSinh(@RequestParam("studentID") String studentID,
                                  @RequestParam("password") String password,
                                  ModelMap model,
                                  HttpSession session) {
        try {
            Students student = entityManager.find(Students.class, studentID);

            if (student != null && student.getPassword().equals(password)) {
                return "redirect:/TrangChuHocSinh";
            } else {
                model.addAttribute("error", "Mã học sinh hoặc mật khẩu không đúng!");
                return "redirect:/DangNhapHocSinh";
            }
        } catch (NoResultException e) {
            model.addAttribute("error", "Mã học sinh không tồn tại!");
            return "redirect:/DangNhapHocSinh";
        }
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

    @PostMapping("/LuuThongTinHocSinh")
    public String luuThongTinHocSinh(@RequestParam String firstName,
                                     @RequestParam String lastName,
                                     @RequestParam String email,
                                     @RequestParam String phoneNumber,
                                     HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String studentId = authentication.getName();
        Person person = entityManager.find(Person.class, studentId);
        Students student = (Students) person;

        // Cập nhật thông tin học sinh
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        entityManager.merge(student);

        return "redirect:/TrangChuHocSinh"; // Tải lại trang cá nhân với thông tin mới
    }


}
