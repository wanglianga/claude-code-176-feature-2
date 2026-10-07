package com.county.vision.domain.repository;

import com.county.vision.domain.entity.RefusalFollowUpNode;
import com.county.vision.domain.enums.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RefusalFollowUpNodeRepository extends JpaRepository<RefusalFollowUpNode, Long> {

    /** 学生的全部跟进节点，按链路顺序（序号、时间）排列 */
    List<RefusalFollowUpNode> findByStudentIdOrderBySeqAscIdAsc(Long studentId);

    /** 该学生某类型的全部节点（拒绝可有多条，绝不能用 findFirst 覆盖） */
    List<RefusalFollowUpNode> findByStudentIdAndNodeTypeOrderBySeqAscIdAsc(Long studentId, NodeType nodeType);

    /** 最近一个跟进节点 */
    Optional<RefusalFollowUpNode> findFirstByStudentIdOrderBySeqDescIdDesc(Long studentId);

    /**
     * 到期/逾期待回访：跟进中、约定回访未完成，且“有效约定日期”（优先下一次约定，否则本节点约定）已到。
     */
    @Query("select n from RefusalFollowUpNode n where n.status = 'ACTIVE' and n.returnDone = false and " +
            "coalesce(n.nextPlannedReturnDate, n.plannedReturnDate) is not null and " +
            "coalesce(n.nextPlannedReturnDate, n.plannedReturnDate) <= :date " +
            "order by coalesce(n.nextPlannedReturnDate, n.plannedReturnDate) asc, n.id asc")
    List<RefusalFollowUpNode> findDueReturnVisits(@Param("date") LocalDate date);

    /** 已安排约定回访日期、仍在跟进中的节点（健康老师据此在约定时间发起回访） */
    List<RefusalFollowUpNode> findByStatusAndPlannedReturnDateIsNotNullOrderByPlannedReturnDateAsc(String status);

    /** 全部处于某状态的节点（用于学校列表/监管） */
    List<RefusalFollowUpNode> findByStatusOrderByIdAsc(String status);

    long countByNodeTypeAndStatus(NodeType nodeType, String status);

    long countByStudentId(Long studentId);
}

