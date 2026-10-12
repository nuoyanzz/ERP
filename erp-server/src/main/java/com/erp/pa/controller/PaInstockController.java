package com.erp.pa.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pa.domain.dto.PaInstockQueryRequest;
import com.erp.pa.domain.dto.PaInstockSaveRequest;
import com.erp.pa.domain.vo.PaInstockDetailVO;
import com.erp.pa.domain.vo.PaInstockListVO;
import com.erp.pa.service.PaInstockService;
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
@RequestMapping("/pa/instock")
@RequiredArgsConstructor
public class PaInstockController {

    private final PaInstockService instockService;

    @GetMapping("/page")
    @RequiresPermission("pa:instock:query")
    @OperLog(module = "pa")
    public Result<PageResult<PaInstockListVO>> page(PaInstockQueryRequest request) {
        return Result.ok(instockService.pageInstock(request));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pa:instock:query")
    @OperLog(module = "pa")
    public Result<PaInstockDetailVO> detail(@PathVariable Long id) {
        return Result.ok(instockService.getInstockDetail(id));
    }

    @PostMapping
    @RequiresPermission("pa:instock:add")
    @OperLog(module = "pa")
    public Result<Long> create(@Valid @RequestBody PaInstockSaveRequest request) {
        return Result.ok(instockService.createInstock(request));
    }

    @PutMapping
    @RequiresPermission("pa:instock:edit")
    @OperLog(module = "pa")
    public Result<Void> update(@Valid @RequestBody PaInstockSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        instockService.updateInstock(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pa:instock:remove")
    @OperLog(module = "pa")
    public Result<Void> delete(@PathVariable Long id) {
        instockService.deleteInstock(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("pa:instock:audit")
    @OperLog(module = "pa")
    public Result<Void> audit(@PathVariable Long id) {
        instockService.auditInstock(id);
        return Result.ok();
    }
}








