package org.example.sds_project.service;

import lombok.RequiredArgsConstructor;
import org.example.sds_project.entity.VoiceMessage;
import org.example.sds_project.repository.VoiceMessageRepository;
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
public class TranscriptionService {

    private final VoiceMessageRepository voiceMessageRepository;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.model}")
    private String apiModel;


    public VoiceMessage transcribeAudio(MultipartFile audioFile) throws IOException {
        File convFile = convertMultiPartToFile(audioFile);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);


        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(convFile));
        body.add("model", apiModel);
        body.add("language", "uz");


        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            String text = (String) response.getBody().get("text");

            VoiceMessage message = new VoiceMessage();
            message.setFileName(audioFile.getOriginalFilename());
            message.setTranscription(text);


            convFile.delete();
            return voiceMessageRepository.save(message);
        } catch (Exception e) {
            convFile.delete();
            throw new RuntimeException("Api bilan ishlashda xatolik yuz berdi: " + e.getMessage());
        }

    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File conFile = new File(System.getProperty("java.io.tmpdir") + "/" + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(conFile)) {
            fos.write(file.getBytes());
        }
        return conFile;
    }
}
