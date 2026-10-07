package com.county.vision.web;

import com.county.vision.common.ApiResponse;
import com.county.vision.service.QueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "台账（异常事项/复查计划/补助）")
@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final QueryService queryService;

    public LedgerController(QueryService queryService) {
        this.queryService = queryService;
    }

    @Operation(summary = "异常事项台账（OPEN/RESOLVED/ALL）")
    @GetMapping("/issues")
    public ApiResponse<List<Map<String, Object>>> issues(@RequestParam(required = false) String status) {
        return ApiResponse.ok(queryService.issues(status));
    }

    @Operation(summary = "复查随访台账（PLANNED/NO_SHOW/DONE/ALL）")
    @GetMapping("/followups")
    public ApiResponse<List<Map<String, Object>>> followups(@RequestParam(required = false) String status) {
        return ApiResponse.ok(queryService.followups(status));
    }

    @Operation(summary = "补助申请台账（按状态过滤）")
    @GetMapping("/subsidies")
    public ApiResponse<List<Map<String, Object>>> subsidies(@RequestParam(required = false) String status) {
        return ApiResponse.ok(queryService.subsidies(status));
    }

    @Operation(summary = "家长拒诊跟进台账（dueOnly=true 仅返回到约定回访时间、仍在拒绝的节点）")
    @GetMapping("/refusals")
    public ApiResponse<List<Map<String, Object>>> refusals(
            @RequestParam(required = false, defaultValue = "false") boolean dueOnly) {
        return ApiResponse.ok(queryService.refusalLedger(dueOnly));
    }

    @Operation(summary = "多方共同沟通台账（APPLIED/ACCEPTED/DONE/REJECTED/ALL）")
    @GetMapping("/joint-comms")
    public ApiResponse<List<Map<String, Object>>> jointComms(@RequestParam(required = false) String status) {
        return ApiResponse.ok(queryService.jointCommLedger(status));
    }
}
