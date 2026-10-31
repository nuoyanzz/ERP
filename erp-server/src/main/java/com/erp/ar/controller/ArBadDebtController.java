package com.erp.ar.controller;

import com.erp.ar.domain.dto.ArBadDebtQueryRequest;
import com.erp.ar.domain.dto.ArBadDebtSaveRequest;
import com.erp.ar.domain.vo.ArBadDebtVO;
import com.erp.ar.service.ArBadDebtService;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * 应收账款坏账计提。
 */
@RestController
@RequestMapping("/ar/baddebt")
@RequiredArgsConstructor
public class ArBadDebtController {

    private final ArBadDebtService badDebtService;

    @GetMapping("/page")
    @RequiresPermission("ar:baddebt:query")
    @OperLog(module = "ar")
    public Result<PageResult<ArBadDebtVO>> page(ArBadDebtQueryRequest request) {
        return Result.ok(badDebtService.pageBadDebt(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ar:baddebt:query")
    @OperLog(module = "ar")
    public Result<ArBadDebtVO> detail(@PathVariable Long id) {
        return Result.ok(badDebtService.getBadDebtDetail(id));
    }

    @PostMapping
    @RequiresPermission("ar:baddebt:add")
    @OperLog(module = "ar", type = "CREATE", value = "新增坏账计提单")
    public Result<Long> create(@Valid @RequestBody ArBadDebtSaveRequest request) {
        return Result.ok(badDebtService.createBadDebt(request));
    }

    @PutMapping
    @RequiresPermission("ar:baddebt:edit")
    @OperLog(module = "ar", type = "UPDATE", value = "修改坏账计提单")
    public Result<Void> update(@Valid @RequestBody ArBadDebtSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        badDebtService.updateBadDebt(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("ar:baddebt:remove")
    @OperLog(module = "ar", type = "DELETE", value = "删除坏账计提单")
    public Result<Void> delete(@PathVariable Long id) {
        badDebtService.deleteBadDebt(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("ar:baddebt:audit")
    @OperLog(module = "ar", type = "AUDIT", value = "审核坏账计提单")
    public Result<Void> audit(@PathVariable Long id) {
        badDebtService.auditBadDebt(id);
        return Result.ok();
    }
}













