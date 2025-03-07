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
                                "/TrangCaNhanAdmin",
                                "/DanhSachGiaoVien",
                                "/DanhSachNhanVien",
                                "/ThemGiaoVien",
                                "/ThemNhanVien",
                                "/ThemHocSinh",
                                "/XoaHocSinh/**",
                                "/XoaGiaoVien/**",
                                "/XoaNhanVien/**"
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
                                "/TrangCaNhanNhanVien",
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
                                "/BangDieuKhienGiaoVien/**"
                        ).hasRole("EMPLOYEE")
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
                                "/ChiTietTinNhanCuaHocSinh/**",
                                "/TrangCaNhanHocSinh"
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
                                "/*.css"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/DangKyGiaoVien",
                                "/DangKyNhanVien",
                                "/DangKyHocSinh"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/DangNhap")
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
                                String role = authentication.getAuthorities().iterator().next().getAuthority();
                                if ("ROLE_ADMIN".equals(role)) {
                                    response.sendRedirect("/DangNhapAdmin");
                                } else if ("ROLE_TEACHER".equals(role)) {
                                    response.sendRedirect("/DangNhapGiaoVien");
                                } else if ("ROLE_STUDENT".equals(role)) {
                                    response.sendRedirect("/DangNhapHocSinh");
                                } else {
                                    response.sendRedirect("/DangNhapNhanVien");
                                }
                            } else {
                                response.sendRedirect("/DangNhap");
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