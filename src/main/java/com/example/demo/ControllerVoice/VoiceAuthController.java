package com.example.demo.ControllerVoice;

import com.example.demo.ModelOOP.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class VoiceAuthController {

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/DangKyGiongNoi")
    @Transactional
    public String dangKyGiongNoi(@RequestParam("voiceData") String voiceData, RedirectAttributes redirectAttributes) {
        if (voiceData == null || voiceData.isEmpty()) {
            redirectAttributes.addFlashAttribute("voiceDataInvalid", "Giọng nói không hợp lệ");
            return "redirect:/TrangCaNhan";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("notLoggedIn", "Bạn cần đăng nhập để thực hiện thao tác này");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            redirectAttributes.addFlashAttribute("userNotFound", "Không tìm thấy thông tin người dùng");
            return "redirect:/TrangCaNhan";
        }

        person.setVoiceData(voiceData);
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("voiceSaveFailed", "Lưu dữ liệu giọng nói thất bại");
            return "redirect:/TrangCaNhan";
        }

        redirectAttributes.addFlashAttribute("voiceRegisterSuccess", "Đăng ký giọng nói thành công");
        return "redirect:/TrangCaNhan";
    }

    @GetMapping("/XoaGiongNoi")
    @Transactional
    public String xoaGiongNoi(RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("notLoggedIn", "Bạn cần đăng nhập để thực hiện thao tác này");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            redirectAttributes.addFlashAttribute("userNotFound", "Không tìm thấy thông tin người dùng");
            return "redirect:/TrangCaNhan";
        }

        if (person.getVoiceData() == null || person.getVoiceData().isEmpty()) {
            redirectAttributes.addFlashAttribute("noVoiceToDelete", "Không có dữ liệu giọng nói để xóa");
            return "redirect:/TrangCaNhan";
        }

        person.setVoiceData(null);
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("voiceDeleteFailed", "Xóa dữ liệu giọng nói thất bại");
            return "redirect:/TrangCaNhan";
        }

        redirectAttributes.addFlashAttribute("voiceDeleteSuccess", "Xóa giọng nói thành công");
        return "redirect:/TrangCaNhan";
    }
}