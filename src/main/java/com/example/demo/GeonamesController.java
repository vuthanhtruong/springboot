package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/geonames")
public class GeonamesController {

    private static final Logger logger = LoggerFactory.getLogger(GeonamesController.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${geonames.username}")
    private String geoNamesUsername;

    @GetMapping("/children")
    public ResponseEntity<?> getChildren(@RequestParam("geonameId") String geonameId) {
        if (geoNamesUsername == null || geoNamesUsername.trim().isEmpty()) {
            logger.error("GeoNames username không được cấu hình trong application.properties");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Cấu hình server lỗi: thiếu GeoNames username");
        }

        // Sử dụng HTTP thay vì HTTPS cho API miễn phí
        String url = "http://api.geonames.org/childrenJSON?geonameId=" + geonameId + "&username=" + geoNamesUsername;
        logger.info("Gửi yêu cầu tới GeoNames: {}", url);

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            logger.debug("Phản hồi từ GeoNames: {}", jsonResponse);

            if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
                logger.warn("Không nhận được dữ liệu từ GeoNames cho geonameId: {}", geonameId);
                return ResponseEntity.ok().body("{\"geonames\": []}");
            }

            Object response = objectMapper.readValue(jsonResponse, Object.class);
            return ResponseEntity.ok(response);
        } catch (HttpClientErrorException e) {
            logger.error("Lỗi HTTP từ GeoNames: Status={}, Response={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body("Lỗi từ GeoNames: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Lỗi khi gọi GeoNames API: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi tải dữ liệu từ GeoNames: " + e.getMessage());
        }
    }
}