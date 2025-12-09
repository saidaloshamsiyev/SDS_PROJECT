package org.example.sds_project.service.stratagey;

import org.example.sds_project.entity.VoiceMessage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public interface TranscriptionStrategy {
    VoiceMessage transcribeAudio(MultipartFile multipartFile);

    String getProviderName();
}
