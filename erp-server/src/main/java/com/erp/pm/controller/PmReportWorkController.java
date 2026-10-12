package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pm.domain.dto.PmReportWorkSaveRequest;
import com.erp.pm.domain.vo.PmReportWorkVO;
import com.erp.pm.service.PmReportWorkService;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pm/report")
@RequiredArgsConstructor
public class PmReportWorkController {

    private final PmReportWorkService reportWorkService;

    @GetMapping("/page")
    @RequiresPermission("pm:report:query")
    public Result<PageResult<PmReportWorkVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   @RequestParam(required = false) Long moId,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String reportNo) {
        return Result.ok(reportWorkService.pageReport(pageNum, pageSize, moId, status, reportNo));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pm:report:query")
    public Result<PmReportWorkVO> detail(@PathVariable Long id) {
        return Result.ok(reportWorkService.getReportDetail(id));
    }

    @PostMapping
    @RequiresPermission("pm:report:add")
    @OperLog(module = "pm", type = "CREATE", value = "新增生产报工单")
    public Result<Long> create(@Valid @RequestBody PmReportWorkSaveRequest request) {
        return Result.ok(reportWorkService.createReport(request));
    }

    @PutMapping
    @RequiresPermission("pm:report:edit")
    @OperLog(module = "pm", type = "UPDATE", value = "修改生产报工单")
    public Result<Void> update(@Valid @RequestBody PmReportWorkSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        reportWorkService.updateReport(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pm:report:remove")
    @OperLog(module = "pm", type = "DELETE", value = "删除生产报工单")
    public Result<Void> delete(@PathVariable Long id) {
        reportWorkService.deleteReport(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("pm:report:audit")
    @OperLog(module = "pm", type = "AUDIT", value = "审核生产报工单")
    public Result<Void> audit(@PathVariable Long id) {
        reportWorkService.auditReport(id);
        return Result.ok();
    }
}








