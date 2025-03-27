package com.example.demo.ControllerGET;


import com.example.demo.FileStorageService.FileStorageService;
import com.example.demo.ModelOOP.Documents;
import com.example.demo.ModelOOP.Person;
import com.example.demo.ModelOOP.Posts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam("documentTitle") String documentTitle,
                                             @RequestParam("postId") Long postId,
                                             HttpSession session) {
        try {
            if (file.isEmpty()) {
                log.error("⚠️ Tệp trống.");
                return ResponseEntity.badRequest().body("⚠️ Tệp tải lên trống.");
            }

            log.info("📤 Tệp nhận được: {} - Kích thước: {} bytes", file.getOriginalFilename(), file.getSize());

            // Lấy người tạo
            String teacherId = (String) session.getAttribute("TeacherID");
            if (teacherId == null) {
                log.error("🚫 Không tìm thấy ID giáo viên.");
                return ResponseEntity.badRequest().body("🚫 Không tìm thấy ID giáo viên.");
            }

            Person creator = entityManager.find(Person.class, teacherId);
            Posts post = entityManager.find(Posts.class, postId);

            if (creator == null || post == null) {
                log.error("❌ Không tìm thấy người tạo hoặc bài viết.");
                return ResponseEntity.badRequest().body("❌ Không tìm thấy người tạo hoặc bài viết.");
            }

            // Upload lên GCS
            String fileUrl = fileStorageService.uploadFile(file);
            log.info("🌐 URL tệp: {}", fileUrl);

            // Tạo đối tượng Document
            Documents document = new Documents();
            document.setDocumentTitle(documentTitle);
            document.setCreator(creator);
            document.setPost(post);
            document.setFilePath(fileUrl);
            document.setFileData(file.getBytes());  // 🌟 Lưu dữ liệu

            log.info("📥 Dữ liệu tệp (bytes): {}", document.getFileData().length);

            entityManager.persist(document);
            entityManager.flush();

            log.info("✅ Đã lưu DocumentID: {}", document.getDocumentId());
            return ResponseEntity.ok("📄 Tệp tải lên thành công.");
        } catch (IOException e) {
            log.error("🚫 Lỗi đọc tệp: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("❌ Lỗi đọc tệp.");
        } catch (Exception e) {
            log.error("🚫 Lỗi hệ thống: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("❌ Lỗi hệ thống.");
        }
    }

    @GetMapping("/download/{documentId}")
    public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable Long documentId) {
        Documents document = entityManager.find(Documents.class, documentId);

        if (document == null || document.getFileData() == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = determineContentType(document.getDocumentTitle());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getDocumentTitle() + "\"")
                .body(new ByteArrayResource(document.getFileData()));
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (fileName.endsWith(".doc") || fileName.endsWith(".docx")) {
            return "application/msword";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else {
            return "application/octet-stream";
        }
    }


    @DeleteMapping("/delete/{documentId}")
    @Transactional
    public ResponseEntity<String> deleteFile(@PathVariable Long documentId) {
        Documents document = entityManager.find(Documents.class, documentId);
        if (document == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("❌ Không tìm thấy tài liệu với ID: " + documentId);
        }

        entityManager.remove(document);
        entityManager.flush();

        log.info("🗑️ Đã xóa tài liệu với ID: {}", documentId);
        return ResponseEntity.ok("✅ Tệp đã được xóa thành công.");
    }

    @GetMapping("/test")
    public ResponseEntity<String> testApi() {
        return ResponseEntity.ok("✅ API hoạt động bình thường!");
    }
}
