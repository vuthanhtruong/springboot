package com.example.demo.POST;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/")
@Transactional
public class TrangCaNhanPost {

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/LuuThongTinCaNhan")
    public String luuThongTinCaNhan(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            HttpSession session,
            ModelMap model, RedirectAttributes redirectAttributes) {

        // Kiểm tra dữ liệu đầu vào
        if (firstName == null || firstName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("firstNameInvalid", "Họ không được để trống");
            return "redirect:/TrangCaNhan";
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("lastNameInvalid", "Tên không được để trống");
            return "redirect:/TrangCaNhan";
        }
        if (email == null || email.trim().isEmpty() || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            redirectAttributes.addFlashAttribute("emailInvalid", "Email không hợp lệ");
            return "redirect:/TrangCaNhan";
        }

        // Kiểm tra định dạng số điện thoại
        if (!phoneNumber.matches("^[0-9]+$")) { // Kiểm tra toàn số
            redirectAttributes.addFlashAttribute("phoneNumberInvalid", "Số điện thoại chỉ được chứa chữ số!");
            return "redirect:/TrangCaNhan";
        } else if (!phoneNumber.matches("^\\d{9,10}$")) { // Kiểm tra độ dài
            redirectAttributes.addFlashAttribute("phoneNumberInvalid", "Số điện thoại phải có 9-10 chữ số!");
            return "redirect:/TrangCaNhan";
        }

        // Lấy ID từ Authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("notLoggedIn", "Bạn cần đăng nhập để thực hiện thao tác này");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();

        // Lấy thông tin từ database
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            redirectAttributes.addFlashAttribute("userNotFound", "Không tìm thấy thông tin người dùng");
            return "redirect:/TrangCaNhan";
        }

        // Kiểm tra trùng email
        Query emailQuery = entityManager.createQuery(
                "SELECT p FROM Person p WHERE p.email = :email AND p.id != :userId", Person.class);
        emailQuery.setParameter("email", email);
        emailQuery.setParameter("userId", userId);
        if (!emailQuery.getResultList().isEmpty()) {
            redirectAttributes.addFlashAttribute("emailDuplicate", "Email này đã được sử dụng bởi người dùng khác");
            return "redirect:/TrangCaNhan";
        }

        // Kiểm tra trùng số điện thoại
        Query phoneQuery = entityManager.createQuery(
                "SELECT p FROM Person p WHERE p.phoneNumber = :phoneNumber AND p.id != :userId", Person.class);
        phoneQuery.setParameter("phoneNumber", phoneNumber);
        phoneQuery.setParameter("userId", userId);
        if (!phoneQuery.getResultList().isEmpty()) {
            redirectAttributes.addFlashAttribute("phoneNumberDuplicate", "Số điện thoại này đã được sử dụng bởi người dùng khác");
            return "redirect:/TrangCaNhan";
        }

        // Cập nhật thông tin và xử lý lỗi khi merge
        try {
            if (person instanceof Teachers teacher) {
                teacher.setFirstName(firstName);
                teacher.setLastName(lastName);
                teacher.setEmail(email);
                teacher.setPhoneNumber(phoneNumber);
                entityManager.merge(teacher);
                redirectAttributes.addFlashAttribute("profileUpdateSuccess", "Cập nhật thông tin thành công");
                return "redirect:/TrangChuGiaoVien";
            } else if (person instanceof Students student) {
                student.setFirstName(firstName);
                student.setLastName(lastName);
                student.setEmail(email);
                student.setPhoneNumber(phoneNumber);
                entityManager.merge(student);
                redirectAttributes.addFlashAttribute("profileUpdateSuccess", "Cập nhật thông tin thành công");
                return "redirect:/TrangChuHocSinh";
            } else if (person instanceof Admin admin) {
                admin.setFirstName(firstName);
                admin.setLastName(lastName);
                admin.setEmail(email);
                admin.setPhoneNumber(phoneNumber);
                entityManager.merge(admin);
                redirectAttributes.addFlashAttribute("profileUpdateSuccess", "Cập nhật thông tin thành công");
                return "redirect:/TrangChuAdmin";
            } else if (person instanceof Employees employee) {
                employee.setFirstName(firstName);
                employee.setLastName(lastName);
                employee.setEmail(email);
                employee.setPhoneNumber(phoneNumber);
                entityManager.merge(employee);
                redirectAttributes.addFlashAttribute("profileUpdateSuccess", "Cập nhật thông tin thành công");
                return "redirect:/TrangChuNhanVien";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("updateFailed", "Lưu thông tin thất bại. Vui lòng thử lại");
            return "TrangCaNhan";
        }

        return "redirect:/TrangChu";
    }
}