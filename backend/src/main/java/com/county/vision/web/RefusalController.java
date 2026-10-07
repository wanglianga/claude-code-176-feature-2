package com.county.vision.web;

import com.county.vision.common.ApiResponse;
import com.county.vision.domain.entity.JointCommunication;
import com.county.vision.domain.entity.RefusalFollowUp;
import com.county.vision.dto.RecordDtos;
import com.county.vision.service.RefusalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 家长拒绝复诊后的持续跟进接口：
 * 拒绝登记（原因/风险告知/知情确认/沟通人员）、健康回访、共同沟通申请与结果。
 */
@Tag(name = "家长拒诊跟进（拒绝登记/健康回访/共同沟通）")
@RestController
@RequestMapping("/api/refusal")
public class RefusalController {

    private final RefusalService refusalService;

    public RefusalController(RefusalService refusalService) {
        this.refusalService = refusalService;
    }

    @Operation(summary = "登记家长拒绝复诊（记录拒绝原因、风险告知内容、知情确认、确认时间、沟通人员；形成新的跟进节点，不覆盖历史）")
    @PostMapping("/record")
    public ApiResponse<Long> record(@RequestBody RecordDtos.RefusalRequest req) {
        return ApiResponse.ok(refusalService.recordRefusal(req).getId());
    }

    @Operation(summary = "健康老师在约定时间回访（座位/作业用眼/户外活动/家长态度，与上次筛查同档比较；仍拒绝则形成新节点）")
    @PostMapping("/callback")
    public ApiResponse<Long> callback(@RequestBody RecordDtos.CallbackRequest req) {
        return ApiResponse.ok(refusalService.healthCallback(req).getId());
    }

    @Operation(summary = "连续拒绝且风险持续升高，学校申请班主任/社区医生共同沟通")
    @PostMapping("/joint/apply")
    public ApiResponse<Long> applyJoint(@RequestBody RecordDtos.JointCommRequest req) {
        return ApiResponse.ok(refusalService.applyJointComm(req).getId());
    }

    @Operation(summary = "受理/退回共同沟通申请（ACCEPTED/REJECTED）")
    @PostMapping("/joint/{id}/review")
    public ApiResponse<Long> reviewJoint(@PathVariable Long id,
                                         @RequestParam String action,
                                         @RequestParam(required = false) String opinion) {
        return ApiResponse.ok(refusalService.reviewJointComm(id, action, opinion).getId());
    }

    @Operation(summary = "登记共同沟通结果与学校干预措施（可追溯到学生；家长同意复诊则关闭拒诊跟进）")
    @PostMapping("/joint/result")
    public ApiResponse<Long> jointResult(@RequestBody RecordDtos.JointCommResultRequest req) {
        JointCommunication jc = refusalService.recordJointCommResult(req);
        return ApiResponse.ok(jc.getId());
    }
}
