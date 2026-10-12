package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.pm.domain.vo.PmManufacturingCostVO;
import com.erp.pm.domain.vo.PmMoCostVO;
import com.erp.pm.service.PmManufacturingCostService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pm/cost")
@RequiredArgsConstructor
public class PmManufacturingCostController {

    private final PmManufacturingCostService costService;

    @GetMapping("/page")
    @RequiresPermission("pm:cost:query")
    public Result<PageResult<PmMoCostVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize,
                                                @RequestParam(required = false) Long moId,
                                                @RequestParam(required = false) Long materialId) {
        return Result.ok(costService.pageMoCost(pageNum, pageSize, moId, materialId));
    }

    @GetMapping("/detail/{moId}")
    @RequiresPermission("pm:cost:query")
    public Result<PmMoCostVO> detail(@PathVariable Long moId) {
        return Result.ok(costService.getMoCostDetail(moId));
    }

    @GetMapping("/detail-page")
    @RequiresPermission("pm:cost:query")
    public Result<PageResult<PmManufacturingCostVO>> detailPage(@RequestParam(defaultValue = "1") int pageNum,
                                                                 @RequestParam(defaultValue = "10") int pageSize,
                                                                 @RequestParam(required = false) Long moId,
                                                                 @RequestParam(required = false) Integer costType) {
        return Result.ok(costService.pageCostDetail(pageNum, pageSize, moId, costType));
    }

    @GetMapping("/list")
    @RequiresPermission("pm:cost:query")
    public Result<List<PmMoCostVO>> list(@RequestParam(required = false) Long materialId) {
        return Result.ok(costService.listMoCost(materialId));
    }

    @GetMapping("/export")
    @RequiresPermission("pm:cost:export")
    @OperLog(module = "pm", type = "EXPORT", value = "导出生产成本归集")
    public Result<List<PmMoCostVO>> export(@RequestParam(required = false) Long materialId) {
        return Result.ok(costService.listMoCost(materialId));
    }
}








