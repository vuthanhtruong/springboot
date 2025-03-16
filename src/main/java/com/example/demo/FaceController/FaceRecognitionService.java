package com.example.demo.FaceController;

import com.example.demo.OOP.Person;
import com.example.demo.Repository.PersonRepository;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class FaceRecognitionService {

    private static final String API_KEY = "lDjG0cUY_UpZGb45HBGKMM0ekM4CqRYv";
    private static final String API_SECRET = "tpugTFVboS3YeRTiklf_CgKZigHwFYYg";
    private static final String FACE_API_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";
    private final PersonRepository personRepository;
    private final OkHttpClient client = new OkHttpClient();

    public FaceRecognitionService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    public String findPersonIdByFace(String faceData) {
        try {
            if (faceData == null || faceData.trim().isEmpty()) {
                System.err.println("Input face data is null or empty");
                return null;
            }

            String cleanedFaceData = cleanBase64(faceData);
            if (cleanedFaceData == null || cleanedFaceData.isEmpty()) {
                System.err.println("Cleaned input face data is invalid");
                return null;
            }
            System.out.println("Input face data length: " + cleanedFaceData.length());

            for (Person person : personRepository.findAll()) {
                String storedFaceData = person.getFaceData();
                if (storedFaceData != null && !storedFaceData.trim().isEmpty()) {
                    String cleanedStoredFaceData = cleanBase64(storedFaceData);
                    if (cleanedStoredFaceData != null && !cleanedStoredFaceData.isEmpty()) {
                        System.out.println("Comparing with ID: " + person.getId() +
                                ", stored face data length: " + cleanedStoredFaceData.length());
                        if (compareFaces(cleanedFaceData, cleanedStoredFaceData)) {
                            System.out.println("Matched person ID: " + person.getId());
                            return person.getId();
                        }
                    } else {
                        System.err.println("Invalid stored face data for ID: " + person.getId() +
                                ", original: " + storedFaceData.substring(0, Math.min(50, storedFaceData.length())));
                    }
                }
            }
            System.err.println("No matching face found in database");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean compareFaces(String faceData1, String faceData2) {
        try {
            if (faceData1 == null || faceData2 == null || faceData1.isEmpty() || faceData2.isEmpty()) {
                System.err.println("Invalid face data: faceData1 length=" + (faceData1 != null ? faceData1.length() : "null") +
                        ", faceData2 length=" + (faceData2 != null ? faceData2.length() : "null"));
                return false;
            }

            RequestBody body = new FormBody.Builder()
                    .add("api_key", API_KEY)
                    .add("api_secret", API_SECRET)
                    .add("image_base64_1", faceData1)
                    .add("image_base64_2", faceData2)
                    .build();

            Request request = new Request.Builder()
                    .url(FACE_API_URL)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                if (!response.isSuccessful()) {
                    System.err.println("Face++ API error: " + responseBody);
                    return false;
                }

                System.out.println("Face++ response: " + responseBody);
                JSONObject jsonObject = new JSONObject(responseBody);

                if (jsonObject.has("confidence")) {
                    double confidence = jsonObject.getDouble("confidence");
                    return confidence > 80.0;
                } else {
                    System.err.println("No confidence value in Face++ response");
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String cleanBase64(String base64Data) {
        if (base64Data == null || base64Data.trim().isEmpty()) {
            return null;
        }
        if (base64Data.startsWith("data:image")) {
            return base64Data.substring(base64Data.indexOf(",") + 1).trim();
        }
        if (!base64Data.matches("^[A-Za-z0-9+/=]+$")) {
            System.err.println("Invalid base64 format: " + base64Data.substring(0, Math.min(50, base64Data.length())));
            return null;
        }
        return base64Data.trim();
    }

    public boolean verifyFace(String base64FaceData) {
        return base64FaceData != null && !base64FaceData.isEmpty();
    }

    public String processFaceData(String faceData) {
        try {
            if (faceData != null && !faceData.isEmpty()) {
                return "Face recognized successfully!";
            } else {
                return "No face detected.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing face data.";
        }
    }
}