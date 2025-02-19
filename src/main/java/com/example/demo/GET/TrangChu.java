package com.example.demo.GET;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class TrangChu {
    @GetMapping("/TrangChu")
    public String TrangChu() {
        return "TrangChu";
    }
    @GetMapping("/DangNhapGiaoVien")
    public String DanhChoGiaoVien() {
        return "DangNhapGiaoVien";
    }
    @GetMapping("/DangNhapNhanVien")
    public String DanhChoNhanVien() {
        return "DangNhapNhanVien";
    }
    @GetMapping("/DangNhapHocSinh")
    public String DanhChoHocSinh() {
        return "DangNhapHocSinh";
    }
    @GetMapping("/DangNhapAdmin")
    public String DanhChoAdmih() {
        return "DangNhapAdmin";
    }
}
