package com.erp.sa.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.sa.domain.dto.SaOutstockQueryRequest;
import com.erp.sa.domain.dto.SaOutstockSaveRequest;
import com.erp.sa.domain.vo.SaOutstockDetailVO;
import com.erp.sa.domain.vo.SaOutstockListVO;
import com.erp.sa.service.SaOutstockService;
import jakarta.validation.Valid;
import com.erp.security.annotation.RequiresPermission;
import com.erp.system.annotation.OperLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sa/outstock")
@RequiredArgsConstructor
public class SaOutstockController {

    private final SaOutstockService outstockService;

    @GetMapping("/page")
    @RequiresPermission("sa:outstock:query")
    @OperLog(module = "sa")
    public Result<PageResult<SaOutstockListVO>> page(SaOutstockQueryRequest request) {
        return Result.ok(outstockService.pageOutstock(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sa:outstock:query")
    @OperLog(module = "sa")
    public Result<SaOutstockDetailVO> detail(@PathVariable Long id) {
        return Result.ok(outstockService.getOutstockDetail(id));
    }

    @PostMapping
    @RequiresPermission("sa:outstock:add")
    @OperLog(module = "sa")
    public Result<Long> create(@Valid @RequestBody SaOutstockSaveRequest request) {
        return Result.ok(outstockService.createOutstock(request));
    }

    @PutMapping
    @RequiresPermission("sa:outstock:edit")
    @OperLog(module = "sa")
    public Result<Void> update(@Valid @RequestBody SaOutstockSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        outstockService.updateOutstock(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sa:outstock:remove")
    @OperLog(module = "sa")
    public Result<Void> delete(@PathVariable Long id) {
        outstockService.deleteOutstock(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("sa:outstock:audit")
    @OperLog(module = "sa")
    public Result<Void> audit(@PathVariable Long id) {
        outstockService.auditOutstock(id);
        return Result.ok();
    }
}








