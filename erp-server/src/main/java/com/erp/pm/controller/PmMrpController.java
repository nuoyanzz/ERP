package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.pm.domain.vo.MrpReleaseResultVO;
import com.erp.pm.domain.vo.MrpSuggestionVO;
import com.erp.pm.service.PmMrpService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pm/mrp")
@RequiredArgsConstructor
public class PmMrpController {

    private final PmMrpService mrpService;

    @PostMapping("/run")
    @RequiresPermission("pm:mrp:run")
    @OperLog(module = "pm")
    public Result<List<MrpSuggestionVO>> run() {
        return Result.ok(mrpService.runMrp());
    }

    @GetMapping("/page")
    @RequiresPermission("pm:mrp:query")
    @OperLog(module = "pm")
    public Result<PageResult<MrpSuggestionVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                    @RequestParam(defaultValue = "10") int pageSize,
                                                    @RequestParam(required = false) String runNo,
                                                    @RequestParam(required = false) Long materialId,
                                                    @RequestParam(required = false) Integer suggestType) {
        return Result.ok(mrpService.pageResult(pageNum, pageSize, runNo, materialId, suggestType));
    }

    @GetMapping("/latest-run-no")
    @RequiresPermission("pm:mrp:query")
    @OperLog(module = "pm")
    public Result<String> latestRunNo() {
        return Result.ok(mrpService.latestRunNo());
    }

    @PostMapping("/release-purchase")
    @RequiresPermission("pm:mrp:releasePurchase")
    @OperLog(module = "pm", type = "RELEASE", value = "MRP 投放采购订单")
    public Result<MrpReleaseResultVO> releasePurchase(@RequestParam String runNo) {
        return Result.ok(mrpService.releasePurchaseOrder(runNo));
    }

    @PostMapping("/release-production")
    @RequiresPermission("pm:mrp:releaseProduction")
    @OperLog(module = "pm", type = "RELEASE", value = "MRP 投放生产订单")
    public Result<MrpReleaseResultVO> releaseProduction(@RequestParam String runNo) {
        return Result.ok(mrpService.releaseProductionOrder(runNo));
    }
}








