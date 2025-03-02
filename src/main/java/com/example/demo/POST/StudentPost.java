package com.example.demo.POST;

import com.example.demo.OOP.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/")
@Transactional
public class StudentPost {
    @PersistenceContext
    private EntityManager entityManager;
    @PostMapping("/DangKyHocSinh")
    public String DangKyHocSinh(@RequestParam("EmployeeID") String employeeID,
                                @RequestParam("StudentID") String studentID,
                                @RequestParam("FirstName") String firstName,
                                @RequestParam("LastName") String lastName,
                                @RequestParam("Email") String email,
                                @RequestParam("PhoneNumber") String phoneNumber,
                                @RequestParam(value = "MisID", required = false) String misID,
                                @RequestParam("Password") String password,
                                @RequestParam("ConfirmPassword") String confirmPassword) {

        // Kiểm tra mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            return "redirect:/DangKyHocSinh?error=password_mismatch";
        }


        List<Admin> admins = entityManager.createQuery("from Admin", Admin.class).getResultList();
        Admin admin = admins.get(0);  // Lấy trực tiếp đối tượng Admin từ danh sách
        Employees employee = entityManager.find(Employees.class, employeeID);

        if (employee == null) {
            return "redirect:/DangKyHocSinh?error=invalid_employee";
        }

        // Kiểm tra xem StudentID đã tồn tại chưa
        if (entityManager.find(Students.class, studentID) != null) {
            return "redirect:/DangKyHocSinh?error=student_exists";
        }

        Students student = new Students();
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        student.setPassword(password);
        student.setId(studentID); // Không cần thêm "STU", vì đã kiểm tra trước đó
        student.setAdmin(admin);
        student.setEmployee(employee);
        student.setMisId(misID);

        entityManager.persist(student);

        return "redirect:/DangNhapHocSinh";
    }

    @PostMapping("/DangNhapHocSinh")
    public String DangNhapHocSinh(@RequestParam("studentID") String studentID,
                                  @RequestParam("password") String password,
                                  ModelMap model,
                                  HttpSession session) {
        try {
            Students student = entityManager.find(Students.class, studentID);

            if (student != null && student.getPassword().equals(password)) {
                session.setAttribute("StudentID", student.getId());
                return "redirect:/TrangChuHocSinh";
            } else {
                model.addAttribute("error", "Mã học sinh hoặc mật khẩu không đúng!");
                return "redirect:/DangNhapHocSinh";
            }
        } catch (NoResultException e) {
            model.addAttribute("error", "Mã học sinh không tồn tại!");
            return "redirect:/DangNhapHocSinh";
        }
    }
    @Transactional
    @PostMapping("/BinhLuanHocSinh")
    public String themBinhLuan(@RequestParam("postId") Long postId,
                               @RequestParam("commentText") String commentText, SessionStatus sessionStatus, HttpSession session) {
        // Lấy thông tin người bình luận
        Person commenter = entityManager.find(Person.class, session.getAttribute("StudentID"));

        // Lấy thông tin bài đăng
        Posts post = entityManager.find(Posts.class, postId);
        if (post == null) {
            throw new IllegalArgumentException("Không tìm thấy bài đăng với ID: " + postId);
        }

        // Tạo mới bình luận
        Comments comment = new Comments(commenter, post, commentText);

        // Lưu vào database
        entityManager.persist(comment);
        return "redirect:/ChiTietLopHocHocSinh/" + post.getRoom().getRoomId();
    }

    private static final Logger log = LoggerFactory.getLogger(GiaoVienPost.class);
    @Value("${file.upload-dir:C:/uploads}")
    private String uploadDir;
    @Transactional
    @PostMapping("/BaiPostHocSinh")
    public String handleStudentPost(@RequestParam("postContent") String postContent,
                                    @RequestParam(value = "file", required = false) MultipartFile file,
                                    @RequestParam("roomId") String roomId,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session) {
        try {
            log.info("🔍 Xử lý bài đăng của học sinh. Nội dung: {}", postContent);

            // 🟢 Lấy ID học sinh từ session
            String studentId = (String) session.getAttribute("StudentID");
            if (studentId == null) {
                log.error("🚫 Không tìm thấy ID học sinh.");
                redirectAttributes.addFlashAttribute("error", "Lỗi: Không tìm thấy ID học sinh.");
                return "redirect:/DangNhapHocSinh";
            }

            // 📚 Lấy thông tin học sinh
            Students student = entityManager.find(Students.class, studentId);
            if (student == null) {
                throw new IllegalArgumentException("Không tìm thấy học sinh với ID: " + studentId);
            }

            // 📝 Tạo bài đăng mới
            Posts newPost = new Posts();
            newPost.setContent(postContent);
            newPost.setCreator(student);
            newPost.setCreatedAt(LocalDateTime.now());

            // 🏫 Lấy phòng học
            Rooms room = entityManager.find(Rooms.class, roomId);
            if (room == null) {
                throw new IllegalArgumentException("Không tìm thấy phòng học với ID: " + roomId);
            }
            newPost.setRoom(room);

            // 💾 Lưu bài post vào DB
            entityManager.persist(newPost);
            log.info("✅ Bài đăng đã được lưu với ID: {}", newPost.getPostId());

            // 📂 Xử lý tệp đính kèm (nếu có)
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
                document.setCreator(student);
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

        return "redirect:/ChiTietLopHocHocSinh/" + roomId;
    }
    @PostMapping("/GuiNhanXetGiaoVien")
    @Transactional
    public String guiNhanXetGiaoVien(@RequestParam("teacherId") String teacherId,
                                     @RequestParam("text") String text,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        try {
            log.info("📝 Nhận xét giáo viên với ID: {}", teacherId);

            // Lấy ID học sinh từ session
            Students student = entityManager.find(Students.class, session.getAttribute("StudentID"));


            // Lấy thông tin giáo viên từ database
            Teachers teacher = entityManager.find(Teachers.class, teacherId);
            if (teacher == null) {
                throw new IllegalArgumentException("Không tìm thấy giáo viên với ID: " + teacherId);
            }
            // Tạo nhận xét mới
            Feedbacks feedback = new Feedbacks();
            feedback.setReviewer(student);
            feedback.setTeacher(teacher);
            feedback.setReceiver(student.getEmployee());
            feedback.setText(text);
            feedback.setCreatedAt(LocalDateTime.now());

            // Lưu vào database
            entityManager.persist(feedback);
            log.info("✅ Nhận xét đã được lưu thành công.");

            // Gửi thông báo về giao diện
            redirectAttributes.addFlashAttribute("message", "Nhận xét của bạn đã được gửi!");

        } catch (Exception e) {
            log.error("❌ Lỗi khi gửi nhận xét: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra, vui lòng thử lại.");
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return "redirect:/TrangChuHocSinh";
    }




}
