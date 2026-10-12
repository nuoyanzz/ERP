package com.erp.sa.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.sa.domain.dto.SaReturnQueryRequest;
import com.erp.sa.domain.dto.SaReturnSaveRequest;
import com.erp.sa.domain.vo.SaReturnDetailVO;
import com.erp.sa.domain.vo.SaReturnListVO;
import com.erp.sa.service.SaReturnService;
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
@RequestMapping("/sa/return")
@RequiredArgsConstructor
public class SaReturnController {

    private final SaReturnService returnService;

    @GetMapping("/page")
    @RequiresPermission("sa:return:query")
    @OperLog(module = "sa")
    public Result<PageResult<SaReturnListVO>> page(SaReturnQueryRequest request) {
        return Result.ok(returnService.pageReturn(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("sa:return:query")
    @OperLog(module = "sa")
    public Result<SaReturnDetailVO> detail(@PathVariable Long id) {
        return Result.ok(returnService.getReturnDetail(id));
    }

    @PostMapping
    @RequiresPermission("sa:return:add")
    @OperLog(module = "sa")
    public Result<Long> create(@Valid @RequestBody SaReturnSaveRequest request) {
        return Result.ok(returnService.createReturn(request));
    }

    @PutMapping
    @RequiresPermission("sa:return:edit")
    @OperLog(module = "sa")
    public Result<Void> update(@Valid @RequestBody SaReturnSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        returnService.updateReturn(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sa:return:remove")
    @OperLog(module = "sa")
    public Result<Void> delete(@PathVariable Long id) {
        returnService.deleteReturn(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("sa:return:audit")
    @OperLog(module = "sa")
    public Result<Void> audit(@PathVariable Long id) {
        returnService.auditReturn(id);
        return Result.ok();
    }
}








