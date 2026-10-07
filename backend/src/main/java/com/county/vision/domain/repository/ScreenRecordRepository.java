package com.county.vision.domain.repository;

import com.county.vision.domain.entity.ScreenRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScreenRecordRepository extends JpaRepository<ScreenRecord, Long> {
    Optional<ScreenRecord> findFirstByStudentIdOrderByScreenDateDescIdDesc(Long studentId);
    /** 最近两次筛查（最新在前），用于判断后续筛查是否继续下降 */
    List<ScreenRecord> findFirst2ByStudentIdOrderByScreenDateDescIdDesc(Long studentId);
    /** 该学生全部筛查记录（时间正序），档案中逐次比较 */
    List<ScreenRecord> findByStudentIdOrderByScreenDateAscIdAsc(Long studentId);
    long countByAbnormalTrue();
}
