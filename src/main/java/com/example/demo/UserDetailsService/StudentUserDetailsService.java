package com.example.demo.UserDetailsService;

import com.example.demo.OOP.Students;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class StudentUserDetailsService implements UserDetailsService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Students student = entityManager.createQuery(
                        "SELECT t FROM Students t WHERE t.id = :id", Students.class)
                .setParameter("id", username)
                .getSingleResult();
        return new org.springframework.security.core.userdetails.User(
                student.getId(),
                student.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

}
