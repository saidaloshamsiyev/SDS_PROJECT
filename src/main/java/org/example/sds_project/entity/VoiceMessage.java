package org.example.sds_project.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity(name = "voice_messages")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class VoiceMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String transcription;  //matnga aylangan yozuv

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
