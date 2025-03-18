package com.example.demo.GET;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private AuthenticationManager authenticationManager;

    // Hiển thị trang đăng nhập chung
    @GetMapping("/DangNhap")
    public String showLoginPage() {
        return "DangNhap"; // Trả về template DangNhap.html
    }

    // Xử lý redirect sau khi đăng nhập thành công (username/password)
    @GetMapping("/redirect")
    public String redirectAfterLogin(Authentication authentication, HttpServletRequest request, Model model)
            throws ServletException {
        if (authentication == null || !authentication.isAuthenticated()) {
            model.addAttribute("NOTE", "Tài khoản hoặc mật khẩu không chính xác");
            return "redirect:/TrangChu?error";
        }

        System.out.println("✅ Đăng nhập: " + authentication.getName());
        authentication.getAuthorities().forEach(auth ->
                System.out.println("🔹 Quyền: " + auth.getAuthority())
        );

        String redirectUrl = determineRedirectUrl(authentication.getAuthorities());
        if (redirectUrl == null) {
            System.out.println("❌ Không tìm thấy quyền hợp lệ, đăng xuất...");
            request.logout();
            return "redirect:/DangNhap?error";
        }

        return "redirect:" + redirectUrl;
    }

    // Xử lý đăng nhập bằng khuôn mặt
    @PostMapping("/auth/verify-face-login")
    public String verifyFaceLogin(@RequestParam("image") String faceData, Model model) {
        if (faceData == null || faceData.isEmpty()) {
            model.addAttribute("error", "invalid_face");
            return "redirect:/TrangChu";
        }
        System.out.println("Received face data length: " + faceData.length());

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken("face-login", faceData)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            String redirectUrl = determineRedirectUrl(auth.getAuthorities());
            return "redirect:" + (redirectUrl != null ? redirectUrl : "/TrangChu");
        } catch (AuthenticationException e) {
            e.printStackTrace();
            model.addAttribute("error", "face_not_recognized_or_user_not_found");
            return "redirect:/TrangChu";
        }
    }

    // Xử lý đăng nhập bằng giọng nói
    @PostMapping("/DangNhapBangGiongNoi")
    public String dangNhapBangGiongNoi(@RequestParam("voiceData") String voiceData, Model model) {
        if (voiceData == null || voiceData.isEmpty()) {
            model.addAttribute("error", "invalid_voice");
            return "redirect:/TrangChu";
        }
        System.out.println("Received voice data length: " + voiceData.length());

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken("voice-login", voiceData)
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            String redirectUrl = determineRedirectUrl(auth.getAuthorities());
            return "redirect:" + (redirectUrl != null ? redirectUrl : "/TrangChu");
        } catch (AuthenticationException e) {
            e.printStackTrace();
            model.addAttribute("error", "voice_not_matched_or_user_not_found");
            return "redirect:/TrangChu";
        }
    }

    // Phương thức chung để xác định URL chuyển hướng dựa trên role
    private String determineRedirectUrl(Iterable<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
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
                    System.err.println("No valid role found, defaulting to /TrangChu");
                    return null;
            }
        }
        return null;
    }
}