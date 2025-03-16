package com.example.demo.GET;

import com.example.demo.OOP.FaceImageDTO;
import com.example.demo.OOP.Person;
import com.example.demo.POST.FaceRecognitionService;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.config.UserDetailsServiceResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FaceLoginController {

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private UserDetailsServiceResolver userDetailsServiceResolver;

    @PostMapping("/auth/verify-face-login")
    public ResponseEntity<?> verifyFaceLogin(@RequestBody FaceImageDTO faceImageDTO) {
        try {
            // 1. Validate Face Image Input
            if (faceImageDTO.getImage() == null || faceImageDTO.getImage().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new LoginResponse(false, "Ảnh khuôn mặt không hợp lệ"));
            }

            // 2. Face Recognition - Retrieve Person ID
            String personId = faceRecognitionService.findPersonIdByFace(faceImageDTO.getImage());
            if (personId == null) {
                return ResponseEntity.status(401)
                        .body(new LoginResponse(false, "Không nhận diện được khuôn mặt"));
            }

            System.out.println("Person ID: " + personId);

            // 3. Find Person from Database
            Person person = personRepository.findById(personId)
                    .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng"));

            // 4. Dynamically Load the Correct UserDetailsService
            UserDetailsService dynamicUserDetailsService = userDetailsServiceResolver.getUserDetailsService(personId);

            // 5. Load UserDetails and Create Authentication
            UserDetails userDetails = dynamicUserDetailsService.loadUserByUsername(personId);

            // 6. Create Authentication and Store in Security Context
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 7. Determine Redirect URL Based on User Role
            String redirectUrl = determineRedirectUrl(userDetails);
            return ResponseEntity.ok(new LoginResponse(true, redirectUrl));

        } catch (UsernameNotFoundException e) {
            // Handle specific case of user not found
            return ResponseEntity.status(404)
                    .body(new LoginResponse(false, "Người dùng không tìm thấy: " + e.getMessage()));
        } catch (Exception e) {
            // General system error handling
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new LoginResponse(false, "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private String determineRedirectUrl(UserDetails userDetails) {
        // Ensure that the user has a role, and redirect based on that role
        for (var authority : userDetails.getAuthorities()) {
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
                    // Unknown role, redirect to default page or error
                    return "/TrangChu";
            }
        }
        return "/TrangChu"; // Default fallback if no role is found
    }

    // Response class to return in JSON format
    public static class LoginResponse {
        private final boolean success;
        private String redirectUrl;
        private String message;

        public LoginResponse(boolean success, String messageOrUrl) {
            this.success = success;
            if (success) {
                this.redirectUrl = messageOrUrl;
            } else {
                this.message = messageOrUrl;
            }
        }

        public boolean isSuccess() {
            return success;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getMessage() {
            return message;
        }
    }
}
