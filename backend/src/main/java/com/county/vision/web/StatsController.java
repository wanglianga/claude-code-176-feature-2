package com.county.vision.web;

import com.county.vision.common.ApiResponse;
import com.county.vision.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "县域监管统计看板")
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @Operation(summary = "总览指标（筛查率/异常率/真实复诊率/闭环率/补助等）")
    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(@RequestParam(required = false) Long schoolId,
                                                     @RequestParam(required = false) Long regionId) {
        return ApiResponse.ok(stats.overview(schoolId, regionId));
    }

    @Operation(summary = "按年级的近视发展趋势")
    @GetMapping("/trend-grade")
    public ApiResponse<List<Map<String, Object>>> trendByGrade(@RequestParam(required = false) Long schoolId,
                                                               @RequestParam(required = false) Long regionId) {
        return ApiResponse.ok(stats.trendByGrade(schoolId, regionId));
    }

    @Operation(summary = "按学校对比（真实复诊率/干预执行）")
    @GetMapping("/by-school")
    public ApiResponse<List<Map<String, Object>>> bySchool(@RequestParam(required = false) Long regionId) {
        return ApiResponse.ok(stats.bySchool(regionId));
    }

    @Operation(summary = "按片区对比")
    @GetMapping("/by-region")
    public ApiResponse<List<Map<String, Object>>> byRegion() {
        return ApiResponse.ok(stats.byRegion());
    }

    @Operation(summary = "各类异常事项分布（待处理/已闭环，含家长拒诊/连续拒绝风险升高）")
    @GetMapping("/issues")
    public ApiResponse<List<Map<String, Object>>> issues() {
        return ApiResponse.ok(stats.issueDistribution());
    }

    @Operation(summary = "健康管理阶段漏斗")
    @GetMapping("/stage-funnel")
    public ApiResponse<List<Map<String, Object>>> stageFunnel(@RequestParam(required = false) Long schoolId,
                                                              @RequestParam(required = false) Long regionId) {
        return ApiResponse.ok(stats.stageFunnel(schoolId, regionId));
    }
}
