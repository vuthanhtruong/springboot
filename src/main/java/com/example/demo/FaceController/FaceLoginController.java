package com.example.demo.FaceController;

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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FaceLoginController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private CommonUserDetailsService commonUserDetailsService;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/auth/verify-face-login")
    public String verifyFaceLogin(@RequestParam("image") String faceData) {
        // 1. Validate Face Image Input
        if (faceData == null || faceData.isEmpty()) {
            return "redirect:/TrangChu?error=invalid_face";
        }
        System.out.println("Received face data length: " + faceData.length());

        // 2. Face Recognition - Retrieve Person ID
        String personId = faceRecognitionService.findPersonIdByFace(faceData);
        if (personId == null) {
            return "redirect:/TrangChu?error=face_not_recognized";
        }
        System.out.println("Recognized Person ID: " + personId);

        // 3. Load UserDetails
        UserDetails userDetails;
        try {
            userDetails = commonUserDetailsService.loadUserByUsername(personId);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/TrangChu?error=user_not_found";
        }

        // 4. Create Authentication and Store in Security Context
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 5. Determine Redirect URL Based on User Role
        String redirectUrl = determineRedirectUrl(userDetails);
        return "redirect:" + redirectUrl;
    }

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

    @PostMapping("/DangKyKhuonMat")
    @Transactional
    public String dangKyKhuonMat(@RequestParam("faceData") String faceData, RedirectAttributes redirectAttributes) {
        if (faceData == null || faceData.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid_face");
            return "redirect:/TrangCaNhan";
        }
        System.out.println("Received face data length: " + faceData.length());

        String personId = faceRecognitionService.findPersonIdByFace(faceData);
        if (personId != null) {
            redirectAttributes.addFlashAttribute("noteFace", "face_already_registered");
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

        person.setFaceData(faceData);
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "save_failed");
            return "redirect:/TrangCaNhan";
        }

        redirectAttributes.addFlashAttribute("success", true);
        return "redirect:/TrangCaNhan";
    }

    @GetMapping("/XoaKhuonMat")
    public String showXoaKhuonMat(ModelMap modelMap) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);

        modelMap.addAttribute("user", person);
        return "XoaKhuonMat"; // Trả về trang XoaKhuonMat.html
    }

    @PostMapping("/XoaKhuonMat")
    @Transactional
    public String xoaKhuonMat(@RequestParam("faceData") String faceData, RedirectAttributes redirectAttributes) {
        if (faceData == null || faceData.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "invalid_face");
            return "redirect:/XoaKhuonMat";
        }
        System.out.println("Received face data length for deletion: " + faceData.length());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "not_logged_in");
            return "redirect:/TrangChu";
        }

        String userId = authentication.getName();
        Person person = entityManager.find(Person.class, userId);
        if (person == null) {
            redirectAttributes.addFlashAttribute("error", "user_not_found");
            return "redirect:/XoaKhuonMat";
        }

        String currentFaceData = person.getFaceData();
        if (currentFaceData == null || currentFaceData.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "no_face_to_delete");
            return "redirect:/XoaKhuonMat";
        }

        String matchedPersonId = faceRecognitionService.findPersonIdByFace(faceData);
        if (!userId.equals(matchedPersonId)) {
            redirectAttributes.addFlashAttribute("error", "face_not_matched");
            return "redirect:/XoaKhuonMat";
        }

        person.setFaceData(null);
        try {
            entityManager.merge(person);
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "delete_failed");
            return "redirect:/XoaKhuonMat";
        }

        redirectAttributes.addFlashAttribute("successDelete", true);
        return "redirect:/TrangCaNhan";
    }

}