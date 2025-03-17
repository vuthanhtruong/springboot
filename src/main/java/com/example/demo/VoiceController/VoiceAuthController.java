package com.example.demo.VoiceController;

import com.example.demo.OOP.Person;
import com.example.demo.config.CommonUserDetailsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VoiceAuthController {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private CommonUserDetailsService userDetailsService; // Sử dụng CommonUserDetailsService

    @PostMapping("/DangNhapBangGiongNoi")
    public String dangNhapBangGiongNoi(@RequestParam("voiceData") String voiceData, RedirectAttributes redirectAttributes) {
        if (voiceData == null || voiceData.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid_voice");
            return "redirect:/TrangChu";
        }

        // Tìm userId dựa trên voiceData (transcript)
        String userId = findUserIdByVoice(voiceData);
        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "voice_not_matched");
            return "redirect:/TrangChu";
        }

        // Tải UserDetails từ CommonUserDetailsService
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(userId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "user_not_found");
            return "redirect:/TrangChu";
        }

        // Đăng nhập thủ công với Spring Security
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Xác định URL chuyển hướng dựa trên vai trò
        String redirectUrl = determineRedirectUrl(userDetails);
        redirectAttributes.addFlashAttribute("success", "Đăng nhập bằng giọng nói thành công!");
        return "redirect:" + redirectUrl;
    }

    // Hàm tìm userId dựa trên voiceData (so sánh transcript)
    private String findUserIdByVoice(String voiceData) {
        try {
            var query = entityManager.createQuery("SELECT p FROM Person p WHERE p.voiceData = :voiceData", Person.class);
            query.setParameter("voiceData", voiceData);
            Person person = query.getSingleResult();
            return person.getId();
        } catch (Exception e) {
            return null; // Không tìm thấy user khớp
        }
    }

    // Hàm xác định URL chuyển hướng dựa trên vai trò từ UserDetails
    private String determineRedirectUrl(UserDetails userDetails) {
        for (var authority : userDetails.getAuthorities()) {
            System.out.println("User authority: " + authority.getAuthority());
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN":
                    return "/TrangChuAdmin";
                case "ROLE_EMPLOYEE":
                    return "/TrangChuNhanVien";
                case "ROLE_TEACHER":
                    return "/TrangChuGiaoVien";
                case "ROLE_STUDENT":
                    return "/TrangChuHocSinh";
                default:
                    return "/TrangChu";
            }
        }
        System.err.println("No valid role found for user, defaulting to /TrangChu");
        return "/TrangChu";
    }

    @PostMapping("/DangKyGiongNoi")
    @Transactional
    public String dangKyGiongNoi(@RequestParam("voiceData") String voiceData, RedirectAttributes redirectAttributes) {
        if (voiceData == null || voiceData.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid_voice");
            return "redirect:/TrangCaNhan";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "not_logged_in");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            redirectAttributes.addFlashAttribute("error", "user_not_found");
            return "redirect:/TrangCaNhan";
        }

        person.setVoiceData(voiceData); // Lưu transcript thay vì profileId
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "save_failed");
            return "redirect:/TrangCaNhan";
        }

        redirectAttributes.addFlashAttribute("success", "Đăng ký giọng nói thành công!");
        return "redirect:/TrangCaNhan";
    }

    @GetMapping("/XoaGiongNoi")
    @Transactional
    public String xoaGiongNoi(RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "not_logged_in");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            redirectAttributes.addFlashAttribute("error", "user_not_found");
            return "redirect:/TrangCaNhan";
        }

        if (person.getVoiceData() == null || person.getVoiceData().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "no_voice_to_delete");
            return "redirect:/TrangCaNhan";
        }

        person.setVoiceData(null);
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "delete_failed");
            return "redirect:/TrangCaNhan";
        }

        redirectAttributes.addFlashAttribute("success", "Xóa giọng nói thành công!");
        return "redirect:/TrangCaNhan";
    }
}