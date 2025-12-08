package org.example.sds_project.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import lombok.RequiredArgsConstructor;
import org.example.sds_project.entity.VoiceMessage;
import org.example.sds_project.service.TranscriptionFactory;
import org.example.sds_project.service.TranscriptionStrategy;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VoiceController {

    private final TranscriptionFactory transcriptionFactory;


    @Operation(
            summary = "Audio faylni yuklash va matnga aylantirish",
            description = "Ushbu endpoint audio faylni qabul qiladi va uni matnga aylantiradi."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @Parameter(description = "Audio fayl tanlang", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE))
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "model", defaultValue = "gemini") String model

    ) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {

            TranscriptionStrategy strategy = transcriptionFactory.getStrategy(model);

            VoiceMessage result = strategy.transcribeAudio(file);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing the audio file: " + e.getMessage());
        }
    }

}
