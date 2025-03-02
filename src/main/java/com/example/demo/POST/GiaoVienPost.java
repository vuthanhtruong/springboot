package com.example.demo.POST;
import com.example.demo.OOP.*;
import com.example.demo.Repository.DocumentsRepository;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.Repository.PostsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/")
@Transactional
public class GiaoVienPost {


    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PostsRepository postsRepository;

    @Autowired
    private DocumentsRepository documentsRepository;

    @Transactional

    @PostMapping("/DangKyGiaoVien")
    public String dangKyGiaoVien(@RequestParam("EmployeeID") String employeeID,
                                 @RequestParam("TeacherID") String teacherID,
                                 @RequestParam("FirstName") String firstName,
                                 @RequestParam("LastName") String lastName,
                                 @RequestParam("Email") String email,
                                 @RequestParam("PhoneNumber") String phoneNumber,
                                 @RequestParam(value = "MisID", required = false) String misID,
                                 @RequestParam("Password") String password,
                                 @RequestParam("ConfirmPassword") String confirmPassword) {

        // Kiểm tra mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            return "redirect:/DangKyGiaoVien?error=passwordsNotMatch";
        }
        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = admins.get(0);  // Lấy trực tiếp đối tượng Admin từ danh sách
        Employees employee = entityManager.find(Employees.class, employeeID);

        // Kiểm tra nếu TeacherID đã tồn tại
        if (entityManager.find(Teachers.class, teacherID) != null) {
            return "redirect:/DangKyGiaoVien?error=teacherIDExists";
        }

        // Tạo giáo viên mới
        Teachers giaoVien = new Teachers();
        giaoVien.setEmployee(employee);
        giaoVien.setAdmin(admin);
        giaoVien.setId(teacherID);
        giaoVien.setFirstName(firstName);
        giaoVien.setLastName(lastName);
        giaoVien.setEmail(email);
        giaoVien.setPhoneNumber(phoneNumber);
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password);
        entityManager.persist(giaoVien);

        return "redirect:/DangNhapGiaoVien";
    }

    @PostMapping("/DangNhapGiaoVien")
    public String DangNhapGiaoVien(@RequestParam("TeacherID") String teacherID,
                                   @RequestParam("Password") String password,
                                   ModelMap model,
                                   HttpSession session) {
        try {
            Teachers teacher = entityManager.createQuery(
                            "SELECT t FROM Teachers t WHERE t.id = :teacherID", Teachers.class)
                    .setParameter("teacherID", teacherID)
                    .getSingleResult();

            if (teacher != null && teacher.getPassword().equals(password)) {
                session.setAttribute("TeacherID", teacher.getId()); // ✅ Lưu ID vào session
                return "redirect:/TrangChuGiaoVien"; // ✅ Đăng nhập thành công
            } else {
                model.addAttribute("error", "Mã giáo viên hoặc mật khẩu không đúng!");
                return "DangNhapGiaoVien"; // ❌ Không dùng redirect để giữ thông báo lỗi
            }
        } catch (NoResultException e) {
            model.addAttribute("error", "Mã giáo viên không tồn tại!");
            return "DangNhapGiaoVien";
        }
    }

    private static final Logger log = LoggerFactory.getLogger(GiaoVienPost.class);
    @Value("${file.upload-dir:C:/uploads}")
    private String uploadDir;

    @Transactional
    @PostMapping("/BaiPostGiaoVien")
    public String handlePost(@RequestParam("postContent") String postContent,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             @RequestParam("roomId") String roomId,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        try {
            log.info("🔍 Bắt đầu xử lý bài đăng. Nội dung: {}", postContent);

            // 🟢 Lấy ID giáo viên
            String teacherId = (String) session.getAttribute("TeacherID");
            if (teacherId == null) {
                log.error("🚫 Không tìm thấy ID giáo viên.");
                redirectAttributes.addFlashAttribute("error", "Lỗi: Không tìm thấy ID giáo viên.");
                return "redirect:/DangNhapGiaoVien";
            }

            // 📚 Lấy thông tin giáo viên
            Teachers teacher = entityManager.find(Teachers.class, teacherId);
            if (teacher == null) {
                throw new IllegalArgumentException("Không tìm thấy giáo viên với ID: " + teacherId);
            }

            // 📝 Tạo bài đăng
            Posts newPost = new Posts();
            newPost.setContent(postContent);
            newPost.setCreator(teacher);
            newPost.setCreatedAt(LocalDateTime.now());

            // 🏫 Lấy phòng học
            Rooms room = entityManager.find(Rooms.class, roomId);
            if (room == null) {
                throw new IllegalArgumentException("Không tìm thấy phòng học với ID: " + roomId);
            }
            newPost.setRoom(room);

            // 💾 Lưu bài post
            entityManager.persist(newPost);
            log.info("✅ Bài đăng đã được lưu với ID: {}", newPost.getPostId());

            // 📂 Xử lý tệp
            if (file != null && !file.isEmpty()) {
                byte[] fileData = file.getBytes();
                log.info("📏 Kích thước tệp (bytes): {}", fileData.length);

                if (fileData.length == 0) {
                    throw new IOException("❌ Tệp rỗng hoặc không đọc được.");
                }

                // Lưu tệp vào DB
                Documents document = new Documents();
                document.setDocumentTitle(file.getOriginalFilename());
                document.setFileData(fileData);  // 🟢 Lưu byte[] vào DB
                document.setFilePath(uploadDir + File.separator + file.getOriginalFilename());
                document.setCreator(teacher);
                document.setPost(newPost);

                entityManager.persist(document);
                log.info("✅ Document đã lưu với ID: {}", document.getDocumentId());
            }

            redirectAttributes.addFlashAttribute("message", "Bài đăng đã được tạo thành công!");

        } catch (IOException e) {
            log.error("❌ Lỗi khi xử lý tệp: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xử lý tệp: " + e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (Exception e) {
            log.error("🚫 Lỗi không mong muốn: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }

        return "redirect:/ChiTietLopHocGiaoVien/" + roomId;
    }

    @PostMapping("/LuuThongTinGiaoVien")
    public String LuuThongTinGiaoVien(@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName,
                                      @RequestParam("email") String email,
                                      @RequestParam("misID") String misID, @RequestParam("phoneNumber") String phoneNumber, HttpSession session) {

        Teachers teacher = entityManager.find(Teachers.class, session.getAttribute("TeacherID"));
        teacher.setFirstName(firstName);
        teacher.setLastName(lastName);
        teacher.setEmail(email);
        teacher.setMisID(misID);
        teacher.setPhoneNumber(phoneNumber);
        entityManager.persist(teacher);
        return "redirect:/TrangCaNhanGiaoVien";
    }

    @Transactional
    @PostMapping("/BinhLuanGiaoVien")
    public String themBinhLuan(@RequestParam("postId") Long postId,
                               @RequestParam("commentText") String commentText, SessionStatus sessionStatus, HttpSession session) {
        // Lấy thông tin người bình luận
        Person commenter = entityManager.find(Person.class, session.getAttribute("TeacherID"));

        // Lấy thông tin bài đăng
        Posts post = entityManager.find(Posts.class, postId);
        if (post == null) {
            throw new IllegalArgumentException("Không tìm thấy bài đăng với ID: " + postId);
        }

        // Tạo mới bình luận
        Comments comment = new Comments(commenter, post, commentText);

        // Lưu vào database
        entityManager.persist(comment);
        return "redirect:/ChiTietLopHocGiaoVien/" + post.getRoom().getRoomId();
    }

}

