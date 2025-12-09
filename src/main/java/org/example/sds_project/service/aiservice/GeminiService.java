package org.example.sds_project.service.aiservice;

import lombok.RequiredArgsConstructor;
import org.example.sds_project.entity.VoiceMessage;
import org.example.sds_project.repository.VoiceMessageRepository;
import org.example.sds_project.service.stratagey.TranscriptionStrategy;
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

            String prompt = """
                    Sen professional o'zbek tili muharririsiz. Quyidagi audio faylni tingla va undagi nutqni matnga aylantir.
                    
                    Quyidagi qoidalarga qat'iy rioya qil:
                    1. Faqat va faqat audiodagi gaplarni yoz. Ortiqcha izoh (masalan "Audio mazmuni", "Suhbat tugadi") qo'shma.
                    2. Agar gap shevada bo'lsa, uni adabiy o'zbek tiliga moslashtirib yoz (masalan: "kelyapman" deb yoz, "kevotman" emas).
                    3. Imlo xatolarini va tinish belgilarini (nuqta, vergul) to'g'ri qo'y.
                    4. Agar audioda ruscha yoki inglizcha terminlar bo'lsa, ularni o'z holicha to'g'ri yoz.
                    
                    Natijani faqat matn ko'rinishida chiqar.
                    """;


            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

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
