package com.example.demo.GET;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class LoginController {

    @GetMapping("/DangNhap")
    public String showLoginPage() {
        return "DangNhap"; // Trả về tên template: DangNhap.html trong resources/templates/
    }

    @GetMapping("/redirect")
    public String redirectAfterLogin(Authentication authentication, HttpServletRequest request) throws ServletException, IOException {
        if (authentication == null) {
            System.out.println("🔴 Authentication null, chuyển về /DangNhap");
            return "redirect:/TrangChu?error";
        }

        System.out.println("✅ Đăng nhập: " + authentication.getName());
        authentication.getAuthorities().forEach(auth -> System.out.println("🔹 Quyền: " + auth.getAuthority()));

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            switch (authority.getAuthority()) {
                case "ROLE_ADMIN":
                    return "redirect:/TrangChuAdmin";
                case "ROLE_EMPLOYEE":
                    return "redirect:/TrangChuNhanVien";
                case "ROLE_TEACHER":
                    return "redirect:/TrangChuGiaoVien";
                case "ROLE_STUDENT":
                    return "redirect:/TrangChuHocSinh";
            }
        }

        System.out.println("❌ Không tìm thấy quyền hợp lệ, đăng xuất...");
        request.logout();
        return "redirect:/DangNhap?error";
    }

}