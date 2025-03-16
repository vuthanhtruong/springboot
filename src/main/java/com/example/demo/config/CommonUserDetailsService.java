package com.example.demo.config;

import com.example.demo.OOP.*;
import com.example.demo.Repository.PersonRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CommonUserDetailsService implements UserDetailsService {

    @Autowired
    private PersonRepository personRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Tìm trong bảng Person trước
        Person person = personRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));

        // Xác định role bằng cách kiểm tra các bảng con
        String role = determineRole(username);
        if (role == null) {
            role = "ROLE_USER"; // Mặc định nếu không xác định được
        }

        // Tạo UserDetails (mật khẩu để trống vì đăng nhập bằng khuôn mặt không cần)
        return new User(
                person.getId(),
                "{noop}no-password", // Không cần mật khẩu cho face login
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    private String determineRole(String id) {
        try {
            // Kiểm tra Admin
            Admin admin = entityManager.find(Admin.class, id);
            if (admin != null) return "ROLE_ADMIN";

            // Kiểm tra Employee
            Employees employee = entityManager.find(Employees.class, id);
            if (employee != null) return "ROLE_EMPLOYEE";

            // Kiểm tra Teacher
            Teachers teacher = entityManager.find(Teachers.class, id);
            if (teacher != null) return "ROLE_TEACHER";

            // Kiểm tra Student
            Students student = entityManager.find(Students.class, id);
            if (student != null) return "ROLE_STUDENT";

            // Nếu không thuộc bảng nào, trả về mặc định
            return "ROLE_USER";
        } catch (Exception e) {
            System.err.println("Error determining role for ID: " + id + ", defaulting to ROLE_USER");
            return "ROLE_USER";
        }
    }
}