package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pm.domain.dto.PmPickSaveRequest;
import com.erp.pm.domain.vo.PmPickVO;
import com.erp.pm.service.PmPickService;
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
@RequestMapping("/pm/pick")
@RequiredArgsConstructor
public class PmPickController {

    private final PmPickService pickService;

    @GetMapping("/page")
    @RequiresPermission("pm:pick:query")
    @OperLog(module = "pm")
    public Result<PageResult<PmPickVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "10") int pageSize,
                                             @RequestParam(required = false) Long moId,
                                             @RequestParam(required = false) Integer status) {
        return Result.ok(pickService.pagePick(pageNum, pageSize, moId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pm:pick:query")
    @OperLog(module = "pm")
    public Result<PmPickVO> detail(@PathVariable Long id) {
        return Result.ok(pickService.getPickDetail(id));
    }

    @PostMapping
    @RequiresPermission("pm:pick:add")
    @OperLog(module = "pm")
    public Result<Long> create(@Valid @RequestBody PmPickSaveRequest request) {
        return Result.ok(pickService.createPick(request));
    }

    @PutMapping
    @RequiresPermission("pm:pick:edit")
    @OperLog(module = "pm")
    public Result<Void> update(@Valid @RequestBody PmPickSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        pickService.updatePick(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pm:pick:remove")
    @OperLog(module = "pm")
    public Result<Void> delete(@PathVariable Long id) {
        pickService.deletePick(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("pm:pick:audit")
    @OperLog(module = "pm")
    public Result<Void> audit(@PathVariable Long id) {
        pickService.auditPick(id);
        return Result.ok();
    }
}








