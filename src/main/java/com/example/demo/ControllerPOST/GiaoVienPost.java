package com.example.demo.ControllerPOST;

import com.example.demo.ModelOOP.*;
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

    @PostMapping("/DangKyGiaoVien")
    @Transactional(rollbackOn = Exception.class)
    public String dangKyGiaoVien(
            @RequestParam("TeacherID") String teacherID,
            @RequestParam("FirstName") String firstName,
            @RequestParam("LastName") String lastName,
            @RequestParam("Email") String email,
            @RequestParam("PhoneNumber") String phoneNumber,
            @RequestParam("BirthDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
            @RequestParam("Gender") Gender gender,
            @RequestParam(value = "Country", required = false) String country,
            @RequestParam(value = "Province", required = false) String province,
            @RequestParam(value = "District", required = false) String district,
            @RequestParam(value = "Ward", required = false) String ward,
            @RequestParam(value = "Street", required = false) String street,
            @RequestParam(value = "PostalCode", required = false) String postalCode,
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

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("^[0-9]+$")) {
            model.addAttribute("phoneError", "Số điện thoại chỉ được chứa chữ số!");
            return "DangKyGiaoVien";
        } else if (!phoneNumber.matches("^\\d{9,10}$")) {
            model.addAttribute("phoneError", "Số điện thoại phải có 9-10 chữ số!");
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

        // Tìm nhân viên có ít giáo viên nhất
        Employees selectedEmployee = findEmployeeWithFewestTeachers();
        if (selectedEmployee == null) {
            model.addAttribute("employeeError", "Không tìm thấy nhân viên nào để phân bổ.");
            return "DangKyGiaoVien";
        }

        // Tạo giáo viên mới
        Teachers giaoVien = new Teachers();
        giaoVien.setId(teacherID);
        giaoVien.setFirstName(firstName);
        giaoVien.setLastName(lastName);
        giaoVien.setEmail(email);
        giaoVien.setPhoneNumber(phoneNumber);
        giaoVien.setBirthDate(birthDate);
        giaoVien.setGender(gender);
        giaoVien.setCountry(country); // Lưu quốc gia
        giaoVien.setProvince(province); // Lưu tỉnh/thành phố
        giaoVien.setDistrict(district); // Lưu quận/huyện
        giaoVien.setWard(ward); // Lưu xã/phường
        giaoVien.setStreet(street); // Lưu đường, số nhà
        giaoVien.setPostalCode(postalCode); // Lưu mã bưu điện
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password); // Mã hóa mật khẩu
        giaoVien.setEmployee(selectedEmployee);
        giaoVien.setAdmin(admin);

        // Lưu giáo viên
        entityManager.persist(giaoVien);
        System.out.println("Đăng ký giáo viên thành công! Gán cho nhân viên: " + selectedEmployee.getId());

        return "redirect:/TrangChu";
    }

    // Phương thức phụ để tìm nhân viên có ít giáo viên nhất
    private Employees findEmployeeWithFewestTeachers() {
        // Lấy tất cả nhân viên
        List<Employees> employeesList = entityManager.createQuery("FROM Employees", Employees.class).getResultList();
        if (employeesList.isEmpty()) {
            return null;
        }

        Employees selectedEmployee = null;
        Long minTeacherCount = Long.MAX_VALUE;

        // Duyệt qua từng nhân viên và đếm số giáo viên họ quản lý
        for (Employees employee : employeesList) {
            Long teacherCount = entityManager.createQuery(
                            "SELECT COUNT(t) FROM Teachers t WHERE t.employee = :employee", Long.class)
                    .setParameter("employee", employee)
                    .getSingleResult();

            if (teacherCount < minTeacherCount) {
                minTeacherCount = teacherCount;
                selectedEmployee = employee;
            }
        }

        return selectedEmployee;
    }

    // Hàm kiểm tra tính hợp lệ của mật khẩu
    private boolean isValidPassword(String password) {
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }
}

