package com.example.demo.VoiceController;

import com.example.demo.OOP.Person;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

@Service
public class VoiceRecognitionService {

    private final String subscriptionKey = "YOUR_AZURE_SUBSCRIPTION_KEY";
    private final String region = "YOUR_AZURE_REGION"; // e.g., "westus"

    @PersistenceContext
    private EntityManager entityManager;


    public String findUserIdByVoice(String voiceData) throws Exception {
        if (voiceData == null || voiceData.isEmpty()) {
            return null;
        }

        String base64Data = voiceData.split(",")[1];
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);
        File tempFile = File.createTempFile("voice", ".wav");
        Files.write(tempFile.toPath(), audioBytes);

        List<Person> persons = entityManager.createQuery("SELECT p FROM Person p WHERE p.voiceData IS NOT NULL", Person.class)
                .getResultList();

        if (persons.isEmpty()) {
            tempFile.delete();
            return null;
        }

        String profileIds = persons.stream()
                .map(Person::getVoiceData)
                .reduce((a, b) -> a + "," + b)
                .get();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + region + ".api.cognitive.microsoft.com/sts/v1.0/issuetoken"))
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .POST(HttpRequest.BodyPublishers.ofFile(tempFile.toPath()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        // Giả định response chứa profileId (cần parse JSON thực tế)
        if (responseBody.contains("profileId")) {
            String profileId = parseProfileIdFromResponse(responseBody); // Hàm tự định nghĩa
            tempFile.delete();
            return profileId;
        }

        tempFile.delete();
        return null;
    }

    public String registerVoiceProfile(String voiceData) throws Exception {
        if (voiceData == null || voiceData.isEmpty()) {
            return null;
        }

        String base64Data = voiceData.split(",")[1];
        byte[] audioBytes = Base64.getDecoder().decode(base64Data);
        File tempFile = File.createTempFile("voice", ".wav");
        Files.write(tempFile.toPath(), audioBytes);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + region + ".api.cognitive.microsoft.com/sts/v1.0/issuetoken"))
                .header("Ocp-Apim-Subscription-Key", subscriptionKey)
                .POST(HttpRequest.BodyPublishers.ofFile(tempFile.toPath()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String profileId = parseProfileIdFromResponse(response.body()); // Hàm tự định nghĩa

        tempFile.delete();
        return profileId;
    }

    private String parseProfileIdFromResponse(String responseBody) {
        // Logic parse JSON để lấy profileId (cần thư viện như Jackson nếu muốn chính xác)
        return "mock-profile-id"; // Thay bằng logic thực tế
    }
}