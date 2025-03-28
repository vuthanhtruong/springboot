package com.example.demo.ControllerGET;


import com.example.demo.ModelOOP.Documents;
import com.example.demo.ModelOOP.Posts;
import com.example.demo.ModelOOP.Room;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class PostGet {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/BaiDangCaNhan/{roomId}")
    public String BaiDangCaNhan(@PathVariable String roomId, ModelMap model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        // 📌 Tìm phòng học hiện tại
        Room room = entityManager.find(Room.class, roomId);
        if (room == null) {
            return "redirect:/BaiDangCaNhan?error=RoomNotFound";
        }

        // 📂 Truy vấn tài liệu cá nhân của giáo viên trong phòng học hiện tại
        List<Documents> documents = entityManager.createQuery(
                        "FROM Documents d WHERE d.creator.id = :userId AND d.post.room.roomId = :roomId",
                        Documents.class)
                .setParameter("userId", userId)
                .setParameter("roomId", roomId)
                .getResultList();
        model.addAttribute("documents", documents);

        // 📝 Truy vấn bài đăng cá nhân của giáo viên trong phòng học hiện tại
        List<Posts> posts = entityManager.createQuery(
                        "FROM Posts p WHERE p.creator.id = :userId AND p.room.roomId = :roomId", Posts.class)
                .setParameter("userId", userId)
                .setParameter("roomId", roomId)
                .getResultList();
        model.addAttribute("posts", posts);

        model.addAttribute("room", room);

        return "BaiDangCaNhan";
    }

    @GetMapping("/SuaBaiDangCaNhan/{id}")
    public String SuaBaiDangCaNhan(@PathVariable Long id, ModelMap model) {
        // 📌 Tìm bài đăng
        Posts post = entityManager.find(Posts.class, id);
        if (post == null) {
            return "redirect:/BaiDangCaNhan?error=PostNotFound";
        }

        // 📂 Lấy tài liệu đính kèm (nếu có)
        Documents document = entityManager.createQuery(
                        "FROM Documents d WHERE d.post.postId = :postId", Documents.class)
                .setParameter("postId", id)
                .getResultStream()
                .findFirst()
                .orElse(null);

        // 📝 Đưa dữ liệu vào ModelMap
        model.addAttribute("document", document);
        model.addAttribute("post", post);

        return "SuaBaiDangCaNhan";
    }

    @GetMapping("/XoaBaiDangCaNhan/{id}")
    @Transactional
    public String XoaBaiDangCaNhan(@PathVariable Long id, @RequestParam("roomId") String roomId, HttpSession session) {

        // 🔍 Tìm bài đăng
        Posts post = entityManager.find(Posts.class, id);
        if (post == null) {
            return "redirect:/BaiDangCaNhan?error=PostNotFound";
        }

        // 📂 Xóa tất cả tài liệu liên quan
        List<Documents> documents = entityManager.createQuery(
                        "FROM Documents d WHERE d.post.postId = :postId", Documents.class)
                .setParameter("postId", id)
                .getResultList();

        for (Documents doc : documents) {
            entityManager.remove(doc);
        }

        // 🗑️ Xóa bài đăng
        entityManager.remove(post);
        entityManager.flush();

        return "redirect:/BaiDangCaNhan/" + roomId;
    }
}

