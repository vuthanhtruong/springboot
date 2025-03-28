package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(HttpMethod.GET,

                                        "/TrangChuAdmin",
                                        "/DanhSachGiaoVien",
                                        "/DanhSachNhanVien",
                                        "/ThemGiaoVien",
                                        "/ThemNhanVien",
                                        "/ThemHocSinh",
                                        "/XoaHocSinh/**",
                                        "/XoaGiaoVien/**",
                                        "/XoaNhanVien/**",
                                        "/Dashboard"
                                ).hasRole("ADMIN")
                                .requestMatchers(HttpMethod.POST,

                                        "/ThemGiaoVien",
                                        "/ThemNhanVien",
                                        "/ThemHocSinh",
                                        "/SuaGiaoVien/**",
                                        "/SuaHocSinh/**",
                                        "/CapNhatAdmin",
                                        "/TimKiemHocSinh",
                                        "/TimKiemGiaoVien",
                                        "/TimKiemNhanVien"
                                ).hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET,

                                        "/TrangChuNhanVien",
                                        "/DanhSachNhanVien",
                                        "/DanhSachGiaoVienCuaBan",
                                        "/DanhSachHocSinhCuaBan",
                                        "/DanhSachNguoiDungHeThong",
                                        "/DanhSachPhongHoc"
                                ).hasRole("EMPLOYEE")
                                .requestMatchers(HttpMethod.POST,

                                        "/CapNhatEmployee",
                                        "/TimKiemNhanVien",
                                        "/ThemGiaoVienCuaBan",
                                        "/ThemHocSinhCuaBan",
                                        "/ThemPhongHoc",
                                        "/ThemPhongHocOnline",
                                        "/BoTriLopHoc",
                                        "/XoaGiaoVienCuaBan/**",
                                        "/XoaHocSinhCuaBan/**",
                                        "/SuaGiaoVienCuaBan/**",
                                        "/SuaHocSinhCuaBan/**",
                                        "/SuaPhongHocOffline/**",
                                        "/SuaPhongHocOnline/**",
                                        "/XoaPhongHoc/**",
                                        "/ChiTietLopHoc/**",
                                        "/XoaGiaoVienTrongLop",
                                        "/XoaHocSinhTrongLop",
                                        "/GuiThongBao/**",
                                        "/BangDieuKhienNhanVien/**",
                                        "/BangDieuKhienHocSinh/**",
                                        "/BangDieuKhienGiaoVien/**",
                                        "/TimKiemGiaoVienCuaBan",
                                        "/TimKiemHocSinhCuaBan",
                                        "/DanhSachTimKiemPhongHoc",
                                        "/ThoiKhoaBieu",
                                        "/LuuLichHocNhieuSlot",
                                        "/LuuLichHoc",
                                        "/XoaLichHoc"
                                ).hasRole("EMPLOYEE")
                                .requestMatchers(HttpMethod.GET,
                                        "/TinNhanCuaBan",
                                        "/ChiTietTinNhan/**",
                                        "/XoaKhuonMat",
                                        "/XoaGiongNoi",
                                        "/ChiTietBuoiHoc",
                                        "/redirect"
                                ).hasAnyRole("TEACHER", "STUDENT", "ADMIN", "EMPLOYEE")
                                .requestMatchers(HttpMethod.POST, "/BaiPost", "/auth/verify-face-login",
                                        "/BinhLuan", "/register-face", "/XoaKhuonMat", "/DangKyKhuonMat", "/DangKyGiongNoi", "/LuuThongTinCaNhan")
                                .hasAnyRole("TEACHER", "STUDENT", "ADMIN", "EMPLOYEE")

                                .requestMatchers(HttpMethod.POST, "/DiemDanh")
                                .hasAnyRole("TEACHER", "EMPLOYEE")
                                .requestMatchers(HttpMethod.GET, "/ThoiKhoaBieuNguoiDung", "/SuaBaiDangCaNhan/**", "/XoaBaiDangCaNhan/**")
                                .hasAnyRole("TEACHER", "STUDENT")


                                .requestMatchers(HttpMethod.GET,

                                        "/TrangChuGiaoVien",
                                        "/DanhSachLopHocGiaoVien",
                                        "/ChiTietLopHocGiaoVien/**",
                                        "/ThanhVienTrongLopGiaoVien/**",
                                        "/TinNhanCuaGiaoVien",
                                        "/ChiTietTinNhanCuaGiaoVien/**",
                                        "/TrangCaNhanGiaoVien",
                                        "/ChiTietTinNhanCuaGiaoVien/**",
                                        "/TinNhanCuaGiaoVien"
                                ).hasRole("TEACHER")
                                .requestMatchers(HttpMethod.POST,

                                        "/BaiPostGiaoVien",
                                        "/LuuThongTinGiaoVien",
                                        "/BinhLuanGiaoVien"
                                ).hasRole("TEACHER")
                                .requestMatchers(HttpMethod.GET,

                                        "/TrangChuHocSinh",
                                        "/DangXuatHocSinh",
                                        "/DanhSachLopHocHocSinh",
                                        "/ChiTietLopHocHocSinh/{id}",
                                        "/ThanhVienTrongLopHocSinh/**",
                                        "/TinNhanCuaHocSinh",
                                        "/ChiTietTinNhanCuaHocSinh/**"
                                ).hasRole("STUDENT")
                                .requestMatchers(HttpMethod.POST,
                                        "/BinhLuanHocSinh",
                                        "/BaiPostHocSinh",
                                        "/GuiNhanXetGiaoVien",
                                        "/LuuThongTinHocSinh"
                                ).hasRole("STUDENT")
                                .requestMatchers(HttpMethod.GET,
                                        "/TrangChu",
                                        "/DangNhapAdmin",
                                        "/DangNhapNhanVien",
                                        "/DangNhapGiaoVien",
                                        "/DangNhapHocSinh",
                                        "/DangKyHocSinh",
                                        "/DangKyGiaoVien",
                                        "/DangKyNhanVien",
                                        "/*.css",
                                        "/auth/QuenMatKhau",
                                        "/auth/verify-otp",
                                        "/auth/DatLaiMatKhau",
                                        "/Blogs",
                                        "/XoaTatCaHocSinh",
                                        "/auth/verify-face-login"
                                ).permitAll()
                                .requestMatchers(HttpMethod.POST, "/auth/verify-face-login").permitAll()
                                .requestMatchers(HttpMethod.POST,
                                        "/DangKyGiaoVien",
                                        "/DangKyNhanVien",
                                        "/DangKyHocSinh",
                                        "/auth/QuenMatKhau",
                                        "/auth/verify-otp",
                                        "/auth/reset-password",
                                        "/auth/DatLaiMatKhau",
                                        "/auth/resend-otp",
                                        "/XuLyThemBlog",
                                        "/DangNhapBangGiongNoi"
                                ).permitAll()
                                .requestMatchers("/ws/**").permitAll()
                                // Mở API public cho AJAX requests
                                .requestMatchers("/api/**").permitAll()
                                .anyRequest().authenticated()
                        // Mở WebSocket API mà không cần xác thực

                )
                .formLogin(form -> form
                        .loginPage("/TrangChu")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username") // Dùng chung cho cả Admin & Employee
                        .passwordParameter("password")
                        .defaultSuccessUrl("/redirect", true) // Điều hướng tới controller xử lý role
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/DangXuat")  // Dùng chung cho cả Admin & Nhân viên
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if (authentication != null && authentication.getAuthorities() != null) {
                                response.sendRedirect("/TrangChu");
                            }
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .securityContext(securityContext -> securityContext.requireExplicitSave(false));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(CustomAuthenticationProvider customAuthenticationProvider) {
        return new ProviderManager(List.of(customAuthenticationProvider));
    }

}
