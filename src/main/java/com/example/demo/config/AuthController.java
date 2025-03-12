package com.example.demo.config;

import com.example.demo.OOP.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
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
        session.setAttribute("otp", otp);
        session.setAttribute("email", email);
        return "NhapOTP";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp, Model model, HttpSession session) {
        String email = (String) session.getAttribute("email");
        if (email == null || otp == null || !isValidOtp(email, otp)) {
            model.addAttribute("error", "OTP không hợp lệ hoặc đã hết hạn!");
            return "NhapOTP";
        }

        session.setAttribute("otpVerified", true);
        return "DatLaiMatKhau";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model, HttpSession session) {
        String email = (String) session.getAttribute("email");
        Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");

        if (email == null || otpVerified == null || !otpVerified) {
            return "redirect:/auth/QuenMatKhau";
        }

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "DatLaiMatKhau";
        }

        Person person = findPersonByEmail(email);
        if (person == null) {
            model.addAttribute("error", "Email không hợp lệ!");
            return "DatLaiMatKhau";
        }

        updatePassword(person, newPassword);
        deleteUsedTokens(email);
        session.invalidate();
        return "redirect:/TrangChu";
    }

    private void updatePassword(Person person, String newPassword) {
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
        }
        entityManager.flush();
    }

    private Person findPersonByEmail(String email) {
        try {
            return entityManager.createQuery("SELECT p FROM Person p WHERE p.email = :email", Person.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private void savePasswordResetToken(Person person, String otp) {
        entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.person.id = :personId")
                .setParameter("personId", person.getId())
                .executeUpdate();

        PasswordResetToken token = new PasswordResetToken();
        token.setOtp(otp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        token.setPerson(person);
        entityManager.persist(token);
    }

    private boolean isValidOtp(String email, String otp) {
        try {
            Long count = entityManager.createQuery(
                            "SELECT COUNT(t) FROM PasswordResetToken t " +
                                    "WHERE t.person.email = :email AND t.otp = :otp " +
                                    "AND t.expiryDate > :now", Long.class)
                    .setParameter("email", email)
                    .setParameter("otp", otp)
                    .setParameter("now", LocalDateTime.now())
                    .getSingleResult();
            return count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteUsedTokens(String email) {
        entityManager.createQuery("DELETE FROM PasswordResetToken t WHERE t.person.email = :email")
                .setParameter("email", email)
                .executeUpdate();
    }

    private void sendEmail(String email, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("Mã OTP đặt lại mật khẩu");
            String emailContent = "<html><body>" +
                    "<h2>Yêu cầu đặt lại mật khẩu</h2>" +
                    "<p>Mã OTP của bạn là: <strong>" + otp + "</strong></p>" +
                    "<p>OTP có hiệu lực trong 10 phút.</p>" +
                    "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>" +
                    "</body></html>";
            helper.setText(emailContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
