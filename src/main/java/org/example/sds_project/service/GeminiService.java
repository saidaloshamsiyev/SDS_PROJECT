package org.example.sds_project.service;

import lombok.RequiredArgsConstructor;
import org.example.sds_project.entity.VoiceMessage;
import org.example.sds_project.repository.VoiceMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService implements TranscriptionStrategy {

    private final VoiceMessageRepository voiceMessageRepository;

    @Value("${gemini.api.url}")
    private String apiUrl;


    @Value("${gemini.api.key}")
    private String apiKey;


    @Override
    public VoiceMessage transcribeAudio(MultipartFile multipartFile) {
        try {

            String clearApiKey = apiKey.trim();

            String clearApiUrl = apiUrl.trim();

            String finalUrl = clearApiUrl + "?key=" + clearApiKey;



            String base64Audio = Base64.getEncoder().encodeToString(multipartFile.getBytes());

            String mimeType = multipartFile.getContentType();

            if (mimeType == null || mimeType.equals("application/octet-stream")) {
                mimeType = "audio/mp3";
            }

            Map<String, Object> inlineData = new HashMap<>();
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Audio);


            Map<String, Object> audioPart = new HashMap<>();
            audioPart.put("inline_data", inlineData);


            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", "Ushbu audio o'zbek tilida. Uni matnga aylantirib ber. Faqat matnni o'zini yoz.");

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(textPart, audioPart));


            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(finalUrl, request, Map.class);

            String transcribedText = extractTextFromGeminiResponse(response.getBody());

            VoiceMessage voiceMessage = new VoiceMessage();
            voiceMessage.setFileName(multipartFile.getOriginalFilename());
            voiceMessage.setTranscription(transcribedText);

            return voiceMessageRepository.save(voiceMessage);
        } catch (Exception e) {
            throw new RuntimeException("Gemini API xatosi: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    private String extractTextFromGeminiResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "Matnni aniqlab bo'lmadi (Parse error)";
        }
    }
}
