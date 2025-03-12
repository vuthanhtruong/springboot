package com.example.demo.POST;

import com.example.demo.OOP.*;
import com.example.demo.Repository.DocumentsRepository;
import com.example.demo.Repository.PersonRepository;
import com.example.demo.Repository.PostsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class GiaoVienPost {


    private static final Logger log = LoggerFactory.getLogger(GiaoVienPost.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private PersonRepository personRepository;
    @Autowired
    private PostsRepository postsRepository;
    @Autowired
    private DocumentsRepository documentsRepository;
    @Value("${file.upload-dir:C:/uploads}")
    private String uploadDir;

    @Transactional
    @PostMapping("/DangKyGiaoVien")
    public String dangKyGiaoVien(@RequestParam("EmployeeID") String employeeID,
                                 @RequestParam("TeacherID") String teacherID,
                                 @RequestParam("FirstName") String firstName,
                                 @RequestParam("LastName") String lastName,
                                 @RequestParam("Email") String email,
                                 @RequestParam("PhoneNumber") String phoneNumber,
                                 @RequestParam("BirthDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                                 @RequestParam(value = "MisID", required = false) String misID,
                                 @RequestParam("Password") String password,
                                 @RequestParam("ConfirmPassword") String confirmPassword,
                                 Model model) {

        System.out.println("Bắt đầu đăng ký giáo viên...");

        // Kiểm tra mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            model.addAttribute("passwordError", "Mật khẩu không khớp.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra tính hợp lệ của mật khẩu
        if (!isValidPassword(password)) {
            model.addAttribute("passwordInvalid", "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra ngày sinh có hợp lệ không (phải trong quá khứ)
        if (birthDate.isAfter(LocalDate.now())) {
            model.addAttribute("birthDateError", "Ngày sinh không hợp lệ.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra nếu Email đã tồn tại
        List<Teachers> existingTeachers = entityManager.createQuery("SELECT t FROM Teachers t WHERE t.email = :email", Teachers.class)
                .setParameter("email", email)
                .getResultList();
        if (!existingTeachers.isEmpty()) {
            model.addAttribute("emailError", "Email này đã được sử dụng.");
            return "DangKyGiaoVien";
        }

        // Kiểm tra TeacherID đã tồn tại
        if (entityManager.find(Person.class, teacherID) != null) {
            model.addAttribute("teacherIDError", "TeacherID đã tồn tại.");
            return "DangKyGiaoVien";
        }
        // Lấy Admin
        List<Admin> adminList = entityManager.createQuery("FROM Admin", Admin.class).getResultList();
        if (adminList.isEmpty()) {
            model.addAttribute("adminError", "Không tìm thấy Admin.");
            return "DangKyGiaoVien";
        }
        Admin admin = adminList.get(0);

        // Lấy Employee
        Employees employee = entityManager.find(Employees.class, employeeID);
        if (employee == null) {
            model.addAttribute("employeeError", "Employee ID không hợp lệ.");
            return "DangKyGiaoVien";
        }

        // Tạo giáo viên mới
        Teachers giaoVien = new Teachers();
        giaoVien.setId(teacherID);
        giaoVien.setFirstName(firstName);
        giaoVien.setLastName(lastName);
        giaoVien.setEmail(email);
        giaoVien.setPhoneNumber(phoneNumber);
        giaoVien.setBirthDate(birthDate); // Lưu ngày sinh
        giaoVien.setMisID(misID);
        giaoVien.setPassword(password); // Nên mã hóa mật khẩu trước khi lưu
        giaoVien.setEmployee(employee);
        giaoVien.setAdmin(admin);

        try {
            entityManager.persist(giaoVien);
            System.out.println("Đăng ký giáo viên thành công!");
        } catch (Exception e) {
            System.out.println("Lỗi khi lưu giáo viên: " + e.getMessage());
            model.addAttribute("databaseError", "Lỗi khi lưu dữ liệu.");
            return "DangKyGiaoVien";
        }
        return "redirect:/TrangChu";
    }


    private boolean isValidPassword(String password) {
        // Mật khẩu phải có ít nhất 8 ký tự, chứa ít nhất 1 chữ hoa, 1 chữ thường, 1 số và 1 ký tự đặc biệt
        String passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(passwordRegex);
    }

    @Transactional
    @PostMapping("/BaiPostGiaoVien")
    public String handlePost(@RequestParam("postContent") String postContent,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             @RequestParam("roomId") String roomId,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        try {
            log.info("🔍 Bắt đầu xử lý bài đăng. Nội dung: {}", postContent);


            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String teacherId = authentication.getName();
            Person person = entityManager.find(Person.class, teacherId);
            Teachers teacher = (Teachers) person;
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
            Events event = entityManager.find(Events.class, 3);
            newPost.setEvent(event);

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
                Events eventq = entityManager.find(Events.class, 4);
                document.setEvent(eventq);

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

    @Transactional
    @PostMapping("/BinhLuanGiaoVien")
    public String themBinhLuan(@RequestParam("postId") Long postId,
                               @RequestParam("commentText") String commentText, SessionStatus sessionStatus, HttpSession session) {
        // Lấy thông tin người bình luận
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String teacherId = authentication.getName();
        Person commenter = entityManager.find(Person.class, teacherId);

        // Lấy thông tin bài đăng
        Posts post = entityManager.find(Posts.class, postId);
        if (post == null) {
            throw new IllegalArgumentException("Không tìm thấy bài đăng với ID: " + postId);
        }

        // Tạo mới bình luận
        Comments comment = new Comments(commenter, post, commentText);
        Events event = entityManager.find(Events.class, 5);
        comment.setEvent(event);
        // Lưu vào database
        entityManager.persist(comment);
        return "redirect:/ChiTietLopHocGiaoVien/" + post.getRoom().getRoomId();
    }

}

