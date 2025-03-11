package com.example.demo.config;

import com.example.demo.OOP.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Random;

@Controller
@RequestMapping("/auth")
@Transactional
public class AuthController {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JavaMailSender mailSender;

    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    @GetMapping("/QuenMatKhau")
    public String quenMatKhau() {
        return "QuenMatKhau";
    }

    @PostMapping("/QuenMatKhau")
    public String forgotPassword(@RequestParam String email, Model model, HttpSession session) {
        Person person = findPersonByEmail(email);
        if (person == null) {
            model.addAttribute("error", "Email không tồn tại!");
            return "QuenMatKhau";
        }

        String otp = generateOTP();
        savePasswordResetToken(person, otp);
        sendEmail(person.getEmail(), otp);

        // Lưu email vào session để sử dụng ở các bước tiếp theo
        session.setAttribute("reset_email", email);

        return "redirect:/auth/verify-otp";
    }

    @GetMapping("/verify-otp")
    public String verifyOtpForm(HttpSession session, Model model) {
        String email = (String) session.getAttribute("reset_email");
        if (email == null) {
            return "redirect:/auth/QuenMatKhau";
        }

        model.addAttribute("email", email);
        return "NhapOTP";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String otp,
            Model model,
            HttpSession session) {

        String email = (String) session.getAttribute("reset_email");
        if (email == null) {
            return "redirect:/auth/QuenMatKhau";
        }

        if (!isValidOtp(email, otp)) {
            model.addAttribute("error", "OTP không hợp lệ hoặc đã hết hạn!");
            model.addAttribute("email", email);
            return "NhapOTP";
        }

        // Đánh dấu OTP đã xác thực
        session.setAttribute("otp_verified", true);

        return "redirect:/auth/DatLaiMatKhau";
    }

    @GetMapping("/DatLaiMatKhau")
    public String datLaiMatKhau(HttpSession session, Model model) {
        String email = (String) session.getAttribute("reset_email");
        Boolean otpVerified = (Boolean) session.getAttribute("otp_verified");

        if (email == null || otpVerified == null || !otpVerified) {
            return "redirect:/auth/QuenMatKhau";
        }
        model.addAttribute("email", email);
        return "DatLaiMatKhau";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            Model model,
            HttpSession session) {

        String email = (String) session.getAttribute("reset_email");
        Boolean otpVerified = (Boolean) session.getAttribute("otp_verified");

        if (email == null || otpVerified == null || !otpVerified) {
            return "redirect:/auth/QuenMatKhau";
        }

        // Kiểm tra mật khẩu và xác nhận mật khẩu có khớp không
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            model.addAttribute("email", email);
            return "DatLaiMatKhau";
        }

        Person person = findPersonByEmail(email);
        if (person == null) {
            model.addAttribute("error", "Email không hợp lệ!");
            return "DatLaiMatKhau";
        }

        try {
            if (person instanceof Students student) {
                student.setPassword(newPassword);
                entityManager.merge(student);
            } else if (person instanceof Teachers teacher) {
                teacher.setPassword(newPassword);
                entityManager.merge(teacher);
            } else if (person instanceof Employees employee) {
                employee.setPassword(newPassword);
                entityManager.merge(employee);
            } else if (person instanceof Admin admin) {
                admin.setPassword(newPassword);
                entityManager.merge(admin);
            } else {
                model.addAttribute("error", "Không thể đặt lại mật khẩu!");
                return "DatLaiMatKhau";
            }

            entityManager.flush();

            // Xóa token đã sử dụng
            deleteUsedTokens(email);

            // Xóa dữ liệu trong session
            session.removeAttribute("reset_email");
            session.removeAttribute("otp_verified");

            // Thông báo thành công
            return "redirect:/login?resetSuccess=true";
        } catch (Exception e) {
            model.addAttribute("error", "Đã xảy ra lỗi khi cập nhật mật khẩu!");
            model.addAttribute("email", email);
            return "DatLaiMatKhau";
        }
    }

    private Person findPersonByEmail(String email) {
        try {
            Query query = entityManager.createQuery(
                    "SELECT p FROM Person p WHERE p.email = :email", Person.class);
            query.setParameter("email", email);
            return (Person) query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private void savePasswordResetToken(Person person, String otp) {
        // Xóa token cũ nếu có
        Query deleteQuery = entityManager.createQuery(
                "DELETE FROM PasswordResetToken t WHERE t.person.id = :personId");
        deleteQuery.setParameter("personId", person.getId());
        deleteQuery.executeUpdate();

        // Tạo token mới
        PasswordResetToken token = new PasswordResetToken();
        token.setOtp(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // 10 phút hiệu lực
        token.setPerson(person);
        entityManager.persist(token);
    }

    private boolean isValidOtp(String email, String otp) {
        try {
            Query query = entityManager.createQuery(
                    "SELECT COUNT(t) FROM PasswordResetToken t " +
                            "WHERE t.person.email = :email AND t.otp = :otp " +
                            "AND t.expiryDate > :now"
            );
            query.setParameter("email", email);
            query.setParameter("otp", otp);
            query.setParameter("now", LocalDateTime.now());

            Long count = (Long) query.getSingleResult();
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteUsedTokens(String email) {
        Query deleteQuery = entityManager.createQuery(
                "DELETE FROM PasswordResetToken t WHERE t.person.email = :email");
        deleteQuery.setParameter("email", email);
        deleteQuery.executeUpdate();
    }

    private void sendEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Mã OTP đặt lại mật khẩu");
            String emailContent =
                    "<html><body>" +
                            "<h2>Yêu cầu đặt lại mật khẩu</h2>" +
                            "<p>Mã OTP của bạn là: <strong>" + otp + "</strong></p>" +
                            "<p>OTP có hiệu lực trong 10 phút.</p>" +
                            "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>" +
                            "</body></html>";
            helper.setText(emailContent, true); // Set true để gửi email dạng HTML
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}