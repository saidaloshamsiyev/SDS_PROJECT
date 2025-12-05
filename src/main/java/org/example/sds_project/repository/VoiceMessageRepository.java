package org.example.sds_project.repository;

import org.example.sds_project.entity.VoiceMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VoiceMessageRepository extends JpaRepository<VoiceMessage, Long> {
}
