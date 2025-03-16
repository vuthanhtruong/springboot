package com.example.demo.POST;

import com.example.demo.OOP.Person;
import com.example.demo.Repository.PersonRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/")
@Transactional
public class TrangCaNhanPost {

    private final PersonRepository personRepository;
    private final FaceRecognitionService faceRecognitionService;

    @PersistenceContext
    private EntityManager entityManager;

    public TrangCaNhanPost(PersonRepository personRepository, FaceRecognitionService faceRecognitionService) {
        this.personRepository = personRepository;
        this.faceRecognitionService = faceRecognitionService;
    }

    @PostMapping("/LuuThongTinCaNhan")
    public String luuThongTinCaNhan(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String email,
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String faceData,
            HttpSession session,
            ModelMap model) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/TrangChu?error=not_logged_in";
        }

        String userId = authentication.getName();
        Optional<Person> personOpt = personRepository.findById(userId);
        if (!personOpt.isPresent()) {
            return "redirect:/TrangChu?error=user_not_found";
        }

        Person person = personOpt.get();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setEmail(email);
        person.setPhoneNumber(phoneNumber);

        if (faceData != null && !faceData.isEmpty()) {
            // Chuẩn hóa faceData thành base64 thô
            if (faceData.startsWith("data:image")) {
                faceData = faceData.substring(faceData.indexOf(",") + 1);
            }
            person.setFaceData(faceData);
        }

        try {
            personRepository.save(person);
        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/TrangCaNhan?error=save_failed";
        }

        String redirectUrl = determineRedirectUrl(authentication);
        return "redirect:" + redirectUrl + "?success";
    }

    private String determineRedirectUrl(Authentication authentication) {
        for (var authority : authentication.getAuthorities()) {
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
                    return "/TrangChu"; // Mặc định nếu không có role hợp lệ
            }
        }
        return "/TrangChu"; // Nếu không có quyền nào
    }
}