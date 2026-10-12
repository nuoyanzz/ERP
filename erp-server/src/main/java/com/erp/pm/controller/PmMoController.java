package com.erp.pm.controller;

import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.pm.domain.dto.PmMoSaveRequest;
import com.erp.pm.domain.vo.PmMoVO;
import com.erp.pm.service.PmMoService;
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
@RequestMapping("/pm/mo")
@RequiredArgsConstructor
public class PmMoController {

    private final PmMoService moService;

    @GetMapping("/page")
    @RequiresPermission("pm:mo:query")
    @OperLog(module = "pm")
    public Result<PageResult<PmMoVO>> page(@RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "10") int pageSize,
                                           @RequestParam(required = false) Long materialId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(moService.pageMo(pageNum, pageSize, materialId, status));
    }

    @GetMapping("/{id}")
    @RequiresPermission("pm:mo:query")
    @OperLog(module = "pm")
    public Result<PmMoVO> detail(@PathVariable Long id) {
        return Result.ok(moService.getMoDetail(id));
    }

    @PostMapping
    @RequiresPermission("pm:mo:add")
    @OperLog(module = "pm")
    public Result<Long> create(@Valid @RequestBody PmMoSaveRequest request) {
        return Result.ok(moService.createMo(request));
    }

    @PutMapping
    @RequiresPermission("pm:mo:edit")
    @OperLog(module = "pm")
    public Result<Void> update(@Valid @RequestBody PmMoSaveRequest request) {
        if (request.getId() == null) {
            throw new BizException("id不能为空");
        }
        moService.updateMo(request);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("pm:mo:remove")
    @OperLog(module = "pm")
    public Result<Void> delete(@PathVariable Long id) {
        moService.deleteMo(id);
        return Result.ok();
    }

    @PostMapping("/{id}/audit")
    @RequiresPermission("pm:mo:audit")
    @OperLog(module = "pm")
    public Result<Void> audit(@PathVariable Long id) {
        moService.auditMo(id);
        return Result.ok();
    }
}








