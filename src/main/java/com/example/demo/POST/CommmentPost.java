package com.example.demo.POST;

import com.example.demo.OOP.*;
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
        Person commenter = entityManager.find(Person.class, userId);

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
        Events event = entityManager.find(Events.class, 5);
        comment.setEvent(event);

        // Lưu vào database
        entityManager.persist(comment);

        // Xác định người dùng là Giáo viên hay Học sinh để chuyển hướng đúng trang
        if (commenter instanceof Teachers) {
            return "redirect:/ChiTietLopHocGiaoVien/" + post.getRoom().getRoomId();
        } else if (commenter instanceof Students) {
            return "redirect:/ChiTietLopHocHocSinh/" + post.getRoom().getRoomId();
        } else {
            return "redirect:/TrangChu";
        }
    }
}
