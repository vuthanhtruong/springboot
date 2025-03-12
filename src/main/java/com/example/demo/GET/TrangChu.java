package com.example.demo.GET;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class TrangChu {
    @GetMapping("/TrangChu")
    public String TrangChu(HttpSession session) {
        return "TrangChu";
    }
}
