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
        if (entityManager.find(Persons.class, teacherID) != null) {
            model.addAttribute("teacherIDError", "TeacherID đã tồn tại.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Kiểm tra mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            model.addAttribute("passwordError", "Mật khẩu không khớp.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Kiểm tra tính hợp lệ của mật khẩu
        if (!isValidPassword(password)) {
            model.addAttribute("passwordInvalid", "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Kiểm tra ngày sinh có hợp lệ không (phải trong quá khứ)
        if (birthDate.isAfter(LocalDate.now())) {
            model.addAttribute("birthDateError", "Ngày sinh không hợp lệ.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Kiểm tra nếu Email đã tồn tại
        List<Persons> existingTeachersByEmail = entityManager.createQuery("SELECT t FROM Persons t WHERE t.email = :email", Persons.class)
                .setParameter("email", email)
                .getResultList();
        if (!existingTeachersByEmail.isEmpty()) {
            model.addAttribute("emailError", "Email này đã được sử dụng.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("^[0-9]+$")) {
            model.addAttribute("phoneError", "Số điện thoại chỉ được chứa chữ số!");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        } else if (!phoneNumber.matches("^\\d{9,10}$")) {
            model.addAttribute("phoneError", "Số điện thoại phải có 9-10 chữ số!");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Kiểm tra nếu Số điện thoại đã tồn tại
        List<Persons> existingTeachersByPhone = entityManager.createQuery("SELECT t FROM Persons t WHERE t.phoneNumber = :phoneNumber", Persons.class)
                .setParameter("phoneNumber", phoneNumber)
                .getResultList();
        if (!existingTeachersByPhone.isEmpty()) {
            model.addAttribute("phoneError", "Số điện thoại này đã được sử dụng.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Lấy Admin
        List<Admin> adminList = entityManager.createQuery("FROM Admin", Admin.class).getResultList();
        if (adminList.isEmpty()) {
            model.addAttribute("adminError", "Không tìm thấy Admin.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }
        Admin admin = adminList.get(0);

        // Kiểm tra xem có nhân viên nào trong hệ thống không
        List<Staffs> employeeList = entityManager.createQuery("FROM Staffs", Staffs.class).getResultList();
        if (employeeList.isEmpty()) {
            model.addAttribute("employeeError", "Chưa có nhân viên, không thể đăng ký được.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
            return "DangKyGiaoVien";
        }

        // Tìm nhân viên có ít giáo viên nhất
        Staffs selectedEmployee = findEmployeeWithFewestTeachers();
        if (selectedEmployee == null) {
            model.addAttribute("employeeError", "Không tìm thấy nhân viên nào để phân bổ.");
            addFormDataToModel(model, teacherID, firstName, lastName, email, phoneNumber, birthDate, gender, country, province, district, ward, street, postalCode, misID, password, confirmPassword);
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
        giaoVien.setCountry(country);
        giaoVien.setProvince(province);
        giaoVien.setCity(province);
        giaoVien.setDistrict(district);
        giaoVien.setWard(ward);
        giaoVien.setStreet(street);
        giaoVien.setPostalCode(postalCode);
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password); // Mã hóa mật khẩu
        giaoVien.setEmployee(selectedEmployee);
        giaoVien.setAdmin(admin);

        // Lưu giáo viên
        entityManager.persist(giaoVien);
        System.out.println("Đăng ký giáo viên thành công! Gán cho nhân viên: " + selectedEmployee.getId());

        return "redirect:/TrangChu";
    }

    // Phương thức hỗ trợ để gửi dữ liệu vào model
    private void addFormDataToModel(Model model, String teacherID, String firstName, String lastName,
                                    String email, String phoneNumber, LocalDate birthDate, Gender gender,
                                    String country, String province, String district, String ward, String street,
                                    String postalCode, String misID, String password, String confirmPassword) {
        model.addAttribute("TeacherID", teacherID);
        model.addAttribute("FirstName", firstName);
        model.addAttribute("LastName", lastName);
        model.addAttribute("Email", email);
        model.addAttribute("PhoneNumber", phoneNumber);
        model.addAttribute("BirthDate", birthDate);
        model.addAttribute("Gender", gender);
        model.addAttribute("Country", country);
        model.addAttribute("Province", province);
        model.addAttribute("District", district);
        model.addAttribute("Ward", ward);
        model.addAttribute("Street", street);
        model.addAttribute("PostalCode", postalCode);
        model.addAttribute("MisID", misID);
        model.addAttribute("Password", password);
        model.addAttribute("ConfirmPassword", confirmPassword);
    }

    // Phương thức phụ để tìm nhân viên có ít giáo viên nhất
    private Staffs findEmployeeWithFewestTeachers() {
        // Lấy tất cả nhân viên
        List<Staffs> employeesList = entityManager.createQuery("FROM Staffs", Staffs.class).getResultList();
        if (employeesList.isEmpty()) {
            return null;
        }

        Staffs selectedEmployee = null;
        Long minTeacherCount = Long.MAX_VALUE;

        // Duyệt qua từng nhân viên và đếm số giáo viên họ quản lý
        for (Staffs employee : employeesList) {
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

