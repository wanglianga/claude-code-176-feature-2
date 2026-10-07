package com.county.vision.web;

import com.county.vision.common.ApiResponse;
import com.county.vision.domain.entity.RefusalFollowUpNode;
import com.county.vision.dto.RecordDtos;
import com.county.vision.service.RefusalFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 家长拒绝复诊后的跟进接口。
 * 记录拒绝原因/风险告知/确认时间/沟通人员、约定回访、下降后重新提醒、多方共同沟通，全程追加留痕。
 */
@Tag(name = "家长拒绝复诊跟进（拒绝留痕/约定回访/下降重提醒/多方沟通）")
@RestController
@RequestMapping("/api/refusal")
public class RefusalController {

    private final RefusalFollowService refusalService;

    public RefusalController(RefusalFollowService refusalService) {
        this.refusalService = refusalService;
    }

    @Operation(summary = "记录家长拒绝复诊（拒绝原因/风险告知内容/确认时间/沟通人员，只追加不覆盖）")
    @PostMapping("/record")
    public ApiResponse<Long> record(@RequestBody RecordDtos.RefusalRequest req) {
        RefusalFollowUpNode node = refusalService.recordRefusal(req);
        return ApiResponse.ok(node.getId());
    }

    @Operation(summary = "健康老师在约定时间回访（座位/作业用眼/户外活动/家长态度，与前次筛查同档比较）")
    @PostMapping("/return-visit")
    public ApiResponse<Long> returnVisit(@RequestBody RecordDtos.RefusalReturnVisitRequest req) {
        return ApiResponse.ok(refusalService.returnVisit(req).getId());
    }

    @Operation(summary = "连续拒绝且风险升高：申请班主任/社区医生共同沟通，登记沟通结果与学校干预措施")
    @PostMapping("/joint-comm")
    public ApiResponse<Long> jointComm(@RequestBody RecordDtos.JointCommRequest req) {
        return ApiResponse.ok(refusalService.jointComm(req).getId());
    }

    @Operation(summary = "拒诊跟进中的学生列表（含拒绝次数、风险标记、最近约定回访日期）")
    @GetMapping("/students")
    public ApiResponse<List<Map<String, Object>>> students(@RequestParam(required = false) Long schoolId) {
        return ApiResponse.ok(refusalService.activeStudents(schoolId));
    }

    @Operation(summary = "到期/逾期待回访列表（健康老师在约定时间据此发起回访）")
    @GetMapping("/due-return-visits")
    public ApiResponse<List<Map<String, Object>>> dueReturnVisits(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(refusalService.dueReturnVisits(date));
    }

    @Operation(summary = "连续拒绝且风险持续升高、可申请共同沟通的学生")
    @GetMapping("/joint-candidates")
    public ApiResponse<List<Map<String, Object>>> jointCandidates() {
        return ApiResponse.ok(refusalService.jointCandidates());
    }

    @Operation(summary = "某学生的全部拒诊跟进节点（拒绝/回访/再提醒/共同沟通，按时间链路）")
    @GetMapping("/students/{id}/nodes")
    public ApiResponse<List<Map<String, Object>>> nodes(@PathVariable Long id) {
        return ApiResponse.ok(refusalService.nodesOfStudent(id));
    }
}
