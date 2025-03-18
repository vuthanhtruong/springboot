package com.example.demo.config;

import com.example.demo.FaceController.FaceRecognitionService;
import com.example.demo.OOP.Person;
import com.example.demo.UserDetailsService.AdminUserDetailsService;
import com.example.demo.UserDetailsService.EmployeeUserDetailsService;
import com.example.demo.UserDetailsService.StudentUserDetailsService;
import com.example.demo.UserDetailsService.TeacherUserDetailsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final AdminUserDetailsService adminUserDetailsService;
    private final EmployeeUserDetailsService employeeUserDetailsService;
    private final TeacherUserDetailsService teacherUserDetailsService;
    private final StudentUserDetailsService studentUserDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private FaceRecognitionService faceRecognitionService;

    @PersistenceContext
    private EntityManager entityManager;

    public CustomAuthenticationProvider(AdminUserDetailsService adminUserDetailsService,
                                        EmployeeUserDetailsService employeeUserDetailsService,
                                        TeacherUserDetailsService teacherUserDetailsService,
                                        StudentUserDetailsService studentUserDetailsService,
                                        PasswordEncoder passwordEncoder) {
        this.adminUserDetailsService = adminUserDetailsService;
        this.employeeUserDetailsService = employeeUserDetailsService;
        this.teacherUserDetailsService = teacherUserDetailsService;
        this.studentUserDetailsService = studentUserDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        Object credentials = authentication.getCredentials();
        UserDetails userDetails = null;

        // 1. Xử lý đăng nhập bằng username/password
        if (credentials instanceof String password && !password.isEmpty()) {
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
        }

        // 2. Xử lý đăng nhập bằng khuôn mặt
        if (credentials instanceof String faceData && !faceData.isEmpty() && username.equals("face-login")) {
            String personId = faceRecognitionService.findPersonIdByFace(faceData);
            if (personId != null) {
                userDetails = loadUserDetailsById(personId);
                if (userDetails != null) {
                    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                }
            }
        }

        // 3. Xử lý đăng nhập bằng giọng nói
        if (credentials instanceof String voiceData && !voiceData.isEmpty() && username.equals("voice-login")) {
            String userId = findUserIdByVoice(voiceData);
            if (userId != null) {
                userDetails = loadUserDetailsById(userId);
                if (userDetails != null) {
                    return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                }
            }
        }

        return null; // Không xác thực được
    }

    // Hàm tìm userId dựa trên voiceData
    private String findUserIdByVoice(String voiceData) {
        try {
            var query = entityManager.createQuery("SELECT p FROM Person p WHERE p.voiceData = :voiceData", Person.class);
            query.setParameter("voiceData", voiceData);
            Person person = query.getSingleResult();
            return person.getId();
        } catch (Exception e) {
            return null;
        }
    }

    // Hàm hỗ trợ tải UserDetails từ userId
    private UserDetails loadUserDetailsById(String userId) {
        UserDetails userDetails = null;
        try {
            userDetails = adminUserDetailsService.loadUserByUsername(userId);
        } catch (Exception ignored) {
        }
        if (userDetails == null) {
            try {
                userDetails = employeeUserDetailsService.loadUserByUsername(userId);
            } catch (Exception ignored) {
            }
        }
        if (userDetails == null) {
            try {
                userDetails = teacherUserDetailsService.loadUserByUsername(userId);
            } catch (Exception ignored) {
            }
        }
        if (userDetails == null) {
            try {
                userDetails = studentUserDetailsService.loadUserByUsername(userId);
            } catch (Exception ignored) {
            }
        }
        return userDetails;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}