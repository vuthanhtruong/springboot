package com.example.demo.VoiceController;

import com.example.demo.OOP.Person;
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

        person.setVoiceData(voiceData);
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