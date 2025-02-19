package com.example.demo.POST;

import com.example.demo.OOP.Admin;
import com.example.demo.OOP.Employees;
import com.example.demo.OOP.Students;
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

import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class StudentPost {
    @PersistenceContext
    private EntityManager entityManager;
    @PostMapping("/DangKyHocSinh")
    public String DangKyHocSinh(@RequestParam("EmployeeID") String employeeID,
                                @RequestParam("StudentID") String studentID,
                                @RequestParam("FirstName") String firstName,
                                @RequestParam("LastName") String lastName,
                                @RequestParam("Email") String email,
                                @RequestParam("PhoneNumber") String phoneNumber,
                                @RequestParam(value = "MisID", required = false) String misID,
                                @RequestParam("Password") String password,
                                @RequestParam("ConfirmPassword") String confirmPassword) {

        // Kiểm tra mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            return "redirect:/DangKyHocSinh?error=password_mismatch";
        }


        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = admins.get(0);  // Lấy trực tiếp đối tượng Admin từ danh sách
        Employees employee = entityManager.find(Employees.class, employeeID);

        if (employee == null) {
            return "redirect:/DangKyHocSinh?error=invalid_employee";
        }

        // Kiểm tra xem StudentID đã tồn tại chưa
        if (entityManager.find(Students.class, studentID) != null) {
            return "redirect:/DangKyHocSinh?error=student_exists";
        }

        Students student = new Students();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setPassword(password);
        student.setId(studentID); // Không cần thêm "STU", vì đã kiểm tra trước đó
        student.setAdmin(admin);
        student.setEmployee(employee);
        student.setMisId(misID);

        entityManager.persist(student);

        return "redirect:/DangNhapHocSinh";
    }

    @PostMapping("/DangNhapHocSinh")
    public String DangNhapHocSinh(@RequestParam("studentID") long studentID,
                                  @RequestParam("password") String password,
                                  ModelMap model,
                                  HttpSession session) {
        try {
            Students student = entityManager.find(Students.class, studentID);

            if (student != null && student.getPassword().equals(password)) {
                session.setAttribute("StudentID", student.getId());
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


}
