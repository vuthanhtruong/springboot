package com.example.demo.ControllerPOST;

import com.example.demo.ModelOOP.Comments;
import com.example.demo.ModelOOP.Notifications;
import com.example.demo.ModelOOP.Persons;
import com.example.demo.ModelOOP.Posts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
@Transactional

public class CommmentPost {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @PostMapping("/BinhLuan")
    public String themBinhLuan(@RequestParam("postId") Long postId,
                               @RequestParam("commentText") String commentText,
                               HttpSession session) {
        // Lấy thông tin người dùng hiện tại
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        Persons commenter = entityManager.find(Persons.class, userId);

        // Kiểm tra nếu không tìm thấy người dùng
        if (commenter == null) {
            throw new IllegalArgumentException("Người dùng không hợp lệ hoặc không tồn tại.");
        }

        // Lấy thông tin bài đăng
        Posts post = entityManager.find(Posts.class, postId);
        if (post == null) {
            throw new IllegalArgumentException("Không tìm thấy bài đăng với ID: " + postId);
        }

        // Kiểm tra nội dung bình luận có hợp lệ không
        if (commentText == null || commentText.trim().isEmpty()) {
            return "redirect:/error?message=Bình luận không được để trống!";
        }

        // Tạo mới bình luận
        Comments comment = new Comments(commenter, post, commentText);

        // Liên kết với sự kiện nếu cần
        Notifications event = entityManager.find(Notifications.class, 6);
        comment.setEvent(event);

        // Lưu vào database
        entityManager.persist(comment);

        return "redirect:/ChiTietLopHocBanThamGia/" + post.getRoom().getRoomId();
    }
}
