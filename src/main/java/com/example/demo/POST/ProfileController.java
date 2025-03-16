package com.example.demo.POST;

import com.example.demo.OOP.Person;
import com.example.demo.Repository.PersonRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
@Transactional
public class ProfileController {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PersonRepository personRepository;  // Repository để lưu trữ thông tin người dùng

    @Autowired
    private FaceRecognitionService faceRecognitionService;  // Dịch vụ nhận diện khuôn mặt

    @PostMapping("/register-face")
    public String registerFace(@RequestParam("faceData") String faceData,
                               @RequestParam("firstName") String firstName,
                               @RequestParam("lastName") String lastName,
                               @RequestParam("email") String email,
                               @RequestParam("phoneNumber") String phoneNumber,
                               Model model) {
        try {
            // Gửi dữ liệu khuôn mặt tới dịch vụ nhận diện khuôn mặt
            String faceRecognitionResult = faceRecognitionService.processFaceData(faceData);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = authentication.getName();

            // Tạo đối tượng Person mới từ dữ liệu form
            Person user = entityManager.find(Person.class, userId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPhoneNumber(phoneNumber);

            // Handle the response from the face recognition service
            if (faceRecognitionResult != null && !faceRecognitionResult.isEmpty()) {
                user.setFaceData("Face recognized: " + faceRecognitionResult);  // Or store the face token
            } else {
                user.setFaceData("No face detected.");
            }

            // Lưu người dùng vào cơ sở dữ liệu
            personRepository.save(user);

            // Hiển thị thông báo thành công
            model.addAttribute("message", "Đăng ký khuân mặt thành công!");
        } catch (Exception e) {
            model.addAttribute("message", "Có lỗi xảy ra khi đăng ký khuân mặt!");
            e.printStackTrace();
        }

        return "redirect:/TrangCaNhan"; // Hoặc trang phù hợp
    }


}
