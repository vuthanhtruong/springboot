package com.example.demo.POST;


import com.example.demo.OOP.Blogs;
import com.example.demo.OOP.Events;
import com.example.demo.OOP.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/")
@Transactional
public class BlogPost {
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/XuLyThemBlog")
    public String XuLyThemBlog(@ModelAttribute Blogs newBlog) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        Person creator = entityManager.find(Person.class, userId);
        if (creator == null) {
            return "redirect:/Blogs?error=userNotFound";
        }
        newBlog.setCreator(creator);
        newBlog.setCreatedAt(LocalDateTime.now());
        Events events = entityManager.find(Events.class, 6);
        newBlog.setEvent(events);
        entityManager.persist(newBlog);

        return "redirect:/Blogs";
    }


}
