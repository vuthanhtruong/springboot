package com.example.demo.config;

import com.example.demo.UserDetailsService.AdminUserDetailsService;
import com.example.demo.UserDetailsService.EmployeeUserDetailsService;
import com.example.demo.UserDetailsService.StudentUserDetailsService;
import com.example.demo.UserDetailsService.TeacherUserDetailsService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final AdminUserDetailsService adminUserDetailsService;
    private final EmployeeUserDetailsService employeeUserDetailsService;
    private final TeacherUserDetailsService teacherUserDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final StudentUserDetailsService studentUserDetailsService;

    public CustomAuthenticationProvider(AdminUserDetailsService adminUserDetailsService,
                                        EmployeeUserDetailsService employeeUserDetailsService,
                                        TeacherUserDetailsService teacherUserDetailsService,
                                        StudentUserDetailsService studentUserDetailsService,
                                        PasswordEncoder passwordEncoder) {
        this.adminUserDetailsService = adminUserDetailsService;
        this.employeeUserDetailsService = employeeUserDetailsService;
        this.teacherUserDetailsService = teacherUserDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.studentUserDetailsService = studentUserDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        UserDetails userDetails = null;

        try {
            userDetails = adminUserDetailsService.loadUserByUsername(username);
        } catch (Exception ignored) {
        }

        if (userDetails == null) {
            try {
                userDetails = employeeUserDetailsService.loadUserByUsername(username);
            } catch (Exception ignored) {
            }
        }

        if (userDetails == null) {
            try {
                userDetails = teacherUserDetailsService.loadUserByUsername(username);
            } catch (Exception ignored) {
            }
        }

        if (userDetails == null) {
            try {
                userDetails = studentUserDetailsService.loadUserByUsername(username);
            } catch (Exception ignored) {
            }
        }

        if (userDetails != null && passwordEncoder.matches(password, userDetails.getPassword())) {
            return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
        }

        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
