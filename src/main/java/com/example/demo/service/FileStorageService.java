package com.example.demo.service;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileStorageService {

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket-name}")
    private String bucketName;

    public FileStorageService(Storage storage) {
        this.storage = storage;
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        // Upload tệp lên bucket
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();

        storage.create(blobInfo, file.getBytes());

        // Trả về URL công khai nếu cần
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }

    // Xóa file từ GCS
    public boolean deleteFile(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        return storage.delete(blobId);
    }
}
