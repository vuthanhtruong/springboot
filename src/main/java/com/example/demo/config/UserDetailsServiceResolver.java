package com.example.demo.config;

import com.example.demo.OOP.Person;
import com.example.demo.Repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsServiceResolver {

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    @Qualifier("adminUserDetailsService")
    private UserDetailsService adminUserDetailsService;

    @Autowired
    @Qualifier("employeeUserDetailsService")
    private UserDetailsService employeeUserDetailsService;

    @Autowired
    @Qualifier("teacherUserDetailsService")
    private UserDetailsService teacherUserDetailsService;

    @Autowired
    @Qualifier("studentUserDetailsService")
    private UserDetailsService studentUserDetailsService;

    public UserDetailsService getUserDetailsService(String personId) {
        // Tìm Person trong database để lấy role
        return personRepository.findById(personId)
                .map(person -> {
                    String role = getRoleFromPerson(person); // Logic lấy role
                    switch (role) {
                        case "ROLE_ADMIN":
                            return adminUserDetailsService;
                        case "ROLE_EMPLOYEE":
                            return employeeUserDetailsService;
                        case "ROLE_TEACHER":
                            return teacherUserDetailsService;
                        case "ROLE_STUDENT":
                            return studentUserDetailsService;
                        default:
                            throw new RuntimeException("Unknown role: " + role);
                    }
                })
                .orElseThrow(() -> new RuntimeException("Person not found: " + personId));
    }

    private String getRoleFromPerson(Person person) {
        // Giả định: Role được lưu trong database hoặc logic nào đó
        // Thay bằng logic thực tế của bạn
        return "ROLE_TEACHER"; // Ví dụ cho GV1, cần thay bằng logic thực
    }
}