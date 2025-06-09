package com.example.demo.ControllerPOST;

import com.example.demo.ModelOOP.Gender;
import com.example.demo.ModelOOP.Persons;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/")
public class TrangCaNhanPost {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @PostMapping("/LuuThongTinCaNhan")
    public String luuThongTinCaNhan(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String street,
            @RequestParam(required = false) String postalCode,
            RedirectAttributes redirectAttributes) {

        // [1] Xác thực người dùng
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập để thực hiện thao tác này");
            return "redirect:/TrangChu";
        }

        // [2] Tìm thông tin người dùng
        String userId = authentication.getName();
        Persons person = entityManager.find(Persons.class, userId);
        if (person == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng");
            return "redirect:/TrangCaNhan";
        }

        // [3] Validate dữ liệu cơ bản
        if (firstName.trim().isEmpty() || lastName.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Họ và tên không được để trống");
            return "redirect:/TrangCaNhan";
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            redirectAttributes.addFlashAttribute("error", "Email không hợp lệ");
            return "redirect:/TrangCaNhan";
        }
        if (!phoneNumber.matches("^\\+?[0-9]{9,15}$")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ");
            return "redirect:/TrangCaNhan";
        }

        // [4] Kiểm tra trùng lặp email và số điện thoại
        long emailCount = (long) entityManager.createQuery("SELECT COUNT(p) FROM Persons p WHERE p.email = :email AND p.id <> :userId")
                .setParameter("email", email)
                .setParameter("userId", userId)
                .getSingleResult();
        if (emailCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Email này đã được sử dụng bởi người dùng khác");
            return "redirect:/TrangCaNhan";
        }

        long phoneCount = (long) entityManager.createQuery("SELECT COUNT(p) FROM Persons p WHERE p.phoneNumber = :phoneNumber AND p.id <> :userId")
                .setParameter("phoneNumber", phoneNumber)
                .setParameter("userId", userId)
                .getSingleResult();
        if (phoneCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại này đã được sử dụng bởi người dùng khác");
            return "redirect:/TrangCaNhan";
        }

        // [5] Cập nhật thông tin chính
        person.setFirstName(firstName.trim());
        person.setLastName(lastName.trim());
        person.setEmail(email.trim());
        person.setPhoneNumber(phoneNumber.trim());

        // [6] Xử lý ngày sinh
        if (birthDate != null && !birthDate.trim().isEmpty()) {
            try {
                LocalDate parsedDate = LocalDate.parse(birthDate);
                if (parsedDate.isAfter(LocalDate.now())) {
                    redirectAttributes.addFlashAttribute("error", "Ngày sinh không thể là ngày trong tương lai");
                    return "redirect:/TrangCaNhan";
                }
                person.setBirthDate(parsedDate);
            } catch (DateTimeParseException e) {
                redirectAttributes.addFlashAttribute("error", "Định dạng ngày sinh không hợp lệ");
                return "redirect:/TrangCaNhan";
            }
        }

        // [7] Xử lý giới tính
        if (gender != null && !gender.trim().isEmpty()) {
            try {
                person.setGender(Gender.valueOf(gender.toUpperCase()));
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", "Giới tính không hợp lệ");
                return "redirect:/TrangCaNhan";
            }
        }

        // [8] Cập nhật địa chỉ (CHỈ KHI CÓ GIÁ TRỊ)
        if (country != null && !country.isBlank()) person.setCountry(country);
        if (province != null && !province.isBlank()) person.setProvince(province);
        if (district != null && !district.isBlank()) person.setDistrict(district);
        if (ward != null && !ward.isBlank()) person.setWard(ward);
        if (street != null && !street.isBlank()) person.setStreet(street);
        if (postalCode != null && !postalCode.isBlank()) person.setPostalCode(postalCode);

        // [9] Lưu vào database
        try {
            entityManager.merge(person);
            entityManager.flush(); // Đảm bảo dữ liệu được ghi ngay lập tức
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu thông tin: " + e.getMessage());
            return "redirect:/TrangCaNhan";
        }

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công");
        return "redirect:/TrangCaNhan";
    }
}