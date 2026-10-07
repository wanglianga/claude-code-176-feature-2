package com.county.vision.domain.repository;

import com.county.vision.domain.entity.ScreenRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScreenRecordRepository extends JpaRepository<ScreenRecord, Long> {
    Optional<ScreenRecord> findFirstByStudentIdOrderByScreenDateDescIdDesc(Long studentId);
    List<ScreenRecord> findByStudentIdOrderByScreenDateDescIdDesc(Long studentId);
    long countByAbnormalTrue();
}
