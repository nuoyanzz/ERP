package com.erp.ic.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.ic.domain.dto.IcCheckSaveRequest;
import com.erp.ic.domain.vo.IcCheckDetailVO;
import com.erp.ic.domain.vo.IcCheckListVO;
import com.erp.ic.service.IcCheckService;
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
@RequestMapping("/ic/check")
@RequiredArgsConstructor
public class IcCheckController {

    private final IcCheckService checkService;

    @GetMapping("/page")
    @RequiresPermission("ic:check:query")
    @OperLog(module = "ic")
    public Result<PageResult<IcCheckListVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) Long warehouseId,
                                                  @RequestParam(required = false) Integer status) {
        return Result.ok(checkService.pageCheck(pageNum, pageSize, warehouseId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ic:check:query")
    @OperLog(module = "ic")
    public Result<IcCheckDetailVO> detail(@PathVariable Long id) {
        return Result.ok(checkService.getCheckDetail(id));
    }

    @PostMapping
    @RequiresPermission("ic:check:add")
    @OperLog(module = "ic")
    public Result<Long> create(@Valid @RequestBody IcCheckSaveRequest request) {
        return Result.ok(checkService.createCheck(request));
    }

    @PutMapping
    @RequiresPermission("ic:check:edit")
    @OperLog(module = "ic")
    public Result<Void> update(@Valid @RequestBody IcCheckSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        checkService.updateCheck(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("ic:check:remove")
    @OperLog(module = "ic")
    public Result<Void> delete(@PathVariable Long id) {
        checkService.deleteCheck(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("ic:check:audit")
    @OperLog(module = "ic")
    public Result<Void> audit(@PathVariable Long id) {
        checkService.auditCheck(id);
        return Result.ok();
    }
}








