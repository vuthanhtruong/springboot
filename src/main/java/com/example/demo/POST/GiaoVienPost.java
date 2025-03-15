package com.example.demo.POST;

import com.example.demo.OOP.Admin;
import com.example.demo.OOP.Employees;
import com.example.demo.OOP.Person;
import com.example.demo.OOP.Teachers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class GiaoVienPost {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @PostMapping("/DangKyGiaoVien")
    public String dangKyGiaoVien(@RequestParam("EmployeeID") String employeeID,
                                 @RequestParam("TeacherID") String teacherID,
                                 @RequestParam("FirstName") String firstName,
                                 @RequestParam("LastName") String lastName,
                                 @RequestParam("Email") String email,
                                 @RequestParam("PhoneNumber") String phoneNumber,
                                 @RequestParam("BirthDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                                 @RequestParam(value = "MisID", required = false) String misID,
                                 @RequestParam("Password") String password,
                                 @RequestParam("ConfirmPassword") String confirmPassword,
                                 Model model) {

        System.out.println("Bắt đầu đăng ký giáo viên...");

        // Kiểm tra TeacherID đã tồn tại
        if (entityManager.find(Person.class, teacherID) != null) {
            model.addAttribute("teacherIDError", "TeacherID đã tồn tại.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            model.addAttribute("passwordError", "Mật khẩu không khớp.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra tính hợp lệ của mật khẩu
        if (!isValidPassword(password)) {
            model.addAttribute("passwordInvalid", "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra ngày sinh có hợp lệ không (phải trong quá khứ)
        if (birthDate.isAfter(LocalDate.now())) {
            model.addAttribute("birthDateError", "Ngày sinh không hợp lệ.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra nếu Email đã tồn tại
        List<Person> existingTeachersByEmail = entityManager.createQuery("SELECT t FROM Person t WHERE t.email = :email", Person.class)
                .setParameter("email", email)
                .getResultList();
        if (!existingTeachersByEmail.isEmpty()) {
            model.addAttribute("emailError", "Email này đã được sử dụng.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra nếu Số điện thoại đã tồn tại
        List<Person> existingTeachersByPhone = entityManager.createQuery("SELECT t FROM Person t WHERE t.phoneNumber = :phoneNumber", Person.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList();
        if (!existingTeachersByPhone.isEmpty()) {
            model.addAttribute("phoneError", "Số điện thoại này đã được sử dụng.");
            return "DangKyGiaoVien";
        }

        // Lấy Admin
        List<Admin> adminList = entityManager.createQuery("FROM Admin", Admin.class).getResultList();
        if (adminList.isEmpty()) {
            model.addAttribute("adminError", "Không tìm thấy Admin.");
            return "DangKyGiaoVien";
        }
        Admin admin = adminList.get(0);

        // Lấy Employee
        Employees employee = entityManager.find(Employees.class, employeeID);
        if (employee == null) {
            model.addAttribute("employeeError", "Employee ID không hợp lệ.");
            return "DangKyGiaoVien";
        }

        // Tạo giáo viên mới
        Teachers giaoVien = new Teachers();
        giaoVien.setId(teacherID);
        giaoVien.setFirstName(firstName);
        giaoVien.setLastName(lastName);
        giaoVien.setEmail(email);
        giaoVien.setPhoneNumber(phoneNumber);
        giaoVien.setBirthDate(birthDate); // Lưu ngày sinh
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password); // Nên mã hóa mật khẩu trước khi lưu
        giaoVien.setEmployee(employee);
        giaoVien.setAdmin(admin);

        try {
            entityManager.persist(giaoVien);
            System.out.println("Đăng ký giáo viên thành công!");
        } catch (Exception e) {
            System.out.println("Lỗi khi lưu giáo viên: " + e.getMessage());
            model.addAttribute("databaseError", "Lỗi khi lưu dữ liệu.");
            return "DangKyGiaoVien";
        }

        return "redirect:/TrangChu";
    }


    private boolean isValidPassword(String password) {
        // Mật khẩu phải có ít nhất 8 ký tự, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(passwordRegex);
    }

}

