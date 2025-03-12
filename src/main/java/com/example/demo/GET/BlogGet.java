package com.example.demo.GET;


import com.example.demo.OOP.Blogs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
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

}
