package com.example.demo.ControllerPOST;


import com.example.demo.ModelOOP.Blogs;
import com.example.demo.ModelOOP.Notifications;
import com.example.demo.ModelOOP.Persons;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        Persons creator = entityManager.find(Persons.class, userId);
        if (creator == null) {
            return "redirect:/Blogs?error=userNotFound";
        }
        newBlog.setCreator(creator);
        newBlog.setCreatedAt(LocalDateTime.now());
        Notifications events = entityManager.find(Notifications.class, 7);
        newBlog.setEvent(events);
        entityManager.persist(newBlog);

        return "redirect:/Blogs";
    }


    @PostMapping("/XuLySuaBlog")
    public String XuLySuaBlog(@RequestParam("id") Long blogId,
                              @RequestParam("title") String title,
                              @RequestParam("content") String content,
                              RedirectAttributes redirectAttributes) {


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        Persons creator = entityManager.find(Persons.class, userId);
        if (creator == null) {
            return "redirect:/Blogs?error=userNotFound";
        }

        Blogs FixBlog = entityManager.find(Blogs.class, blogId);
        if (FixBlog == null) {
            return "redirect:/Blogs?error=blogNotFound";
        }
        FixBlog.setTitle(title);
        FixBlog.setContent(content);
        FixBlog.setCreator(creator);
        FixBlog.setCreatedAt(LocalDateTime.now());
        entityManager.merge(FixBlog);

        return "redirect:/BlogCaNhan";
    }


}