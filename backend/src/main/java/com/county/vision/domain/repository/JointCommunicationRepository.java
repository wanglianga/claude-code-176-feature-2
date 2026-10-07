package com.county.vision.domain.repository;

import com.county.vision.domain.entity.JointCommunication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JointCommunicationRepository extends JpaRepository<JointCommunication, Long> {
    List<JointCommunication> findByStudentIdOrderByIdAsc(Long studentId);
    List<JointCommunication> findByStatusOrderByApplyTimeDesc(
            com.county.vision.domain.enums.JointCommStatus status);
    List<JointCommunication> findAllByOrderByApplyTimeDesc();
}
