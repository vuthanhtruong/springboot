package com.example.demo.FaceController;

import com.example.demo.OOP.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FaceLoginController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @PersistenceContext
    private EntityManager entityManager;


    @PostMapping("/DangKyKhuonMat")
    @Transactional
    public String dangKyKhuonMat(@RequestParam("faceData") String faceData, Model model) {
        if (faceData == null || faceData.isEmpty()) {
            model.addAttribute("error", "invalid_face");
            return "redirect:/TrangCaNhan";
        }
        System.out.println("Received face data length: " + faceData.length());

        String personId = faceRecognitionService.findPersonIdByFace(faceData);
        if (personId != null) {
            model.addAttribute("noteFace", "face_already_registered");
            return "redirect:/TrangCaNhan";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            model.addAttribute("error", "not_logged_in");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            model.addAttribute("error", "user_not_found");
            return "redirect:/TrangCaNhan";
        }

        person.setFaceData(faceData);
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "save_failed");
            return "redirect:/TrangCaNhan";
        }

        model.addAttribute("success", true);
        return "redirect:/TrangCaNhan";
    }

    @GetMapping("/XoaKhuonMat")
    public String showXoaKhuonMat(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);

        model.addAttribute("user", person);
        return "XoaKhuonMat"; // Trả về trang XoaKhuonMat.html
    }

    @PostMapping("/XoaKhuonMat")
    @Transactional
    public String xoaKhuonMat(@RequestParam("faceData") String faceData, Model model) {
        if (faceData == null || faceData.isEmpty()) {
            model.addAttribute("error", "invalid_face");
            return "redirect:/XoaKhuonMat";
        }
        System.out.println("Received face data length for deletion: " + faceData.length());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            model.addAttribute("error", "not_logged_in");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            model.addAttribute("error", "user_not_found");
            return "redirect:/XoaKhuonMat";
        }

        String currentFaceData = person.getFaceData();
        if (currentFaceData == null || currentFaceData.isEmpty()) {
            model.addAttribute("error", "no_face_to_delete");
            return "redirect:/XoaKhuonMat";
        }

        String matchedPersonId = faceRecognitionService.findPersonIdByFace(faceData);
        if (!userId.equals(matchedPersonId)) {
            model.addAttribute("error", "face_not_matched");
            return "redirect:/XoaKhuonMat";
        }

        person.setFaceData(null);
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "delete_failed");
            return "redirect:/XoaKhuonMat";
        }

        model.addAttribute("successDelete", true);
        return "redirect:/TrangCaNhan";
    }
}