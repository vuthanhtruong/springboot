package com.example.demo.GET;

import com.example.demo.OOP.Blogs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;


@Controller
@RequestMapping("/")
@Transactional
public class BlogGet {
    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/Blogs")
    public String blog(ModelMap model) {
        List<Blogs> blogs = entityManager.createQuery("from Blogs b", Blogs.class).getResultList();
        model.addAttribute("blogs", blogs);
        return "Blog";
    }

    @GetMapping("/BlogCaNhan")
    public String BlogCaNhan(ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        List<Blogs> blogs = entityManager.createQuery("from Blogs b where b.creator.id = :userId", Blogs.class)
                .setParameter("userId", userId)
                .getResultList();
        model.addAttribute("blogs", blogs);
        return "BlogCaNhan";
    }

    @GetMapping("/SuaBlogCaNhan/{id}")
    public String SuaBlogCaNhan(ModelMap model, @PathVariable("id") String id) {
        Blogs Fixblogs = entityManager.find(Blogs.class, id);
        if (Fixblogs == null) {
            return "redirect:/Blogs?error=blogNotFound";
        }
        model.addAttribute("Fixblogs", Fixblogs);
        return "SuaBlogCaNhan";
    }

    @GetMapping("/XoaBlogCaNhan/{id}")
    public String XoaBlogCaNhan(@PathVariable("id") String id) {
        Blogs blogs = entityManager.find(Blogs.class, id);
        if (blogs == null) {
            return "redirect:/Blogs?error=blogNotFound";
        }
        entityManager.remove(blogs);
        return "redirect:/BlogCaNhan";
    }

}