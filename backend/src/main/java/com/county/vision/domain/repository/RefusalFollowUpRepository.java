package com.county.vision.domain.repository;

import com.county.vision.domain.entity.RefusalFollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefusalFollowUpRepository extends JpaRepository<RefusalFollowUp, Long> {
    List<RefusalFollowUp> findByStudentIdOrderBySeqNoAscIdAsc(Long studentId);
    Optional<RefusalFollowUp> findFirstByStudentIdOrderBySeqNoDescIdDesc(Long studentId);
    long countByStudentId(Long studentId);
    /** 存在“风险知情确认仍拒绝”且尚未同意复诊/共同沟通完成的节点 */
    List<RefusalFollowUp> findByOutcome(com.county.vision.domain.enums.RefusalOutcome outcome);
}
