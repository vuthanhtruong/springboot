package com.example.demo.UserDetailsService;

import com.example.demo.ModelOOP.Staffs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserDetails loadUserByUsername(String employeeid) throws UsernameNotFoundException {
        Staffs employee = entityManager.find(Staffs.class, employeeid);

        return new org.springframework.security.core.userdetails.User(
                employee.getId(),
                employee.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
        );
    }
}