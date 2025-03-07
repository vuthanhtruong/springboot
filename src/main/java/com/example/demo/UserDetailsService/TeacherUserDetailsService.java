package com.example.demo.UserDetailsService;

import com.example.demo.OOP.Teachers;
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
public class TeacherUserDetailsService implements UserDetailsService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Teachers teacher = entityManager.createQuery(
                        "SELECT t FROM Teachers t WHERE t.id = :id", Teachers.class)
                .setParameter("id", username)
                .getSingleResult();

        if (teacher == null) {
            throw new UsernameNotFoundException("Không tìm thấy giáo viên với ID: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                teacher.getId(),
                teacher.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
    }

}
