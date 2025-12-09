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

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeepGramService implements TranscriptionStrategy {

    private final VoiceMessageRepository voiceMessageRepository;

    @Value("${deepgram.api.url}")
    private String apiUrl;

    @Value("${deepgram.api.key}")
    private String apiKey;

    @Override
    public VoiceMessage transcribeAudio(MultipartFile multipartFile) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Token " + apiKey);
            // Deepgram faylni binary stream qilib yuborishni so'raydi (o'zbek tili uchun 'uz')
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            // URL ga parametrlar qo'shamiz (model=nova-2 bu eng kuchlisi, language=uz)
            String finalUrl = apiUrl + "?model=whisper-large&language=uz&smart_format=true";

            // Faylni byte array qilib yuboramiz
            byte[] fileBytes = multipartFile.getBytes();
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(finalUrl, requestEntity, Map.class);

            // Javobni o'qiymiz
            String transcribedText = extractTextFromDeepgram(response.getBody());

            VoiceMessage message = new VoiceMessage();
            message.setFileName(multipartFile.getOriginalFilename());
            message.setTranscription(transcribedText);

            return voiceMessageRepository.save(message);

        } catch (IOException e) {
            throw new RuntimeException("Fayl o'qishda xato: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Deepgram API xatosi: " + e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "deepgram";
    }

    // Deepgram JSON javobidan matnni ajratib olish
    private String extractTextFromDeepgram(Map<String, Object> body) {
        try {
            Map<String, Object> results = (Map<String, Object>) body.get("results");
            var channels = (java.util.List<Map<String, Object>>) results.get("channels");
            var alternatives = (java.util.List<Map<String, Object>>) channels.get(0).get("alternatives");
            return (String) alternatives.get(0).get("transcript");
        } catch (Exception e) {
            return "Matn aniqlanmadi";
        }
    }
}