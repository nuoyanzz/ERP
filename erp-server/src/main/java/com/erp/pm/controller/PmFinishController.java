package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pm.domain.dto.PmFinishSaveRequest;
import com.erp.pm.domain.vo.PmFinishVO;
import com.erp.pm.service.PmFinishService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pm/finish")
@RequiredArgsConstructor
public class PmFinishController {

    private final PmFinishService finishService;

    @GetMapping("/page")
    @RequiresPermission("pm:finish:query")
    @OperLog(module = "pm")
    public Result<PageResult<PmFinishVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                               @RequestParam(defaultValue = "10") int pageSize,
                                               @RequestParam(required = false) Long moId,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(finishService.pageFinish(pageNum, pageSize, moId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pm:finish:query")
    @OperLog(module = "pm")
    public Result<PmFinishVO> detail(@PathVariable Long id) {
        return Result.ok(finishService.getFinishDetail(id));
    }

    @PostMapping
    @RequiresPermission("pm:finish:add")
    @OperLog(module = "pm")
    public Result<Long> create(@Valid @RequestBody PmFinishSaveRequest request) {
        return Result.ok(finishService.createFinish(request));
    }

    @PutMapping
    @RequiresPermission("pm:finish:edit")
    @OperLog(module = "pm")
    public Result<Void> update(@Valid @RequestBody PmFinishSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        finishService.updateFinish(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pm:finish:remove")
    @OperLog(module = "pm")
    public Result<Void> delete(@PathVariable Long id) {
        finishService.deleteFinish(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("pm:finish:audit")
    @OperLog(module = "pm")
    public Result<Void> audit(@PathVariable Long id) {
        finishService.auditFinish(id);
        return Result.ok();
    }
}








