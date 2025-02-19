package com.example.demo.POST;
import com.example.demo.OOP.*;
import com.mysql.cj.protocol.Message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class GiaoVienPost {

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/DangKyGiaoVien")
    public String dangKyGiaoVien(@RequestParam("EmployeeID") String employeeID,
                                 @RequestParam("TeacherID") String teacherID,
                                 @RequestParam("FirstName") String firstName,
                                 @RequestParam("LastName") String lastName,
                                 @RequestParam("Email") String email,
                                 @RequestParam("PhoneNumber") String phoneNumber,
                                 @RequestParam(value = "MisID", required = false) String misID,
                                 @RequestParam("Password") String password,
                                 @RequestParam("ConfirmPassword") String confirmPassword) {

        // Kiểm tra mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            return "redirect:/DangKyGiaoVien?error=passwordsNotMatch";
        }
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = admins.get(0);  // Lấy trực tiếp đối tượng Admin từ danh sách
        Employees employee = entityManager.find(Employees.class, employeeID);

        // Kiểm tra nếu TeacherID đã tồn tại
        if (entityManager.find(Teachers.class, teacherID) != null) {
            return "redirect:/DangKyGiaoVien?error=teacherIDExists";
        }

        // Tạo giáo viên mới
        Teachers giaoVien = new Teachers();
        giaoVien.setEmployee(employee);
        giaoVien.setAdmin(admin);
        giaoVien.setId(teacherID);
        giaoVien.setFirstName(firstName);
        giaoVien.setLastName(lastName);
        giaoVien.setEmail(email);
        giaoVien.setPhoneNumber(phoneNumber);
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password);
        entityManager.persist(giaoVien);

        return "redirect:/DangNhapGiaoVien";
    }

    @PostMapping("/DangNhapGiaoVien")
    public String DangNhapGiaoVien(@RequestParam("TeacherID") String teacherID,
                                   @RequestParam("Password") String password,
                                   ModelMap model,
                                   HttpSession session) {
        try {
            Teachers teacher = entityManager.createQuery(
                            "SELECT t FROM Teachers t WHERE t.id = :teacherID", Teachers.class)
                    .setParameter("teacherID", teacherID)
                    .getSingleResult();

            if (teacher != null && teacher.getPassword().equals(password)) {
                session.setAttribute("TeacherID", teacher.getId()); // ✅ Lưu ID vào session
                return "redirect:/TrangChuGiaoVien"; // ✅ Đăng nhập thành công
            } else {
                model.addAttribute("error", "Mã giáo viên hoặc mật khẩu không đúng!");
                return "DangNhapGiaoVien"; // ❌ Không dùng redirect để giữ thông báo lỗi
            }
        } catch (NoResultException e) {
            model.addAttribute("error", "Mã giáo viên không tồn tại!");
            return "DangNhapGiaoVien";
        }
    }
    @PostMapping("/ChiTietTinNhanCuaGiaoVien")
    @Transactional
    public String handleMessage(@RequestParam("studentID") String studentID,
                                @RequestParam("teacherID") String teacherID,
                                @RequestParam("messageText") String messageText,
                                ModelMap model) {
        // Kiểm tra dữ liệu đầu vào
        if (messageText == null || messageText.trim().isEmpty()) {
            model.addAttribute("error", "Nội dung tin nhắn không được để trống!");
            return "redirect:/ChiTietTinNhanCuaGiaoVien/" + studentID;
        }

        // Tìm sender (giáo viên) và recipient (học sinh) trong database
        Person sender = entityManager.find(Person.class, teacherID);
        Person recipient = entityManager.find(Person.class, studentID);

        // Kiểm tra xem cả hai người có tồn tại hay không
        if (sender == null || recipient == null) {
            model.addAttribute("error", "Không tìm thấy giáo viên hoặc học sinh.");
            return "redirect:/ChiTietTinNhanCuaGiaoVien/" + studentID;
        }

        try {
            // Tạo tin nhắn mới
            Messages message = new Messages();
            message.setSender(sender);
            message.setRecipient(recipient);
            message.setText(messageText.trim());
            message.setDatetime(LocalDateTime.now());

            // Lưu vào cơ sở dữ liệu
            entityManager.persist(message);
        } catch (Exception e) {
            model.addAttribute("error", "Gửi tin nhắn thất bại, vui lòng thử lại.");
        }

        // Chuyển hướng đến trang chi tiết tin nhắn
        return "redirect:/ChiTietTinNhanCuaGiaoVien/" + studentID;
    }




}

