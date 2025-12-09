package org.example.sds_project.service.aiservice;

import lombok.RequiredArgsConstructor;
import org.example.sds_project.entity.VoiceMessage;
import org.example.sds_project.repository.VoiceMessageRepository;
import org.example.sds_project.service.stratagey.TranscriptionStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ElevenLabsService implements TranscriptionStrategy {

    private final VoiceMessageRepository voiceMessageRepository;

    @Value("${elevenlabs.api.url}")
    private String apiUrl;

    @Value("${elevenlabs.api.key}")
    private String apiKey;


    @Override
    public VoiceMessage transcribeAudio(MultipartFile multipartFile) {
        // ElevenLabs faylni File formatida so'raydi
        File convFile = convertMultiPartToFile(multipartFile);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();

            // ELEVENLABS MAXSUS HEADER (Authorization emas!)
            headers.set("xi-api-key", apiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(convFile));
            body.add("model_id", "scribe_v1"); // Scribe modeli (STT uchun)
            // body.add("language_code", "uz"); // Agar o'zbek tilini qo'shishsa keyinchalik ochasiz

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // So'rov yuborish
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, requestEntity, Map.class);

            // Javobni olish
            String text = (String) response.getBody().get("text");

            VoiceMessage message = new VoiceMessage();
            message.setFileName(multipartFile.getOriginalFilename());
            message.setTranscription(text);

            return voiceMessageRepository.save(message);

        } catch (Exception e) {
            throw new RuntimeException("ElevenLabs API xatosi: " + e.getMessage());
        } finally {
            // Vaqtincha faylni o'chirish
            convFile.delete();
        }
    }

    @Override
    public String getProviderName() {
        return "elevenlabs";
    }

    private File convertMultiPartToFile(MultipartFile file) {
        try {
            File convFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(convFile)) {
                fos.write(file.getBytes());
            }
            return convFile;
        } catch (IOException e) {
            throw new RuntimeException("Fayl konvertatsiya xatosi");
        }
    }
}