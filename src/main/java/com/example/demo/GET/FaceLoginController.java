package com.example.demo.GET;

import com.example.demo.OOP.FaceImageDTO;
import com.example.demo.POST.FaceRecognitionService;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.config.CommonUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private CommonUserDetailsService commonUserDetailsService;

    @PostMapping("/auth/verify-face-login")
    public ResponseEntity<?> verifyFaceLogin(@RequestBody FaceImageDTO faceImageDTO) {
        try {
            // 1. Validate Face Image Input
            if (faceImageDTO.getImage() == null || faceImageDTO.getImage().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new LoginResponse(false, "Ảnh khuôn mặt không hợp lệ"));
            }
            System.out.println("Received face data length: " + faceImageDTO.getImage().length());

            // 2. Face Recognition - Retrieve Person ID
            String personId = faceRecognitionService.findPersonIdByFace(faceImageDTO.getImage());
            if (personId == null) {
                return ResponseEntity.status(401)
                        .body(new LoginResponse(false, "Không nhận diện được khuôn mặt"));
            }

            UserDetails userDetails = commonUserDetailsService.loadUserByUsername(personId);

            // 5. Create Authentication and Store in Security Context
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 6. Determine Redirect URL Based on User Role
            String redirectUrl = determineRedirectUrl(userDetails);
            return ResponseEntity.ok(new LoginResponse(true, redirectUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new LoginResponse(false, "Lỗi hệ thống: " + e.getMessage()));
        }
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
}